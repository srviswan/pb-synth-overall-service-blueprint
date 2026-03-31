package com.pbsynth.tradecapture.dispatch;

import com.pbsynth.tradecapture.config.DispatchProperties;
import com.pbsynth.tradecapture.domain.DispatchRecord;
import com.pbsynth.tradecapture.domain.DispatchStatus;
import com.pbsynth.tradecapture.domain.DlqRecord;
import com.pbsynth.tradecapture.domain.IngestionRecord;
import com.pbsynth.tradecapture.domain.IngestionStatus;
import com.pbsynth.tradecapture.repo.DispatchRecordRepository;
import com.pbsynth.tradecapture.repo.DlqRecordRepository;
import com.pbsynth.tradecapture.repo.IngestionRecordRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "tradecapture.dispatch.worker-enabled", havingValue = "true", matchIfMissing = true)
public class FanOutDispatchWorker {
    private final DispatchProperties dispatchProperties;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final IngestionRecordRepository ingestionRecordRepository;
    private final DlqRecordRepository dlqRecordRepository;
    private final DispatchTargetRegistry targetRegistry;
    private final MeterRegistry meterRegistry;

    public FanOutDispatchWorker(
            DispatchProperties dispatchProperties,
            DispatchRecordRepository dispatchRecordRepository,
            IngestionRecordRepository ingestionRecordRepository,
            DlqRecordRepository dlqRecordRepository,
            DispatchTargetRegistry targetRegistry,
            MeterRegistry meterRegistry
    ) {
        this.dispatchProperties = dispatchProperties;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.ingestionRecordRepository = ingestionRecordRepository;
        this.dlqRecordRepository = dlqRecordRepository;
        this.targetRegistry = targetRegistry;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${tradecapture.dispatch.poll-interval-ms:1000}")
    @Transactional
    public void dispatch() {
        List<DispatchRecord> pending = dispatchRecordRepository
                .findTop500ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(DispatchStatus.PENDING, Instant.now());
        for (DispatchRecord record : pending) {
            DispatchTargetRegistry.ResolvedTarget resolvedTarget = targetRegistry.resolve(record.getDestinationId());
            if (resolvedTarget == null) {
                failRecord(record, "No dispatch target configured for destination " + record.getDestinationId(), 1);
                recomputeIngestionStatus(record.getIngestionId());
                continue;
            }

            IngestionRecord ingestionRecord = ingestionRecordRepository.findById(record.getIngestionId()).orElse(null);
            if (ingestionRecord == null || ingestionRecord.getInstructionEnvelopeJson() == null) {
                failRecord(record, "Ingestion record or instruction envelope missing", resolvedTarget.config().getMaxAttempts());
                recomputeIngestionStatus(record.getIngestionId());
                continue;
            }

            Map<String, String> headers = buildHeaders(ingestionRecord);
            DispatchResult result = resolvedTarget.strategy()
                    .dispatch(resolvedTarget.config(), ingestionRecord.getInstructionEnvelopeJson(), headers);

            if (result.success()) {
                record.setStatus(DispatchStatus.SENT);
                record.setSentAt(Instant.now());
                record.setLastError(null);
                dispatchRecordRepository.save(record);
                meterRegistry.counter("trade.dispatch.success", "destination", record.getDestinationId()).increment();
            } else {
                failRecord(record, result.errorMessage(), resolvedTarget.config().getMaxAttempts());
                meterRegistry.counter("trade.dispatch.failure", "destination", record.getDestinationId()).increment();
            }
            recomputeIngestionStatus(record.getIngestionId());
        }
    }

    private Map<String, String> buildHeaders(IngestionRecord ingestionRecord) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Idempotency-Key", ingestionRecord.getIngestionId());
        if (ingestionRecord.getCorrelationId() != null) {
            headers.put("X-Correlation-Id", ingestionRecord.getCorrelationId());
        }
        if (ingestionRecord.getUserContext() != null) {
            headers.put("X-User-Context", ingestionRecord.getUserContext());
        }
        return headers;
    }

    private void failRecord(DispatchRecord record, String error, int maxAttempts) {
        int attempts = record.getAttemptCount() + 1;
        record.setAttemptCount(attempts);
        record.setLastError(error);
        if (attempts >= maxAttempts) {
            record.setStatus(DispatchStatus.FAILED);
            DlqRecord dlqRecord = new DlqRecord();
            dlqRecord.setIngestionId(record.getIngestionId());
            dlqRecord.setDestinationId(record.getDestinationId());
            dlqRecord.setPayload(error == null ? "" : error);
            dlqRecord.setFailureReason(error == null ? "Dispatch failed" : error);
            dlqRecordRepository.save(dlqRecord);
            meterRegistry.counter("trade.dispatch.dlq", "destination", record.getDestinationId()).increment();
        } else {
            long delayMs = dispatchProperties.getBaseBackoffMs() * (1L << (attempts - 1));
            record.setNextAttemptAt(Instant.now().plusMillis(delayMs));
        }
        dispatchRecordRepository.save(record);
    }

    private void recomputeIngestionStatus(String ingestionId) {
        IngestionRecord ingestion = ingestionRecordRepository.findById(ingestionId).orElse(null);
        if (ingestion == null) {
            return;
        }
        List<DispatchRecord> records = dispatchRecordRepository.findByIngestionIdOrderByCreatedAtAsc(ingestionId);
        if (records.isEmpty()) {
            ingestion.setStatus(IngestionStatus.QUEUED);
            ingestionRecordRepository.save(ingestion);
            return;
        }
        long total = records.size();
        long pending = records.stream().filter(r -> r.getStatus() == DispatchStatus.PENDING).count();
        long sent = records.stream().filter(r -> r.getStatus() == DispatchStatus.SENT).count();
        long failed = records.stream().filter(r -> r.getStatus() == DispatchStatus.FAILED).count();

        IngestionStatus status;
        if (sent == total) {
            status = IngestionStatus.SENT;
        } else if (failed == total) {
            status = IngestionStatus.FAILED;
        } else if (sent > 0 && failed > 0 && pending == 0) {
            status = IngestionStatus.PARTIALLY_SENT;
        } else if (sent > 0 || failed > 0) {
            status = IngestionStatus.DISPATCHING;
        } else {
            status = IngestionStatus.QUEUED;
        }
        ingestion.setStatus(status);
        ingestionRecordRepository.save(ingestion);
    }
}
