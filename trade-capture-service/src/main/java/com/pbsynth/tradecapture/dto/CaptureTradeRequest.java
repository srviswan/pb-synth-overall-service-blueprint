package com.pbsynth.tradecapture.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CaptureTradeRequest(
        @NotBlank String sourceSystem,
        @NotBlank String tradeExternalId,
        @NotBlank String accountId,
        @NotBlank String bookId,
        @NotBlank String securityId,
        @NotBlank @Pattern(regexp = "BUY|SELL") String direction,
        @NotNull @DecimalMin(value = "0.0000001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0") BigDecimal price,
        @NotNull LocalDate tradeDate,
        LocalDate settlementDate,
        String productId,
        Map<String, Object> metadata
) {}
