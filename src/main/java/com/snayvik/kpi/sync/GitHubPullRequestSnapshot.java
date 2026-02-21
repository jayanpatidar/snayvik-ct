package com.snayvik.kpi.sync;

import java.time.Instant;
import java.util.List;

public record GitHubPullRequestSnapshot(
        String repository,
        int prNumber,
        String title,
        String branchName,
        Instant openedAt,
        Instant mergedAt,
        int reopenCount,
        List<GitHubCommitSnapshot> commits) {

    public GitHubPullRequestSnapshot {
        commits = commits == null ? List.of() : List.copyOf(commits);
    }
}
