package com.snayvik.kpi.kpi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "daily_snapshots")
@IdClass(DailySnapshotId.class)
public class DailySnapshot {

    @Id
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Id
    @Column(nullable = false, length = 16)
    private String prefix;

    @Column(name = "avg_lead_time")
    private Long avgLeadTime;

    @Column(name = "avg_risk_score")
    private Double avgRiskScore;

    @Column(name = "drift_rate")
    private Double driftRate;

    @Column(name = "rework_rate")
    private Double reworkRate;

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Long getAvgLeadTime() {
        return avgLeadTime;
    }

    public void setAvgLeadTime(Long avgLeadTime) {
        this.avgLeadTime = avgLeadTime;
    }

    public Double getAvgRiskScore() {
        return avgRiskScore;
    }

    public void setAvgRiskScore(Double avgRiskScore) {
        this.avgRiskScore = avgRiskScore;
    }

    public Double getDriftRate() {
        return driftRate;
    }

    public void setDriftRate(Double driftRate) {
        this.driftRate = driftRate;
    }

    public Double getReworkRate() {
        return reworkRate;
    }

    public void setReworkRate(Double reworkRate) {
        this.reworkRate = reworkRate;
    }
}
