package com.pbsynth.tradecapture.dto;

import java.time.Instant;
import java.util.List;

public record TradeIngestionStatusResponse(
        String ingestionId,
        String status,
        Instant lastUpdatedAt,
        String failureReason,
        List<DispatchDetailDto> dispatchDetails
) {}
