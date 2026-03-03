package com.pbsynth.tradecapture.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbsynth.tradecapture.domain.DispatchRecord;
import com.pbsynth.tradecapture.domain.IngestionRecord;
import com.pbsynth.tradecapture.domain.IngestionStatus;
import com.pbsynth.tradecapture.dto.CaptureTradeAcceptedResponse;
import com.pbsynth.tradecapture.dto.CaptureTradeBatchAcceptedResponse;
import com.pbsynth.tradecapture.dto.CaptureTradeBatchRequest;
import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.pbsynth.tradecapture.dto.DispatchDetailDto;
import com.pbsynth.tradecapture.dto.TradeIngestionStatusResponse;
import com.pbsynth.tradecapture.exception.ApiException;
import com.pbsynth.tradecapture.messaging.TradeMessage;
import com.pbsynth.tradecapture.messaging.TradeMessagePublisher;
import com.pbsynth.tradecapture.repo.DispatchRecordRepository;
import com.pbsynth.tradecapture.repo.IngestionRecordRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TradeIngestionService {
    private final IngestionRecordRepository ingestionRecordRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final TradeMessagePublisher tradeMessagePublisher;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public TradeIngestionService(
            IngestionRecordRepository ingestionRecordRepository,
            DispatchRecordRepository dispatchRecordRepository,
            TradeMessagePublisher tradeMessagePublisher,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.ingestionRecordRepository = ingestionRecordRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.tradeMessagePublisher = tradeMessagePublisher;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public CaptureTradeAcceptedResponse captureSingle(
            CaptureTradeRequest request,
            String idempotencyKey,
            String correlationId,
            String userContext
    ) {
        if (ingestionRecordRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new ApiException("DUPLICATE_IDEMPOTENCY_KEY", "Duplicate idempotency key", HttpStatus.CONFLICT);
        }

        IngestionRecord record = createIngestionRecord(request, idempotencyKey, correlationId, userContext);
        ingestionRecordRepository.saveAndFlush(record);
        publishMessage(record, request, correlationId, userContext, request.sourceSystem());
        meterRegistry.counter("trade.capture.accepted").increment();
        return new CaptureTradeAcceptedResponse(record.getIngestionId(), "ACCEPTED", record.getCreatedAt(), "ExecutionInstruction");
    }

    @Transactional
    public CaptureTradeBatchAcceptedResponse captureBatch(
            CaptureTradeBatchRequest batchRequest,
            String idempotencyKey,
            String correlationId,
            String userContext
    ) {
        int accepted = 0;
        int rejected = 0;
        for (int i = 0; i < batchRequest.trades().size(); i++) {
            CaptureTradeRequest request = batchRequest.trades().get(i);
            if (!batchRequest.sourceSystem().equals(request.sourceSystem())) {
                rejected++;
                continue;
            }
            String itemKey = idempotencyKey + "-" + i;
            if (ingestionRecordRepository.findByIdempotencyKey(itemKey).isPresent()) {
                rejected++;
                continue;
            }
            IngestionRecord record = createIngestionRecord(request, itemKey, correlationId, userContext);
            ingestionRecordRepository.saveAndFlush(record);
            publishMessage(record, request, correlationId, userContext, batchRequest.sourceSystem());
            accepted++;
        }
        return new CaptureTradeBatchAcceptedResponse(UUID.randomUUID().toString(), "ACCEPTED", accepted, rejected);
    }

    @Transactional(readOnly = true)
    public TradeIngestionStatusResponse getStatus(String ingestionId) {
        IngestionRecord record = ingestionRecordRepository.findById(ingestionId)
                .orElseThrow(() -> new ApiException("INGESTION_NOT_FOUND", "Ingestion record not found", HttpStatus.NOT_FOUND));
        List<DispatchDetailDto> details = dispatchRecordRepository.findByIngestionIdOrderByCreatedAtAsc(ingestionId).stream()
                .map(this::toDispatchDetail)
                .toList();
        return new TradeIngestionStatusResponse(
                record.getIngestionId(),
                toApiStatus(record.getStatus()),
                record.getUpdatedAt(),
                record.getErrorMessage(),
                details
        );
    }

    private IngestionRecord createIngestionRecord(
            CaptureTradeRequest request,
            String idempotencyKey,
            String correlationId,
            String userContext
    ) {
        IngestionRecord record = new IngestionRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setTradeExternalId(request.tradeExternalId());
        record.setSourceSystem(request.sourceSystem());
        record.setPartitionKey(partitionKey(request));
        record.setStatus(IngestionStatus.ACCEPTED);
        record.setCorrelationId(correlationId);
        record.setUserContext(userContext);
        try {
            record.setRequestPayloadJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new ApiException("VALIDATION_ERROR", "Unable to serialize request payload");
        }
        return record;
    }

    private void publishMessage(
            IngestionRecord record,
            CaptureTradeRequest request,
            String correlationId,
            String userContext,
            String sourceSystem
    ) {
        TradeMessage message = new TradeMessage(
                UUID.randomUUID().toString(),
                record.getIngestionId(),
                record.getPartitionKey(),
                record.getIdempotencyKey(),
                correlationId,
                userContext,
                sourceSystem,
                Instant.now(),
                request
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tradeMessagePublisher.publish(message);
                }
            });
        } else {
            tradeMessagePublisher.publish(message);
        }
    }

    private String partitionKey(CaptureTradeRequest request) {
        return request.accountId() + "_" + request.securityId();
    }

    private DispatchDetailDto toDispatchDetail(DispatchRecord dispatchRecord) {
        return new DispatchDetailDto(
                dispatchRecord.getDestinationId(),
                dispatchRecord.getStatus().name(),
                dispatchRecord.getAttemptCount(),
                dispatchRecord.getLastError(),
                dispatchRecord.getSentAt(),
                dispatchRecord.getNextAttemptAt()
        );
    }

    private String toApiStatus(IngestionStatus status) {
        return switch (status) {
            case ACCEPTED -> "ACCEPTED";
            case SENT -> "SENT_TO_LIFECYCLE";
            case FAILED -> "FAILED";
            case QUEUED, DISPATCHING, PARTIALLY_SENT -> "QUEUED";
        };
    }
}
