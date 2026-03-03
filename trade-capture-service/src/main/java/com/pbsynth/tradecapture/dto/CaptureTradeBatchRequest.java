package com.pbsynth.tradecapture.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CaptureTradeBatchRequest(
        @NotBlank String sourceSystem,
        @NotEmpty List<@Valid CaptureTradeRequest> trades
) {}
