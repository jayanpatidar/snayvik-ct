package com.snayvik.kpi.ingress.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @Column(name = "task_key", nullable = false, length = 64)
    private String taskKey;

    @Column(nullable = false, length = 16)
    private String prefix;

    @Column(name = "pulse_id", nullable = false, length = 64)
    private String pulseId;

    @Column(name = "board_id", nullable = false, length = 64)
    private String boardId;

    @Column(length = 64)
    private String status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "first_commit_at")
    private Instant firstCommitAt;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPulseId() {
        return pulseId;
    }

    public void setPulseId(String pulseId) {
        this.pulseId = pulseId;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFirstCommitAt() {
        return firstCommitAt;
    }

    public void setFirstCommitAt(Instant firstCommitAt) {
        this.firstCommitAt = firstCommitAt;
    }

    public Instant getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(Instant mergedAt) {
        this.mergedAt = mergedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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
