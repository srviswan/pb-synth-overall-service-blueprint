package com.pbsynth.tradecapture.dto;

import java.time.Instant;

public record DispatchDetailDto(
        String destinationId,
        String status,
        int attemptCount,
        String lastError,
        Instant sentAt,
        Instant nextRetryAt
) {
}
