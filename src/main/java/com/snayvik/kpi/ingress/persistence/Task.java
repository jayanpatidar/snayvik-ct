package com.snayvik.kpi.ingress.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
}
