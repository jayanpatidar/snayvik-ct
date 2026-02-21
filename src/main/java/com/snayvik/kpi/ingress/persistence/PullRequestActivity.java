package com.snayvik.kpi.ingress.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pull_requests")
public class PullRequestActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_key")
    private String taskKey;

    @Column(nullable = false, length = 255)
    private String repository;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "reopen_count", nullable = false)
    private Integer reopenCount = 0;

    public Long getId() {
        return id;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Integer getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(Integer prNumber) {
        this.prNumber = prNumber;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(Instant mergedAt) {
        this.mergedAt = mergedAt;
    }

    public Integer getReopenCount() {
        return reopenCount;
    }

    public void setReopenCount(Integer reopenCount) {
        this.reopenCount = reopenCount;
    }
}
