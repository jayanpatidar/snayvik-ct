package com.snayvik.kpi.threshold;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "threshold_notification_markers")
public class ThresholdNotificationMarker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "threshold_id", nullable = false)
    private Long thresholdId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "window_start", nullable = false)
    private LocalDate windowStart;

    @Column(name = "last_notified_at", nullable = false)
    private Instant lastNotifiedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getThresholdId() {
        return thresholdId;
    }

    public void setThresholdId(Long thresholdId) {
        this.thresholdId = thresholdId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDate windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(Instant lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }
}
