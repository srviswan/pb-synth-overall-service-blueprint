package com.pbsynth.tradecapture.dto;

import java.time.Instant;

public record CaptureTradeAcceptedResponse(
        String ingestionId,
        String status,
        Instant acceptedAt,
        String lifecycleInstructionType
) {}
