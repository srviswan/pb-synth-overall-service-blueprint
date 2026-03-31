package com.pbsynth.tradecapture;

import com.pbsynth.tradecapture.dispatch.FanOutDispatchWorker;
import com.pbsynth.tradecapture.domain.DispatchRecord;
import com.pbsynth.tradecapture.domain.DispatchStatus;
import com.pbsynth.tradecapture.domain.IngestionRecord;
import com.pbsynth.tradecapture.domain.IngestionStatus;
import com.pbsynth.tradecapture.repo.DispatchRecordRepository;
import com.pbsynth.tradecapture.repo.IngestionRecordRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

@SpringBootTest(properties = "tradecapture.dispatch.worker-enabled=true")
class FanOutDispatchWorkerTest {
    @Autowired
    private IngestionRecordRepository ingestionRecordRepository;
    @Autowired
    private DispatchRecordRepository dispatchRecordRepository;
    @Autowired
    private FanOutDispatchWorker fanOutDispatchWorker;

    @Test
    void failureOnOneTargetDoesNotBlockOtherTarget() {
        IngestionRecord ingestion = new IngestionRecord();
        ingestion.setIdempotencyKey("idem-worker-1");
        ingestion.setTradeExternalId("TRD-W-1");
        ingestion.setSourceSystem("AMS");
        ingestion.setPartitionKey("ACC1_AAPL");
        ingestion.setStatus(IngestionStatus.QUEUED);
        ingestion.setRequestPayloadJson("{}");
        ingestion.setInstructionEnvelopeJson("{\"instructionType\":\"ExecutionInstruction\"}");
        IngestionRecord saved = ingestionRecordRepository.saveAndFlush(ingestion);
        String ingestionId = saved.getIngestionId();

        DispatchRecord lifecycle = new DispatchRecord();
        lifecycle.setIngestionId(ingestionId);
        lifecycle.setDestinationId("lifecycle-engine");
        lifecycle.setStatus(DispatchStatus.PENDING);
        lifecycle.setAttemptCount(4);
        lifecycle.setNextAttemptAt(Instant.now());
        dispatchRecordRepository.save(lifecycle);

        DispatchRecord audit = new DispatchRecord();
        audit.setIngestionId(ingestionId);
        audit.setDestinationId("audit-log");
        audit.setStatus(DispatchStatus.PENDING);
        audit.setNextAttemptAt(Instant.now());
        dispatchRecordRepository.save(audit);

        fanOutDispatchWorker.dispatch();

        var records = dispatchRecordRepository.findByIngestionIdOrderByCreatedAtAsc(ingestionId);
        Assertions.assertEquals(2, records.size());
        boolean hasSuccess = records.stream().anyMatch(r -> r.getStatus() == DispatchStatus.SENT);
        boolean hasFailure = records.stream().anyMatch(r -> r.getStatus() == DispatchStatus.FAILED);
        Assertions.assertTrue(hasSuccess);
        Assertions.assertTrue(hasFailure);
    }
}
