package com.pbsynth.tradecapture.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "ingestion_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ingestion_idempotency", columnNames = "idempotency_key")
        }
)
public class IngestionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ingestion_id", nullable = false)
    private String ingestionId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "trade_external_id", nullable = false, length = 255)
    private String tradeExternalId;

    @Column(name = "source_system", nullable = false, length = 255)
    private String sourceSystem;

    @Column(name = "partition_key", nullable = false, length = 255)
    private String partitionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 64)
    private IngestionStatus status;

    @Lob
    @Column(name = "request_payload_json", nullable = false, columnDefinition = "TEXT")
    private String requestPayloadJson;

    @Lob
    @Column(name = "instruction_envelope_json", columnDefinition = "TEXT")
    private String instructionEnvelopeJson;

    @Column(name = "error_code", length = 255)
    private String errorCode;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "user_context", length = 2048)
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

    public String getIngestionId() {
        return ingestionId;
    }

    public void setIngestionId(String ingestionId) {
        this.ingestionId = ingestionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getTradeExternalId() {
        return tradeExternalId;
    }

    public void setTradeExternalId(String tradeExternalId) {
        this.tradeExternalId = tradeExternalId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public IngestionStatus getStatus() {
        return status;
    }

    public void setStatus(IngestionStatus status) {
        this.status = status;
    }

    public String getRequestPayloadJson() {
        return requestPayloadJson;
    }

    public void setRequestPayloadJson(String requestPayloadJson) {
        this.requestPayloadJson = requestPayloadJson;
    }

    public String getInstructionEnvelopeJson() {
        return instructionEnvelopeJson;
    }

    public void setInstructionEnvelopeJson(String instructionEnvelopeJson) {
        this.instructionEnvelopeJson = instructionEnvelopeJson;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getUserContext() {
        return userContext;
    }

    public void setUserContext(String userContext) {
        this.userContext = userContext;
    }
}
