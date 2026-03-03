package com.pbsynth.tradecapture.service;

import com.pbsynth.tradecapture.domain.EnrichmentStatus;

import java.util.Map;

public record EnrichmentData(
        EnrichmentStatus status,
        Map<String, Object> attributes
) {
}
