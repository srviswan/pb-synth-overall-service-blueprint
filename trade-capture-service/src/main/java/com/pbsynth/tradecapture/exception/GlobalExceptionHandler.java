package com.pbsynth.tradecapture.exception;

import com.pbsynth.tradecapture.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                request.getHeader("X-Correlation-Id"),
                Instant.now(),
                details
        ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        HttpStatus status = switch (ex.getCode()) {
            case "DUPLICATE_IDEMPOTENCY_KEY" -> HttpStatus.CONFLICT;
            case "INGESTION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                request.getHeader("X-Correlation-Id"),
                Instant.now(),
                List.of()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                "INTERNAL_ERROR",
                "Unexpected server error",
                request.getHeader("X-Correlation-Id"),
                Instant.now(),
                List.of(ex.getClass().getSimpleName())
        ));
    }
}
