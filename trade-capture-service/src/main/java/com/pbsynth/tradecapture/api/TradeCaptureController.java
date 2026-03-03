package com.pbsynth.tradecapture.api;

import com.pbsynth.tradecapture.dto.*;
import com.pbsynth.tradecapture.service.TradeCaptureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/trade-capture-service/v1")
public class TradeCaptureController {
    private final TradeCaptureService tradeCaptureService;

    public TradeCaptureController(TradeCaptureService tradeCaptureService) {
        this.tradeCaptureService = tradeCaptureService;
    }

    @PostMapping("/trades")
    public ResponseEntity<CaptureTradeAcceptedResponse> captureTrade(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Correlation-Id") String correlationId,
            @RequestHeader(value = "X-User-Context", required = false) String userContext,
            @Valid @RequestBody CaptureTradeRequest request
    ) {
        CaptureTradeAcceptedResponse response = tradeCaptureService.captureSingle(
                request, idempotencyKey, correlationId, userContext
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/trades:batch")
    public ResponseEntity<CaptureTradeBatchAcceptedResponse> captureTradeBatch(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Correlation-Id") String correlationId,
            @RequestHeader(value = "X-User-Context", required = false) String userContext,
            @Valid @RequestBody CaptureTradeBatchRequest request
    ) {
        CaptureTradeBatchAcceptedResponse response = tradeCaptureService.captureBatch(
                request, idempotencyKey, correlationId, userContext
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/trades/{ingestionId}")
    public TradeIngestionStatusResponse getStatus(@PathVariable String ingestionId) {
        return tradeCaptureService.getStatus(ingestionId);
    }
}
