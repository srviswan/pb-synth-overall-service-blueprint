package com.pbsynth.tradecapture.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        String correlationId,
        Instant timestamp,
        List<String> details
) {}
