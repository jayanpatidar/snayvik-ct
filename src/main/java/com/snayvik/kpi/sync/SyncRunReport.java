package com.snayvik.kpi.sync;

import java.time.Instant;

public record SyncRunReport(
        SyncRunType runType,
        boolean enabled,
        Instant startedAt,
        Instant finishedAt,
        int boardsScanned,
        int mondayItemsProcessed,
        int githubRepositoriesScanned,
        int githubPullRequestsProcessed,
        int githubCommitsProcessed,
        int touchedTasks,
        int tasksRecomputed) {

    public static SyncRunReport disabled(SyncRunType runType, Instant at) {
        return new SyncRunReport(runType, false, at, at, 0, 0, 0, 0, 0, 0, 0);
    }
}
