package com.pbsynth.tradecapture.mapper;

import com.pbsynth.tradecapture.domain.TradeProcessingResult;
import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.pbsynth.tradecapture.dto.lifecycle.InstructionMetadata;
import com.pbsynth.tradecapture.dto.lifecycle.InstructionUserContext;
import com.pbsynth.tradecapture.dto.lifecycle.PrimitiveInstructionEnvelope;
import com.pbsynth.tradecapture.messaging.TradeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CdmInstructionMapper {
    private final ObjectMapper objectMapper;

    public CdmInstructionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PrimitiveInstructionEnvelope toExecutionInstruction(
            TradeMessage tradeMessage,
            TradeProcessingResult processingResult
    ) {
        CaptureTradeRequest request = tradeMessage.payload();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeExternalId", request.tradeExternalId());
        payload.put("sourceSystem", request.sourceSystem());
        payload.put("direction", request.direction());
        payload.put("quantity", request.quantity());
        payload.put("price", request.price());
        payload.put("tradeDate", request.tradeDate());
        payload.put("settlementDate", request.settlementDate());
        payload.put("accountId", request.accountId());
        payload.put("bookId", request.bookId());
        payload.put("securityId", request.securityId());
        payload.put("productId", request.productId());
        payload.put("metadata", request.metadata());
        payload.put("workflowStatus", processingResult.workflowStatus().name());
        payload.put("positionStatus", processingResult.positionStatus().name());
        payload.put("appliedRules", processingResult.appliedRules());

        InstructionMetadata metadata = new InstructionMetadata(
                "trade-capture-service",
                Instant.now(),
                tradeMessage.correlationId(),
                tradeMessage.ingestionId()
        );
        InstructionUserContext userContext = parseUserContext(tradeMessage.userContext(), request);
        return new PrimitiveInstructionEnvelope(
                "ExecutionInstruction",
                tradeMessage.partitionKey(),
                payload,
                metadata,
                userContext
        );
    }

    public String toJson(PrimitiveInstructionEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to map instruction payload", e);
        }
    }

    private InstructionUserContext parseUserContext(String rawUserContext, CaptureTradeRequest request) {
        if (rawUserContext == null || rawUserContext.isBlank()) {
            return new InstructionUserContext(
                    "system",
                    List.of("TRADE_CAPTURE"),
                    List.of(request.accountId()),
                    List.of(request.bookId())
            );
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(rawUserContext, Map.class);
            Object rolesObj = parsed.get("roles");
            List<String> roles = toStringList(rolesObj);
            List<String> accounts = toStringList(parsed.get("accountIds"));
            List<String> books = toStringList(parsed.get("bookIds"));
            String userId = String.valueOf(parsed.getOrDefault("userId", "unknown"));
            if (accounts.isEmpty()) {
                accounts = List.of(request.accountId());
            }
            if (books.isEmpty()) {
                books = List.of(request.bookId());
            }
            return new InstructionUserContext(userId, roles, accounts, books);
        } catch (Exception ignored) {
            return new InstructionUserContext(
                    rawUserContext,
                    List.of("TRADE_CAPTURE"),
                    List.of(request.accountId()),
                    List.of(request.bookId())
            );
        }
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                out.add(String.valueOf(item));
            }
            return out;
        }
        if (value == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        out.add(String.valueOf(value));
        return out;
    }
}
