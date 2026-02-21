package com.snayvik.kpi.sync;

import java.time.Instant;

public record MondayTaskSnapshot(
        String pulseId,
        String status,
        Instant startedAt,
        Instant completedAt,
        Instant descriptionUpdatedAt) {
}
