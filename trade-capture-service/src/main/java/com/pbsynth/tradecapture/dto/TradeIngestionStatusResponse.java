package com.pbsynth.tradecapture.dto;

import java.time.Instant;

public record TradeIngestionStatusResponse(
        String ingestionId,
        String status,
        Instant lastUpdatedAt,
        String failureReason
) {}
