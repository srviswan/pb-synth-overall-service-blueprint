package com.pbsynth.tradecapture.service;

import com.pbsynth.tradecapture.dto.CaptureTradeRequest;
import com.pbsynth.tradecapture.exception.ApiException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ValidationService {
    public void validate(CaptureTradeRequest request) {
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("VALIDATION_ERROR", "quantity must be > 0");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("VALIDATION_ERROR", "price must be >= 0");
        }
    }
}
