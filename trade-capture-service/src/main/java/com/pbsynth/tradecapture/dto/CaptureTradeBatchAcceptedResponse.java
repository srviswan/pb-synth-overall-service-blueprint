package com.pbsynth.tradecapture.dto;

public record CaptureTradeBatchAcceptedResponse(
        String batchId,
        String status,
        int acceptedCount,
        int rejectedCount
) {}
