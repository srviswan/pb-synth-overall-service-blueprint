package com.pbsynth.tradecapture.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ingestion_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ingestion_idempotency", columnNames = "idempotencyKey")
})
public class IngestionRecord {
    @Id
    private String ingestionId;
    @Column(nullable = false)
    private String idempotencyKey;
    @Column(nullable = false)
    private String tradeExternalId;
    @Column(nullable = false)
    private String sourceSystem;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestionStatus status;
    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String requestPayloadJson;
    private String errorCode;
    @Column(length = 2048)
    private String errorMessage;
    @Column(nullable = false)
    private int attemptCount = 0;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    private String correlationId;
    @Column(length = 2048)
    private String userContext;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getIngestionId() { return ingestionId; }
    public void setIngestionId(String ingestionId) { this.ingestionId = ingestionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getTradeExternalId() { return tradeExternalId; }
    public void setTradeExternalId(String tradeExternalId) { this.tradeExternalId = tradeExternalId; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public IngestionStatus getStatus() { return status; }
    public void setStatus(IngestionStatus status) { this.status = status; }
    public String getRequestPayloadJson() { return requestPayloadJson; }
    public void setRequestPayloadJson(String requestPayloadJson) { this.requestPayloadJson = requestPayloadJson; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getUserContext() { return userContext; }
    public void setUserContext(String userContext) { this.userContext = userContext; }
}
