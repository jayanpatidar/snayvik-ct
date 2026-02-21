package com.snayvik.kpi.sync;

import java.time.Instant;

public record GitHubCommitSnapshot(
        String commitHash,
        String author,
        String message,
        Instant committedAt) {
}
