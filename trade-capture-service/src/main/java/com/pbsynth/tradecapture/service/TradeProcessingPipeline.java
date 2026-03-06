package com.pbsynth.tradecapture.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbsynth.tradecapture.dispatch.DispatchTargetRegistry;
import com.pbsynth.tradecapture.domain.DispatchRecord;
import com.pbsynth.tradecapture.domain.DispatchStatus;
import com.pbsynth.tradecapture.domain.IngestionRecord;
import com.pbsynth.tradecapture.domain.IngestionStatus;
import com.pbsynth.tradecapture.domain.PositionStatusEnum;
import com.pbsynth.tradecapture.domain.TradeProcessingResult;
import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.pbsynth.tradecapture.dto.lifecycle.PrimitiveInstructionEnvelope;
import com.pbsynth.tradecapture.mapper.CdmInstructionMapper;
import com.pbsynth.tradecapture.messaging.TradeMessage;
import com.pbsynth.tradecapture.messaging.TradeMessageHandler;
import com.pbsynth.tradecapture.repo.DispatchRecordRepository;
import com.pbsynth.tradecapture.repo.IngestionRecordRepository;
import com.pbsynth.tradecapture.rules.RuleEvaluationResult;
import com.pbsynth.tradecapture.rules.RulesEngine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TradeProcessingPipeline implements TradeMessageHandler {
    private final IngestionRecordRepository ingestionRecordRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final EnrichmentService enrichmentService;
    private final ValidationService validationService;
    private final StateManagementService stateManagementService;
    private final RulesEngine rulesEngine;
    private final CdmInstructionMapper mapper;
    private final DispatchTargetRegistry targetRegistry;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public TradeProcessingPipeline(
            IngestionRecordRepository ingestionRecordRepository,
            DispatchRecordRepository dispatchRecordRepository,
            EnrichmentService enrichmentService,
            ValidationService validationService,
            StateManagementService stateManagementService,
            RulesEngine rulesEngine,
            CdmInstructionMapper mapper,
            DispatchTargetRegistry targetRegistry,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.ingestionRecordRepository = ingestionRecordRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.enrichmentService = enrichmentService;
        this.validationService = validationService;
        this.stateManagementService = stateManagementService;
        this.rulesEngine = rulesEngine;
        this.mapper = mapper;
        this.targetRegistry = targetRegistry;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public void handle(TradeMessage message) {
        IngestionRecord ingestionRecord = null;
        if (message.ingestionId() != null && !message.ingestionId().isBlank()) {
            ingestionRecord = ingestionRecordRepository.findById(message.ingestionId()).orElse(null);
        }
        if (ingestionRecord == null) {
            ingestionRecord = createRecordFromMessage(message);
        }

        // If already mapped and queued/sent, this is a duplicate redelivery.
        if (ingestionRecord.getInstructionEnvelopeJson() != null) {
            return;
        }

        try {
            CaptureTradeRequest request = message.payload();
            validationService.validate(request);
            EnrichmentData enrichmentData = enrichmentService.enrich(request);
            RuleEvaluationResult ruleEvaluationResult = rulesEngine.evaluate(request);
            PositionStatusEnum status = stateManagementService.initialState();

            TradeProcessingResult processingResult = new TradeProcessingResult(
                    enrichmentData.status(),
                    ruleEvaluationResult.getWorkflowStatus(),
                    status,
                    ruleEvaluationResult.getAppliedRules()
            );
            PrimitiveInstructionEnvelope envelope = mapper.toExecutionInstruction(message, processingResult);
            String envelopeJson = mapper.toJson(envelope);

            ingestionRecord.setInstructionEnvelopeJson(envelopeJson);
            ingestionRecord.setStatus(IngestionStatus.QUEUED);
            ingestionRecord.setErrorCode(null);
            ingestionRecord.setErrorMessage(null);
            ingestionRecordRepository.save(ingestionRecord);

            for (var target : targetRegistry.enabledTargets()) {
                if (dispatchRecordRepository.existsByIngestionIdAndDestinationId(ingestionRecord.getIngestionId(), target.getId())) {
                    continue;
                }
                DispatchRecord dispatchRecord = new DispatchRecord();
                dispatchRecord.setIngestionId(ingestionRecord.getIngestionId());
                dispatchRecord.setDestinationId(target.getId());
                dispatchRecord.setStatus(DispatchStatus.PENDING);
                dispatchRecord.setNextAttemptAt(Instant.now());
                dispatchRecordRepository.save(dispatchRecord);
            }

            meterRegistry.counter("trade.capture.processing.success").increment();
        } catch (Exception ex) {
            ingestionRecord.setStatus(IngestionStatus.FAILED);
            ingestionRecord.setErrorCode("VALIDATION_ERROR");
            ingestionRecord.setErrorMessage(ex.getMessage());
            ingestionRecordRepository.save(ingestionRecord);
            meterRegistry.counter("trade.capture.processing.failure").increment();
        }
    }

    private IngestionRecord createRecordFromMessage(TradeMessage message) {
        IngestionRecord record = new IngestionRecord();
        record.setIdempotencyKey(message.idempotencyKey());
        record.setTradeExternalId(message.payload().tradeExternalId());
        record.setSourceSystem(message.sourceSystem());
        record.setPartitionKey(message.partitionKey());
        record.setStatus(IngestionStatus.ACCEPTED);
        record.setCorrelationId(message.correlationId());
        record.setUserContext(message.userContext());
        try {
            record.setRequestPayloadJson(objectMapper.writeValueAsString(message.payload()));
        } catch (JsonProcessingException e) {
            record.setRequestPayloadJson("{}");
        }
        return ingestionRecordRepository.save(record);
    }
}
