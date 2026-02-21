package com.snayvik.kpi.seed;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String PREFIX_SNAY = "SNAY";
    private static final String PREFIX_GOV = "GOV";

    private static final String BOARD_SNAY = "2001001";
    private static final String BOARD_GOV = "2001002";

    private static final String USER_ADMIN = "seed-admin";
    private static final String USER_DEV_A = "seed-dev-a";
    private static final String USER_DEV_B = "seed-dev-b";

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public DemoDataSeeder(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    @Transactional
    public void seed() {
        LocalDate today = LocalDate.now(clock);
        List<SeedTask> tasks = buildSeedTasks(today);
        List<SnapshotSeed> snapshots = buildSnapshotRows(today);

        upsertBoardMappings();
        clearSeededRows();
        seedTasks(tasks);
        seedPullRequests(tasks);
        seedCommits(tasks);
        seedTaskMetrics(tasks);
        seedAccessUsers();
        seedTimeSessions(tasks);
        seedDailySnapshots(snapshots);

        LOGGER.info("Demo seed complete: tasks={}, snapshots={} days", tasks.size(), 14);
    }

    List<SeedTask> buildSeedTasks(LocalDate today) {
        LocalDate startDate = today.minusDays(14);
        List<SeedTask> tasks = new ArrayList<>();

        for (int index = 0; index < 8; index++) {
            boolean snayTask = index < 4;
            String prefix = snayTask ? PREFIX_SNAY : PREFIX_GOV;
            String boardId = snayTask ? BOARD_SNAY : BOARD_GOV;
            String pulseId = String.format("seed%02d", index + 1);
            String taskKey = prefix + "-" + pulseId;

            Instant startedAt = startDate.plusDays(index)
                    .atTime(9 + (index % 2), 0)
                    .toInstant(ZoneOffset.UTC);
            Instant firstCommitAt = startedAt.plus(Duration.ofHours(2));

            boolean done = index % 4 != 3;
            Instant mergedAt = done ? startedAt.plus(Duration.ofHours(22 + (index * 2L))) : null;
            Instant completedAt = done && mergedAt != null ? mergedAt.plus(Duration.ofHours(6 + index)) : null;

            int commitCount = 3 + (index % 2);
            int reopenCount = index % 5 == 0 ? 1 : 0;
            double reworkRate = reopenCount > 0 ? 1.0 : 0.0;

            long leadTimeSeconds = mergedAt == null ? 0L : Duration.between(startedAt, mergedAt).getSeconds();
            long cycleTimeSeconds = completedAt == null ? 0L : Duration.between(startedAt, completedAt).getSeconds();

            double driftScore = index % 3 == 0 ? 18.0 : 4.0;
            double integrityScore = done ? 100.0 : 80.0;
            double riskScore = roundTwoDecimals(
                    done ? 26.0 + (index * 4.8) + (driftScore * 0.2) : 58.0 + (index * 1.7));

            SeedTask task = new SeedTask(
                    taskKey,
                    prefix,
                    pulseId,
                    boardId,
                    done ? "Done" : "In Progress",
                    startedAt,
                    firstCommitAt,
                    mergedAt,
                    completedAt,
                    driftScore,
                    riskScore,
                    leadTimeSeconds,
                    cycleTimeSeconds,
                    commitCount,
                    reworkRate,
                    integrityScore,
                    reopenCount,
                    snayTask ? "snayvik/platform-api" : "snayvik/governance-api",
                    500 + index,
                    index % 2 == 0 ? USER_DEV_A : USER_DEV_B);

            tasks.add(task);
        }

        return tasks;
    }

    List<SnapshotSeed> buildSnapshotRows(LocalDate today) {
        LocalDate from = today.minusDays(13);
        List<SnapshotSeed> snapshots = new ArrayList<>();

        for (int day = 0; day < 14; day++) {
            LocalDate snapshotDate = from.plusDays(day);

            snapshots.add(new SnapshotSeed(
                    snapshotDate,
                    PREFIX_SNAY,
                    hoursToSeconds(27 + (day % 4) + (day / 4)),
                    roundTwoDecimals(22.0 + (day * 1.4)),
                    roundFourDecimals(0.05 + (day * 0.004)),
                    roundFourDecimals(0.03 + ((day % 5) * 0.01))));

            snapshots.add(new SnapshotSeed(
                    snapshotDate,
                    PREFIX_GOV,
                    hoursToSeconds(33 + (day % 3) + (day / 5)),
                    roundTwoDecimals(36.0 + (day * 1.1)),
                    roundFourDecimals(0.08 + (day * 0.003)),
                    roundFourDecimals(0.05 + ((day % 4) * 0.012))));
        }

        return snapshots;
    }

    private void upsertBoardMappings() {
        jdbcTemplate.update(
                """
                insert into board_mappings(prefix, board_id, board_name)
                values (?, ?, ?)
                on conflict (prefix)
                do update set board_id = excluded.board_id,
                              board_name = excluded.board_name
                """,
                PREFIX_SNAY,
                BOARD_SNAY,
                "Snayvik Product Delivery");

        jdbcTemplate.update(
                """
                insert into board_mappings(prefix, board_id, board_name)
                values (?, ?, ?)
                on conflict (prefix)
                do update set board_id = excluded.board_id,
                              board_name = excluded.board_name
                """,
                PREFIX_GOV,
                BOARD_GOV,
                "Snayvik Governance Control");
    }

    private void clearSeededRows() {
        for (String prefix : List.of(PREFIX_SNAY, PREFIX_GOV)) {
            String likePattern = prefix + "-seed%";
            jdbcTemplate.update("delete from commits where task_key like ?", likePattern);
            jdbcTemplate.update("delete from pull_requests where task_key like ?", likePattern);
            jdbcTemplate.update("delete from policy_violations where task_key like ?", likePattern);
            jdbcTemplate.update("delete from time_sessions where task_key like ?", likePattern);
            jdbcTemplate.update("delete from task_metrics where task_key like ?", likePattern);
            jdbcTemplate.update("delete from tasks where task_key like ?", likePattern);
            jdbcTemplate.update("delete from daily_snapshots where prefix = ?", prefix);
        }

        jdbcTemplate.update("delete from access_audit_logs where performed_by = 'seed-system'");
        jdbcTemplate.update(
                "delete from user_skill_groups where user_id in (?, ?, ?)",
                USER_ADMIN,
                USER_DEV_A,
                USER_DEV_B);
    }

    private void seedTasks(List<SeedTask> tasks) {
        for (SeedTask task : tasks) {
            jdbcTemplate.update(
                    """
                    insert into tasks(task_key, prefix, pulse_id, board_id, status, started_at, first_commit_at, merged_at, completed_at, drift_score, risk_score)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (task_key)
                    do update set prefix = excluded.prefix,
                                  pulse_id = excluded.pulse_id,
                                  board_id = excluded.board_id,
                                  status = excluded.status,
                                  started_at = excluded.started_at,
                                  first_commit_at = excluded.first_commit_at,
                                  merged_at = excluded.merged_at,
                                  completed_at = excluded.completed_at,
                                  drift_score = excluded.drift_score,
                                  risk_score = excluded.risk_score
                    """,
                    task.taskKey(),
                    task.prefix(),
                    task.pulseId(),
                    task.boardId(),
                    task.status(),
                    toTimestamp(task.startedAt()),
                    toTimestamp(task.firstCommitAt()),
                    toTimestamp(task.mergedAt()),
                    toTimestamp(task.completedAt()),
                    task.driftScore(),
                    task.riskScore());
        }
    }

    private void seedPullRequests(List<SeedTask> tasks) {
        for (SeedTask task : tasks) {
            jdbcTemplate.update(
                    """
                    insert into pull_requests(task_key, repository, pr_number, opened_at, merged_at, reopen_count)
                    values (?, ?, ?, ?, ?, ?)
                    on conflict (repository, pr_number)
                    do update set task_key = excluded.task_key,
                                  opened_at = excluded.opened_at,
                                  merged_at = excluded.merged_at,
                                  reopen_count = excluded.reopen_count
                    """,
                    task.taskKey(),
                    task.repository(),
                    task.prNumber(),
                    toTimestamp(task.firstCommitAt()),
                    toTimestamp(task.mergedAt()),
                    task.reopenCount());
        }
    }

    private void seedCommits(List<SeedTask> tasks) {
        for (SeedTask task : tasks) {
            for (int commitIndex = 0; commitIndex < task.commitCount(); commitIndex++) {
                jdbcTemplate.update(
                        """
                        insert into commits(task_key, repository, commit_hash, author, committed_at)
                        values (?, ?, ?, ?, ?)
                        on conflict (repository, commit_hash)
                        do update set task_key = excluded.task_key,
                                      author = excluded.author,
                                      committed_at = excluded.committed_at
                        """,
                        task.taskKey(),
                        task.repository(),
                        buildCommitHash(task.taskKey(), commitIndex),
                        task.assigneeUserId(),
                        toTimestamp(task.firstCommitAt().plus(Duration.ofHours(commitIndex))));
            }
        }
    }

    private void seedTaskMetrics(List<SeedTask> tasks) {
        for (SeedTask task : tasks) {
            jdbcTemplate.update(
                    """
                    insert into task_metrics(task_key, lead_time_seconds, cycle_time_seconds, commit_count, rework_rate, integrity_score, drift_score, risk_score)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (task_key)
                    do update set lead_time_seconds = excluded.lead_time_seconds,
                                  cycle_time_seconds = excluded.cycle_time_seconds,
                                  commit_count = excluded.commit_count,
                                  rework_rate = excluded.rework_rate,
                                  integrity_score = excluded.integrity_score,
                                  drift_score = excluded.drift_score,
                                  risk_score = excluded.risk_score
                    """,
                    task.taskKey(),
                    task.leadTimeSeconds(),
                    task.cycleTimeSeconds(),
                    task.commitCount(),
                    task.reworkRate(),
                    task.integrityScore(),
                    task.driftScore(),
                    task.riskScore());
        }
    }

    private void seedAccessUsers() {
        upsertUser(USER_ADMIN, "seed-admin@snayvik.local", "Seed Admin");
        upsertUser(USER_DEV_A, "seed-dev-a@snayvik.local", "Seed Developer A");
        upsertUser(USER_DEV_B, "seed-dev-b@snayvik.local", "Seed Developer B");

        jdbcTemplate.update("insert into skill_groups(name) values (?) on conflict (name) do nothing", "admin");
        jdbcTemplate.update("insert into skill_groups(name) values (?) on conflict (name) do nothing", "engineering");

        assignSkillGroup(USER_ADMIN, "admin");
        assignSkillGroup(USER_DEV_A, "engineering");
        assignSkillGroup(USER_DEV_B, "engineering");

        jdbcTemplate.update(
                """
                insert into access_audit_logs(user_id, system, action, target, status, performed_by, timestamp)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                USER_DEV_B,
                "GITHUB",
                "GRANT",
                "snayvik/governance-api",
                "SUCCESS",
                "seed-system",
                toTimestamp(Instant.now(clock).minus(Duration.ofDays(1))));
    }

    private void seedTimeSessions(List<SeedTask> tasks) {
        jdbcTemplate.update(
                "delete from time_sessions where user_id in (?, ?, ?)",
                USER_ADMIN,
                USER_DEV_A,
                USER_DEV_B);

        for (int index = 0; index < tasks.size(); index++) {
            SeedTask task = tasks.get(index);
            if (index % 3 == 1) {
                continue;
            }

            Instant sessionStart = task.startedAt().plus(Duration.ofHours(1));
            int minutes = 70 + (index * 15);
            Instant sessionEnd = sessionStart.plus(Duration.ofMinutes(minutes));
            String source = index == 6 ? "MANUAL_EDIT" : "LIVE";
            String reason = "MANUAL_EDIT".equals(source) ? "Imported historical correction" : null;

            jdbcTemplate.update(
                    """
                    insert into time_sessions(task_key, user_id, start_time, end_time, duration_minutes, source, edit_reason)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    task.taskKey(),
                    task.assigneeUserId(),
                    toTimestamp(sessionStart),
                    toTimestamp(sessionEnd),
                    minutes,
                    source,
                    reason);
        }
    }

    private void seedDailySnapshots(List<SnapshotSeed> snapshots) {
        for (SnapshotSeed snapshot : snapshots) {
            jdbcTemplate.update(
                    """
                    insert into daily_snapshots(snapshot_date, prefix, avg_lead_time, avg_risk_score, drift_rate, rework_rate)
                    values (?, ?, ?, ?, ?, ?)
                    on conflict (snapshot_date, prefix)
                    do update set avg_lead_time = excluded.avg_lead_time,
                                  avg_risk_score = excluded.avg_risk_score,
                                  drift_rate = excluded.drift_rate,
                                  rework_rate = excluded.rework_rate
                    """,
                    snapshot.snapshotDate(),
                    snapshot.prefix(),
                    snapshot.avgLeadTime(),
                    snapshot.avgRiskScore(),
                    snapshot.driftRate(),
                    snapshot.reworkRate());
        }
    }

    private void upsertUser(String userId, String email, String name) {
        jdbcTemplate.update(
                """
                insert into users(id, email, name, active, deactivated_at)
                values (?, ?, ?, true, null)
                on conflict (id)
                do update set email = excluded.email,
                              name = excluded.name,
                              active = true,
                              deactivated_at = null
                """,
                userId,
                email,
                name);
    }

    private void assignSkillGroup(String userId, String skillGroup) {
        jdbcTemplate.update(
                """
                insert into user_skill_groups(user_id, skill_group_id)
                select ?, sg.id
                from skill_groups sg
                where lower(sg.name) = lower(?)
                on conflict (user_id, skill_group_id)
                do nothing
                """,
                userId,
                skillGroup);
    }

    private long hoursToSeconds(long hours) {
        return Duration.ofHours(hours).getSeconds();
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double roundFourDecimals(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private String buildCommitHash(String taskKey, int commitIndex) {
        String raw = taskKey.replace("-", "").toLowerCase() + Integer.toHexString(commitIndex + 10) + "abc123";
        StringBuilder hash = new StringBuilder();
        while (hash.length() < 40) {
            hash.append(raw);
        }
        return hash.substring(0, 40);
    }

    record SeedTask(
            String taskKey,
            String prefix,
            String pulseId,
            String boardId,
            String status,
            Instant startedAt,
            Instant firstCommitAt,
            Instant mergedAt,
            Instant completedAt,
            double driftScore,
            double riskScore,
            long leadTimeSeconds,
            long cycleTimeSeconds,
            int commitCount,
            double reworkRate,
            double integrityScore,
            int reopenCount,
            String repository,
            int prNumber,
            String assigneeUserId) {
    }

    record SnapshotSeed(
            LocalDate snapshotDate,
            String prefix,
            long avgLeadTime,
            double avgRiskScore,
            double driftRate,
            double reworkRate) {
    }
}
