package com.pbsynth.tradecapture;

import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.pbsynth.tradecapture.exception.ApiException;
import com.pbsynth.tradecapture.repo.IngestionRecordRepository;
import com.pbsynth.tradecapture.service.TradeCaptureService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest
class TradeCaptureServiceTest {

    @Autowired
    private TradeCaptureService service;

    @Autowired
    private IngestionRecordRepository repository;

    @Test
    void capturesSingleTradeAndStoresRecord() {
        var request = sampleRequest("TRD-1");
        var response = service.captureSingle(request, "idem-1", "corr-1", "user-a");
        Assertions.assertEquals("ACCEPTED", response.status());
        Assertions.assertTrue(repository.findById(response.ingestionId()).isPresent());
    }

    @Test
    void duplicateIdempotencyThrowsConflict() {
        var request = sampleRequest("TRD-2");
        service.captureSingle(request, "idem-dup", "corr-2", null);
        ApiException ex = Assertions.assertThrows(ApiException.class,
                () -> service.captureSingle(request, "idem-dup", "corr-3", null));
        Assertions.assertEquals("DUPLICATE_IDEMPOTENCY_KEY", ex.getCode());
    }

    private CaptureTradeRequest sampleRequest(String tradeId) {
        return new CaptureTradeRequest(
                "AMS",
                tradeId,
                "ACC1",
                "BOOK1",
                "AAPL",
                "BUY",
                new BigDecimal("1000"),
                new BigDecimal("151.25"),
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                "PRD-1",
                null
        );
    }
}
