package com.pbsynth.tradecapture.mapper;

import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CdmInstructionMapper {
    private final ObjectMapper objectMapper;

    public CdmInstructionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toExecutionInstructionPayload(CaptureTradeRequest request, String correlationId, String userContext) {
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("instructionType", "ExecutionInstruction");
            envelope.put("tradeKey", request.tradeExternalId());
            envelope.put("payload", objectMapper.convertValue(request, Map.class));
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceSystem", request.sourceSystem());
            metadata.put("correlationId", correlationId);
            metadata.put("userContext", userContext);
            envelope.put("metadata", metadata);
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to map instruction payload", e);
        }
    }
}
