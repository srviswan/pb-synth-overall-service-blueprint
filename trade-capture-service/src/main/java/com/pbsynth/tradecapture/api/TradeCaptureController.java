package com.pbsynth.tradecapture.api;

import com.pbsynth.tradecapture.dto.*;
import com.pbsynth.tradecapture.service.TradeIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/trade-capture-service/v1")
public class TradeCaptureController {
    private final TradeIngestionService tradeIngestionService;

    public TradeCaptureController(TradeIngestionService tradeIngestionService) {
        this.tradeIngestionService = tradeIngestionService;
    }

    @PostMapping("/trades")
    public ResponseEntity<CaptureTradeAcceptedResponse> captureTrade(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Correlation-Id") String correlationId,
            @RequestHeader(value = "X-User-Context", required = false) String userContext,
            @Valid @RequestBody CaptureTradeRequest request
    ) {
        CaptureTradeAcceptedResponse response = tradeIngestionService.captureSingle(
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
        CaptureTradeBatchAcceptedResponse response = tradeIngestionService.captureBatch(
                request, idempotencyKey, correlationId, userContext
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/trades/{ingestionId}")
    public TradeIngestionStatusResponse getStatus(@PathVariable String ingestionId) {
        return tradeIngestionService.getStatus(ingestionId);
    }
}
