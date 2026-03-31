package com.pbsynth.tradecapture.dto.lifecycle;

import java.time.Instant;

public record InstructionAcceptedResponse(
        String instructionId,
        String status,
        Instant queuedAt
) {
}
