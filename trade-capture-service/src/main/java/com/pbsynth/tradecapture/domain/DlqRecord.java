package com.pbsynth.tradecapture.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dlq_record")
public class DlqRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false)
    private String ingestionId;
    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String payload;
    @Column(nullable = false, length = 2048)
    private String failureReason;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getIngestionId() { return ingestionId; }
    public void setIngestionId(String ingestionId) { this.ingestionId = ingestionId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
