package com.pbsynth.tradecapture.messaging;

import com.pbsynth.tradecapture.dto.CaptureTradeRequest;

import java.time.Instant;

public record TradeMessage(
        String messageId,
        String ingestionId,
        String partitionKey,
        String idempotencyKey,
        String correlationId,
        String userContext,
        String sourceSystem,
        Instant receivedAt,
        CaptureTradeRequest payload
) {
}
