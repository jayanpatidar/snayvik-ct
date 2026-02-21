package com.snayvik.kpi.ingress.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "dedupe_key", nullable = false, length = 255)
    private String dedupeKey;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message")
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
