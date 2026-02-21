package com.snayvik.kpi.kpi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_metrics")
public class TaskMetrics {

    @Id
    @Column(name = "task_key", nullable = false, length = 64)
    private String taskKey;

    @Column(name = "lead_time_seconds")
    private Long leadTimeSeconds;

    @Column(name = "cycle_time_seconds")
    private Long cycleTimeSeconds;

    @Column(name = "commit_count", nullable = false)
    private Integer commitCount = 0;

    @Column(name = "rework_rate")
    private Double reworkRate = 0.0;

    @Column(name = "integrity_score")
    private Double integrityScore = 100.0;

    @Column(name = "drift_score")
    private Double driftScore = 0.0;

    @Column(name = "risk_score")
    private Double riskScore = 0.0;

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public Long getLeadTimeSeconds() {
        return leadTimeSeconds;
    }

    public void setLeadTimeSeconds(Long leadTimeSeconds) {
        this.leadTimeSeconds = leadTimeSeconds;
    }

    public Long getCycleTimeSeconds() {
        return cycleTimeSeconds;
    }

    public void setCycleTimeSeconds(Long cycleTimeSeconds) {
        this.cycleTimeSeconds = cycleTimeSeconds;
    }

    public Integer getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(Integer commitCount) {
        this.commitCount = commitCount;
    }

    public Double getReworkRate() {
        return reworkRate;
    }

    public void setReworkRate(Double reworkRate) {
        this.reworkRate = reworkRate;
    }

    public Double getIntegrityScore() {
        return integrityScore;
    }

    public void setIntegrityScore(Double integrityScore) {
        this.integrityScore = integrityScore;
    }

    public Double getDriftScore() {
        return driftScore;
    }

    public void setDriftScore(Double driftScore) {
        this.driftScore = driftScore;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }
}
