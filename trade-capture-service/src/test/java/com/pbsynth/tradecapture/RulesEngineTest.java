package com.pbsynth.tradecapture;

import com.pbsynth.tradecapture.domain.WorkflowStatus;
import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.pbsynth.tradecapture.rules.RulesEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest(properties = "tradecapture.dispatch.worker-enabled=false")
class RulesEngineTest {
    @Autowired
    private RulesEngine rulesEngine;

    @Test
    void manualSourceUsesPendingApprovalRule() {
        CaptureTradeRequest request = new CaptureTradeRequest(
                "MANUAL",
                "TRD-M-1",
                "ACC1",
                "BOOK1",
                "AAPL",
                "BUY",
                new BigDecimal("10"),
                new BigDecimal("12.3"),
                LocalDate.now(),
                null,
                null,
                null
        );
        var result = rulesEngine.evaluate(request);
        Assertions.assertEquals(WorkflowStatus.PENDING_APPROVAL, result.getWorkflowStatus());
    }
}
