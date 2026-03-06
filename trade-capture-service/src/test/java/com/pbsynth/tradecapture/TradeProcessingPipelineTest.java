package com.pbsynth.tradecapture;

import com.pbsynth.tradecapture.domain.IngestionRecord;
import com.pbsynth.tradecapture.domain.IngestionStatus;
import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.pbsynth.tradecapture.messaging.TradeMessage;
import com.pbsynth.tradecapture.repo.DispatchRecordRepository;
import com.pbsynth.tradecapture.repo.IngestionRecordRepository;
import com.pbsynth.tradecapture.service.TradeProcessingPipeline;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@SpringBootTest(properties = "tradecapture.dispatch.worker-enabled=false")
class TradeProcessingPipelineTest {
    @Autowired
    private TradeProcessingPipeline pipeline;
    @Autowired
    private IngestionRecordRepository ingestionRecordRepository;
    @Autowired
    private DispatchRecordRepository dispatchRecordRepository;

    @Test
    void pipelineCreatesInstructionAndDispatchRecords() {
        CaptureTradeRequest request = new CaptureTradeRequest(
                "AMS",
                "TRD-P-1",
                "ACCX",
                "BOOKX",
                "MSFT",
                "BUY",
                new BigDecimal("100"),
                new BigDecimal("99.01"),
                LocalDate.now(),
                null,
                null,
                null
        );
        IngestionRecord ingestionRecord = new IngestionRecord();
        ingestionRecord.setIdempotencyKey("idem-pipeline-1");
        ingestionRecord.setTradeExternalId(request.tradeExternalId());
        ingestionRecord.setSourceSystem(request.sourceSystem());
        ingestionRecord.setPartitionKey("ACCX_MSFT");
        ingestionRecord.setStatus(IngestionStatus.ACCEPTED);
        ingestionRecord.setRequestPayloadJson("{}");
        IngestionRecord saved = ingestionRecordRepository.saveAndFlush(ingestionRecord);
        String ingestionId = saved.getIngestionId();

        TradeMessage message = new TradeMessage(
                "msg-p-1",
                ingestionId,
                "ACCX_MSFT",
                "idem-pipeline-1",
                "corr-p-1",
                null,
                "AMS",
                Instant.now(),
                request
        );
        pipeline.handle(message);

        IngestionRecord updated = ingestionRecordRepository.findById(ingestionId).orElseThrow();
        Assertions.assertEquals(IngestionStatus.QUEUED, updated.getStatus());
        Assertions.assertNotNull(updated.getInstructionEnvelopeJson());
        Assertions.assertFalse(dispatchRecordRepository.findByIngestionIdOrderByCreatedAtAsc(ingestionId).isEmpty());
    }
}
