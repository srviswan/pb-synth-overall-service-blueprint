package com.pbsynth.tradecapture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbsynth.tradecapture.domain.EnrichmentStatus;
import com.pbsynth.tradecapture.domain.PositionStatusEnum;
import com.pbsynth.tradecapture.domain.TradeProcessingResult;
import com.pbsynth.tradecapture.domain.WorkflowStatus;
import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.pbsynth.tradecapture.dto.lifecycle.PrimitiveInstructionEnvelope;
import com.pbsynth.tradecapture.mapper.CdmInstructionMapper;
import com.pbsynth.tradecapture.messaging.TradeMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

class CdmInstructionMapperTest {
    @Test
    void mapsTradeMessageToExecutionInstructionEnvelope() {
        CdmInstructionMapper mapper = new CdmInstructionMapper(new ObjectMapper());
        CaptureTradeRequest request = new CaptureTradeRequest(
                "AMS",
                "TRD-1",
                "ACC1",
                "BOOK1",
                "AAPL",
                "BUY",
                new BigDecimal("10"),
                new BigDecimal("123.45"),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 17),
                "PRD-1",
                Map.of("k", "v")
        );
        TradeMessage message = new TradeMessage(
                "msg-1",
                "ing-1",
                "ACC1_AAPL",
                "idem-1",
                "corr-1",
                "{\"userId\":\"u1\"}",
                "AMS",
                Instant.now(),
                request
        );
        TradeProcessingResult result = new TradeProcessingResult(
                EnrichmentStatus.COMPLETE,
                WorkflowStatus.APPROVED,
                PositionStatusEnum.EXECUTED,
                List.of("RULE-1")
        );
        PrimitiveInstructionEnvelope envelope = mapper.toExecutionInstruction(message, result);

        Assertions.assertEquals("ExecutionInstruction", envelope.instructionType());
        Assertions.assertEquals("ACC1_AAPL", envelope.tradeKey());
        Assertions.assertEquals("TRD-1", envelope.payload().get("tradeExternalId"));
        Assertions.assertEquals("APPROVED", envelope.payload().get("workflowStatus"));
    }
}
