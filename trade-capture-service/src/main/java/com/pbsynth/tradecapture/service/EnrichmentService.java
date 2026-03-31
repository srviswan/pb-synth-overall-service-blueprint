package com.pbsynth.tradecapture.service;

import com.pbsynth.tradecapture.domain.EnrichmentStatus;
import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EnrichmentService {
    public EnrichmentData enrich(CaptureTradeRequest request) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("securityLookup", "stubbed");
        attributes.put("accountLookup", "stubbed");
        attributes.put("securityId", request.securityId());
        attributes.put("accountId", request.accountId());
        return new EnrichmentData(EnrichmentStatus.COMPLETE, attributes);
    }
}
