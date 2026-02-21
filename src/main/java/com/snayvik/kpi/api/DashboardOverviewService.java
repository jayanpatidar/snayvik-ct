package com.snayvik.kpi.api;

import com.snayvik.kpi.ingress.persistence.Task;
import com.snayvik.kpi.ingress.persistence.TaskRepository;
import com.snayvik.kpi.kpi.DailySnapshot;
import com.snayvik.kpi.kpi.DailySnapshotService;
import com.snayvik.kpi.kpi.TaskMetrics;
import com.snayvik.kpi.kpi.TaskMetricsRepository;
import com.snayvik.kpi.policy.PolicyViolation;
import com.snayvik.kpi.policy.PolicyViolationRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardOverviewService {

    private final TaskRepository taskRepository;
    private final TaskMetricsRepository taskMetricsRepository;
    private final DailySnapshotService dailySnapshotService;
    private final PolicyViolationRepository policyViolationRepository;

    public DashboardOverviewService(
            TaskRepository taskRepository,
            TaskMetricsRepository taskMetricsRepository,
            DailySnapshotService dailySnapshotService,
            PolicyViolationRepository policyViolationRepository) {
        this.taskRepository = taskRepository;
        this.taskMetricsRepository = taskMetricsRepository;
        this.dailySnapshotService = dailySnapshotService;
        this.policyViolationRepository = policyViolationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse buildOverview(int days) {
        List<Task> tasks = taskRepository.findAll();
        List<TaskMetrics> metrics = taskMetricsRepository.findAll();
        List<DailySnapshot> snapshots = dailySnapshotService.getSnapshotsForLastDays(days);
        List<PolicyViolation> openViolations = policyViolationRepository.findByResolvedOrderByCreatedAtDesc(false);

        Map<String, Task> tasksByTaskKey = tasks.stream()
                .collect(Collectors.toMap(Task::getTaskKey, Function.identity(), (existing, ignored) -> existing));
        Map<String, TaskMetrics> metricsByTaskKey = metrics.stream()
                .collect(Collectors.toMap(TaskMetrics::getTaskKey, Function.identity(), (existing, ignored) -> existing));

        return new DashboardOverviewResponse(
                tasks.size(),
                averageLong(metrics.stream().map(TaskMetrics::getLeadTimeSeconds).toList()),
                averageDouble(metrics.stream().map(TaskMetrics::getRiskScore).toList()),
                averageDouble(metrics.stream().map(TaskMetrics::getReworkRate).toList()),
                snapshots.stream().map(this::snapshotDto).toList(),
                buildRiskBandSummary(metrics),
                buildStatusSummary(tasks),
                buildPrefixSummary(tasks, metricsByTaskKey),
                buildTopRiskTasks(metrics, tasksByTaskKey),
                buildViolationSummary(openViolations));
    }

    @Transactional(readOnly = true)
    public List<SnapshotDto> snapshotsForDays(int days) {
        return dailySnapshotService.getSnapshotsForLastDays(days).stream().map(this::snapshotDto).toList();
    }

    private List<PrefixSummaryRow> buildPrefixSummary(List<Task> tasks, Map<String, TaskMetrics> metricsByTaskKey) {
        Map<String, List<Task>> byPrefix = tasks.stream().collect(Collectors.groupingBy(Task::getPrefix));
        return byPrefix.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String prefix = entry.getKey();
                    List<Task> prefixTasks = entry.getValue();
                    List<TaskMetrics> prefixMetrics = prefixTasks.stream()
                            .map(task -> metricsByTaskKey.get(task.getTaskKey()))
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    long doneCount = prefixTasks.stream()
                            .filter(task -> "done".equalsIgnoreCase(valueOrBlank(task.getStatus())))
                            .count();
                    long driftedCount = prefixTasks.stream()
                            .filter(task -> task.getDriftScore() != null && task.getDriftScore() > 0.0)
                            .count();
                    long highRiskCount = prefixMetrics.stream()
                            .map(TaskMetrics::getRiskScore)
                            .filter(java.util.Objects::nonNull)
                            .filter(score -> score > 60.0)
                            .count();
                    return new PrefixSummaryRow(
                            prefix,
                            prefixTasks.size(),
                            (int) doneCount,
                            (int) driftedCount,
                            (int) highRiskCount,
                            averageLong(prefixMetrics.stream().map(TaskMetrics::getLeadTimeSeconds).toList()),
                            averageDouble(prefixMetrics.stream().map(TaskMetrics::getRiskScore).toList()),
                            averageDouble(prefixMetrics.stream().map(TaskMetrics::getReworkRate).toList()));
                })
                .toList();
    }

    private List<StatusSummaryRow> buildStatusSummary(List<Task> tasks) {
        Map<String, Long> counts = tasks.stream()
                .map(task -> normalizeStatus(task.getStatus()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new StatusSummaryRow(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }

    private List<TopRiskTaskRow> buildTopRiskTasks(List<TaskMetrics> metrics, Map<String, Task> tasksByTaskKey) {
        return metrics.stream()
                .sorted(Comparator.<TaskMetrics, Double>comparing(metric -> coalesceDouble(metric.getRiskScore())).reversed()
                        .thenComparing(TaskMetrics::getTaskKey))
                .limit(10)
                .map(metric -> {
                    Task task = tasksByTaskKey.get(metric.getTaskKey());
                    return new TopRiskTaskRow(
                            metric.getTaskKey(),
                            task != null ? task.getPrefix() : "UNKNOWN",
                            task != null ? normalizeStatus(task.getStatus()) : "UNKNOWN",
                            roundTwoDecimals(coalesceDouble(metric.getRiskScore())),
                            metric.getLeadTimeSeconds() == null ? 0L : metric.getLeadTimeSeconds(),
                            roundTwoDecimals(coalesceDouble(metric.getReworkRate())),
                            metric.getCommitCount() == null ? 0 : metric.getCommitCount(),
                            roundTwoDecimals(coalesceDouble(metric.getDriftScore())));
                })
                .toList();
    }

    private RiskBandSummary buildRiskBandSummary(List<TaskMetrics> metrics) {
        int green = 0;
        int yellow = 0;
        int red = 0;
        int unknown = 0;
        for (TaskMetrics metric : metrics) {
            Double score = metric.getRiskScore();
            if (score == null) {
                unknown++;
            } else if (score <= 30.0) {
                green++;
            } else if (score <= 60.0) {
                yellow++;
            } else {
                red++;
            }
        }
        return new RiskBandSummary(green, yellow, red, unknown);
    }

    private ViolationSummary buildViolationSummary(List<PolicyViolation> openViolations) {
        Map<String, Integer> bySeverity = openViolations.stream()
                .collect(Collectors.toMap(
                        violation -> normalizeSeverity(violation.getSeverity()),
                        violation -> 1,
                        Integer::sum,
                        LinkedHashMap::new));
        return new ViolationSummary(
                openViolations.size(),
                bySeverity.getOrDefault("HIGH", 0),
                bySeverity.getOrDefault("MEDIUM", 0),
                bySeverity.getOrDefault("WARN", 0),
                bySeverity.getOrDefault("LOW", 0),
                bySeverity);
    }

    private SnapshotDto snapshotDto(DailySnapshot snapshot) {
        return new SnapshotDto(
                snapshot.getSnapshotDate().toString(),
                snapshot.getPrefix(),
                snapshot.getAvgLeadTime(),
                snapshot.getAvgRiskScore(),
                snapshot.getDriftRate(),
                snapshot.getReworkRate());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return status.trim();
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "UNKNOWN";
        }
        return severity.trim().toUpperCase(Locale.ROOT);
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private double coalesceDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private long averageLong(List<Long> values) {
        List<Long> nonNull = values.stream().filter(java.util.Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return 0L;
        }
        return Math.round(nonNull.stream().mapToLong(Long::longValue).average().orElse(0.0));
    }

    private double averageDouble(List<Double> values) {
        List<Double> nonNull = values.stream().filter(java.util.Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return 0.0;
        }
        return roundTwoDecimals(nonNull.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record DashboardOverviewResponse(
            int taskCount,
            long avgLeadTimeSeconds,
            double avgRiskScore,
            double avgReworkRate,
            List<SnapshotDto> snapshots,
            RiskBandSummary riskBands,
            List<StatusSummaryRow> statusBreakdown,
            List<PrefixSummaryRow> prefixSummary,
            List<TopRiskTaskRow> topRiskTasks,
            ViolationSummary openViolations) {}

    public record SnapshotDto(
            String date,
            String prefix,
            Long avgLeadTime,
            Double avgRiskScore,
            Double driftRate,
            Double reworkRate) {}

    public record StatusSummaryRow(String status, int count) {}

    public record PrefixSummaryRow(
            String prefix,
            int taskCount,
            int doneCount,
            int driftedCount,
            int highRiskCount,
            long avgLeadTimeSeconds,
            double avgRiskScore,
            double avgReworkRate) {}

    public record TopRiskTaskRow(
            String taskKey,
            String prefix,
            String status,
            double riskScore,
            long leadTimeSeconds,
            double reworkRate,
            int commitCount,
            double driftScore) {}

    public record RiskBandSummary(int green, int yellow, int red, int unknown) {}

    public record ViolationSummary(
            int totalOpen, int high, int medium, int warn, int low, Map<String, Integer> bySeverity) {}
}
