package com.snayvik.kpi.threshold;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "threshold_policies")
public class ThresholdPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "violation_type", nullable = false, length = 128)
    private String violationType;

    @Column(name = "threshold_count", nullable = false)
    private Integer thresholdCount;

    @Column(name = "time_window_days", nullable = false)
    private Integer timeWindowDays;

    @Column(name = "escalation_level", nullable = false, length = 32)
    private String escalationLevel;

    @Column(name = "notify_email")
    private String notifyEmail;

    @Column(name = "notify_slack")
    private String notifySlack;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public Integer getThresholdCount() {
        return thresholdCount;
    }

    public void setThresholdCount(Integer thresholdCount) {
        this.thresholdCount = thresholdCount;
    }

    public Integer getTimeWindowDays() {
        return timeWindowDays;
    }

    public void setTimeWindowDays(Integer timeWindowDays) {
        this.timeWindowDays = timeWindowDays;
    }

    public String getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(String escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public String getNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(String notifyEmail) {
        this.notifyEmail = notifyEmail;
    }

    public String getNotifySlack() {
        return notifySlack;
    }

    public void setNotifySlack(String notifySlack) {
        this.notifySlack = notifySlack;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
