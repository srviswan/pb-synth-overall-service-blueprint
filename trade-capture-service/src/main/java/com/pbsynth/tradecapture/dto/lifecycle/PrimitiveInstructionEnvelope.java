package com.pbsynth.tradecapture.dto.lifecycle;

import java.util.Map;

public record PrimitiveInstructionEnvelope(
        String instructionType,
        String tradeKey,
        Map<String, Object> payload,
        InstructionMetadata metadata,
        InstructionUserContext userContext
) {
}
