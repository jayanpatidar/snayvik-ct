package com.snayvik.kpi.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DemoDataSeederTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-02-21T10:00:00Z");

    private DemoDataSeeder seeder;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        seeder = new DemoDataSeeder(jdbcTemplate, fixedClock);
    }

    @Test
    void buildsTwoWeeksOfDailySnapshotsAcrossTwoPrefixes() {
        LocalDate today = LocalDate.of(2026, 2, 21);

        var snapshots = seeder.buildSnapshotRows(today);

        assertThat(snapshots).hasSize(28);
        assertThat(snapshots.stream().map(DemoDataSeeder.SnapshotSeed::snapshotDate).distinct()).hasSize(14);
        assertThat(snapshots.stream().map(DemoDataSeeder.SnapshotSeed::prefix).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(Set.of("SNAY", "GOV"));
        assertThat(snapshots.stream().map(DemoDataSeeder.SnapshotSeed::snapshotDate).min(LocalDate::compareTo))
                .contains(LocalDate.of(2026, 2, 8));
        assertThat(snapshots.stream().map(DemoDataSeeder.SnapshotSeed::snapshotDate).max(LocalDate::compareTo))
                .contains(LocalDate.of(2026, 2, 21));
    }

    @Test
    void buildsTaskDataForTwoWeekWindowWithExpectedMetrics() {
        LocalDate today = LocalDate.of(2026, 2, 21);

        var tasks = seeder.buildSeedTasks(today);

        assertThat(tasks).hasSize(8);
        assertThat(tasks)
                .allSatisfy(task -> {
                    assertThat(task.taskKey()).contains("-seed");
                    assertThat(task.boardId()).isIn("2001001", "2001002");
                    assertThat(task.startedAt()).isAfterOrEqualTo(Instant.parse("2026-02-07T00:00:00Z"));
                    assertThat(task.startedAt()).isBefore(Instant.parse("2026-02-15T23:59:59Z"));
                    assertThat(task.commitCount()).isGreaterThanOrEqualTo(3);
                    assertThat(task.riskScore()).isBetween(20.0, 80.0);
                });

        long doneCount = tasks.stream().filter(task -> "Done".equalsIgnoreCase(task.status())).count();
        long inProgressCount = tasks.stream().filter(task -> "In Progress".equalsIgnoreCase(task.status())).count();

        assertThat(doneCount).isEqualTo(6L);
        assertThat(inProgressCount).isEqualTo(2L);
    }
}
