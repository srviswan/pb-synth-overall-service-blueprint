package com.pbsynth.tradecapture.domain;

import java.util.List;

public record TradeProcessingResult(
        EnrichmentStatus enrichmentStatus,
        WorkflowStatus workflowStatus,
        PositionStatusEnum positionStatus,
        List<String> appliedRules
) {
}
