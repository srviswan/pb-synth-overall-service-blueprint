package com.pbsynth.tradecapture.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbsynth.tradecapture.domain.IngestionRecord;
import com.pbsynth.tradecapture.domain.IngestionStatus;
import com.pbsynth.tradecapture.dto.*;
import com.pbsynth.tradecapture.exception.ApiException;
import com.pbsynth.tradecapture.repo.IngestionRecordRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TradeCaptureService {
    private final IngestionRecordRepository ingestionRepo;
    private final ObjectMapper objectMapper;
    private final Counter acceptedCounter;
    private final Counter dedupeCounter;

    public TradeCaptureService(IngestionRecordRepository ingestionRepo, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.ingestionRepo = ingestionRepo;
        this.objectMapper = objectMapper;
        this.acceptedCounter = meterRegistry.counter("tradecapture.ingestion.accepted");
        this.dedupeCounter = meterRegistry.counter("tradecapture.ingestion.deduped");
    }

    @Transactional
    public CaptureTradeAcceptedResponse captureSingle(
            CaptureTradeRequest request,
            String idempotencyKey,
            String correlationId,
            String userContext
    ) {
        if (ingestionRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
            dedupeCounter.increment();
            throw new ApiException("DUPLICATE_IDEMPOTENCY_KEY", "Duplicate idempotency key");
        }
        IngestionRecord record = toRecord(request, idempotencyKey, correlationId, userContext);
        ingestionRepo.save(record);
        acceptedCounter.increment();
        return new CaptureTradeAcceptedResponse(record.getIngestionId(), "ACCEPTED", record.getCreatedAt(), "ExecutionInstruction");
    }

    @Transactional
    public CaptureTradeBatchAcceptedResponse captureBatch(
            CaptureTradeBatchRequest batch,
            String idempotencyKey,
            String correlationId,
            String userContext
    ) {
        if (ingestionRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
            dedupeCounter.increment();
            throw new ApiException("DUPLICATE_IDEMPOTENCY_KEY", "Duplicate idempotency key");
        }
        int accepted = 0;
        int rejected = 0;
        for (int i = 0; i < batch.trades().size(); i++) {
            CaptureTradeRequest item = batch.trades().get(i);
            if (!batch.sourceSystem().equals(item.sourceSystem())) {
                rejected++;
                continue;
            }
            String itemKey = idempotencyKey + "-" + i;
            if (ingestionRepo.findByIdempotencyKey(itemKey).isPresent()) {
                rejected++;
                continue;
            }
            IngestionRecord record = toRecord(item, itemKey, correlationId, userContext);
            ingestionRepo.save(record);
            accepted++;
        }
        return new CaptureTradeBatchAcceptedResponse(UUID.randomUUID().toString(), "ACCEPTED", accepted, rejected);
    }

    @Transactional(readOnly = true)
    public TradeIngestionStatusResponse getStatus(String ingestionId) {
        IngestionRecord record = ingestionRepo.findById(ingestionId)
                .orElseThrow(() -> new ApiException("INGESTION_NOT_FOUND", "Ingestion record not found"));
        return new TradeIngestionStatusResponse(
                record.getIngestionId(),
                record.getStatus().name(),
                record.getUpdatedAt(),
                record.getErrorMessage()
        );
    }

    @Transactional
    public List<IngestionRecord> findQueued(int limit) {
        return ingestionRepo.findTop100ByStatusOrderByCreatedAtAsc(IngestionStatus.QUEUED)
                .stream()
                .limit(limit)
                .toList();
    }

    @Transactional
    public void markQueued(IngestionRecord record) {
        record.setStatus(IngestionStatus.QUEUED);
        ingestionRepo.save(record);
    }

    @Transactional
    public void markSent(IngestionRecord record) {
        record.setStatus(IngestionStatus.SENT_TO_LIFECYCLE);
        record.setErrorCode(null);
        record.setErrorMessage(null);
        ingestionRepo.save(record);
    }

    @Transactional
    public void markDispatchFailure(IngestionRecord record, String errorCode, String message, int maxAttempts) {
        record.setAttemptCount(record.getAttemptCount() + 1);
        record.setErrorCode(errorCode);
        record.setErrorMessage(message);
        if (record.getAttemptCount() >= maxAttempts) {
            record.setStatus(IngestionStatus.FAILED);
        } else {
            record.setStatus(IngestionStatus.QUEUED);
        }
        ingestionRepo.save(record);
    }

    private IngestionRecord toRecord(CaptureTradeRequest request, String idempotencyKey, String correlationId, String userContext) {
        IngestionRecord record = new IngestionRecord();
        record.setIngestionId(UUID.randomUUID().toString());
        record.setIdempotencyKey(idempotencyKey);
        record.setTradeExternalId(request.tradeExternalId());
        record.setSourceSystem(request.sourceSystem());
        record.setStatus(IngestionStatus.QUEUED);
        record.setCorrelationId(correlationId);
        record.setUserContext(userContext);
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        try {
            record.setRequestPayloadJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new ApiException("VALIDATION_ERROR", "Unable to serialize request payload");
        }
        return record;
    }
}
