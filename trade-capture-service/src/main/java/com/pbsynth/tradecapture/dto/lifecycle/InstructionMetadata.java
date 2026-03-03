package com.pbsynth.tradecapture.dto.lifecycle;

import java.time.Instant;

public record InstructionMetadata(
        String sourceSystem,
        Instant receivedAt,
        String correlationId,
        String causationId
) {
}
