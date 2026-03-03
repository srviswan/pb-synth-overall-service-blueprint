package com.pbsynth.tradecapture.service;

import com.pbsynth.tradecapture.config.DispatchProperties;
import com.pbsynth.tradecapture.domain.DlqRecord;
import com.pbsynth.tradecapture.domain.IngestionRecord;
import com.pbsynth.tradecapture.mapper.CdmInstructionMapper;
import com.pbsynth.tradecapture.repo.DlqRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DispatchWorker {
    private final TradeCaptureService tradeCaptureService;
    private final LifecycleClient lifecycleClient;
    private final CdmInstructionMapper mapper;
    private final DlqRecordRepository dlqRecordRepository;
    private final DispatchProperties dispatchProperties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public DispatchWorker(
            TradeCaptureService tradeCaptureService,
            LifecycleClient lifecycleClient,
            CdmInstructionMapper mapper,
            DlqRecordRepository dlqRecordRepository,
            DispatchProperties dispatchProperties,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.tradeCaptureService = tradeCaptureService;
        this.lifecycleClient = lifecycleClient;
        this.mapper = mapper;
        this.dlqRecordRepository = dlqRecordRepository;
        this.dispatchProperties = dispatchProperties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelay = 1500L)
    public void dispatchQueued() {
        for (IngestionRecord record : tradeCaptureService.findQueued(dispatchProperties.getBatchSize())) {
            try {
                Map<?, ?> payloadMap = objectMapper.readValue(record.getRequestPayloadJson(), Map.class);
                String instructionPayload = mapper.toExecutionInstructionPayload(
                        new com.pbsynth.tradecapture.dto.CaptureTradeRequest(
                                String.valueOf(payloadMap.get("sourceSystem")),
                                String.valueOf(payloadMap.get("tradeExternalId")),
                                String.valueOf(payloadMap.get("accountId")),
                                String.valueOf(payloadMap.get("bookId")),
                                String.valueOf(payloadMap.get("securityId")),
                                String.valueOf(payloadMap.get("direction")),
                                new java.math.BigDecimal(String.valueOf(payloadMap.get("quantity"))),
                                new java.math.BigDecimal(String.valueOf(payloadMap.get("price"))),
                                java.time.LocalDate.parse(String.valueOf(payloadMap.get("tradeDate"))),
                                payloadMap.get("settlementDate") == null ? null : java.time.LocalDate.parse(String.valueOf(payloadMap.get("settlementDate"))),
                                payloadMap.get("productId") == null ? null : String.valueOf(payloadMap.get("productId")),
                                payloadMap.get("metadata") instanceof Map<?,?> m ? (Map<String, Object>) m : null
                        ),
                        record.getCorrelationId(),
                        record.getUserContext()
                );
                lifecycleClient.dispatchInstruction(instructionPayload, record.getCorrelationId());
                tradeCaptureService.markSent(record);
                meterRegistry.counter("tradecapture.dispatch.sent").increment();
            } catch (Exception ex) {
                tradeCaptureService.markDispatchFailure(record, "LIFECYCLE_DISPATCH_ERROR", ex.getMessage(), dispatchProperties.getMaxAttempts());
                meterRegistry.counter("tradecapture.dispatch.retry").increment();
                if (record.getAttemptCount() + 1 >= dispatchProperties.getMaxAttempts()) {
                    DlqRecord dlqRecord = new DlqRecord();
                    dlqRecord.setIngestionId(record.getIngestionId());
                    dlqRecord.setPayload(record.getRequestPayloadJson());
                    dlqRecord.setFailureReason(ex.getMessage());
                    dlqRecordRepository.save(dlqRecord);
                    meterRegistry.counter("tradecapture.dispatch.dlq").increment();
                }
            }
        }
    }
}
