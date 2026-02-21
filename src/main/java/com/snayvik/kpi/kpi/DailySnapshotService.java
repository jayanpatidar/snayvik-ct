package com.snayvik.kpi.kpi;

import com.snayvik.kpi.ingress.persistence.Task;
import com.snayvik.kpi.ingress.persistence.TaskRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailySnapshotService {

    private final TaskRepository taskRepository;
    private final TaskMetricsRepository taskMetricsRepository;
    private final DailySnapshotRepository dailySnapshotRepository;

    public DailySnapshotService(
            TaskRepository taskRepository,
            TaskMetricsRepository taskMetricsRepository,
            DailySnapshotRepository dailySnapshotRepository) {
        this.taskRepository = taskRepository;
        this.taskMetricsRepository = taskMetricsRepository;
        this.dailySnapshotRepository = dailySnapshotRepository;
    }

    @Scheduled(cron = "${app.snapshots.cron:0 5 0 * * *}")
    @Transactional
    public void captureDailySnapshot() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Task> tasks = taskRepository.findAll();
        if (tasks.isEmpty()) {
            return;
        }

        Map<String, List<Task>> tasksByPrefix = tasks.stream().collect(Collectors.groupingBy(Task::getPrefix));
        Map<String, TaskMetrics> metricsByTaskKey = taskMetricsRepository.findAll().stream()
                .collect(Collectors.toMap(TaskMetrics::getTaskKey, metrics -> metrics));

        for (Map.Entry<String, List<Task>> entry : tasksByPrefix.entrySet()) {
            String prefix = entry.getKey();
            List<Task> prefixTasks = entry.getValue();
            DailySnapshot snapshot = new DailySnapshot();
            snapshot.setSnapshotDate(today);
            snapshot.setPrefix(prefix);
            snapshot.setAvgLeadTime(averageLeadTime(prefixTasks, metricsByTaskKey));
            snapshot.setAvgRiskScore(averageRisk(prefixTasks, metricsByTaskKey));
            snapshot.setDriftRate(driftRate(prefixTasks));
            snapshot.setReworkRate(averageRework(prefixTasks, metricsByTaskKey));
            dailySnapshotRepository.save(snapshot);
        }
    }

    @Transactional(readOnly = true)
    public List<DailySnapshot> getSnapshotsForLastDays(int days) {
        LocalDate fromDate = LocalDate.now(ZoneOffset.UTC).minusDays(Math.max(days, 1L) - 1L);
        return dailySnapshotRepository.findBySnapshotDateGreaterThanEqualOrderBySnapshotDateAscPrefixAsc(fromDate);
    }

    private Long averageLeadTime(List<Task> tasks, Map<String, TaskMetrics> metricsByTaskKey) {
        List<Long> leadTimes = tasks.stream()
                .map(task -> metricsByTaskKey.get(task.getTaskKey()))
                .filter(java.util.Objects::nonNull)
                .map(TaskMetrics::getLeadTimeSeconds)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (leadTimes.isEmpty()) {
            return 0L;
        }
        return Math.round(leadTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
    }

    private Double averageRisk(List<Task> tasks, Map<String, TaskMetrics> metricsByTaskKey) {
        List<Double> risks = tasks.stream()
                .map(task -> metricsByTaskKey.get(task.getTaskKey()))
                .filter(java.util.Objects::nonNull)
                .map(TaskMetrics::getRiskScore)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (risks.isEmpty()) {
            return 0.0;
        }
        return roundTwoDecimals(risks.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private Double driftRate(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return 0.0;
        }
        long driftedTasks = tasks.stream()
                .filter(task -> task.getDriftScore() != null && task.getDriftScore() > 0.0)
                .count();
        return roundTwoDecimals((double) driftedTasks / tasks.size());
    }

    private Double averageRework(List<Task> tasks, Map<String, TaskMetrics> metricsByTaskKey) {
        List<Double> reworks = tasks.stream()
                .map(task -> metricsByTaskKey.get(task.getTaskKey()))
                .filter(java.util.Objects::nonNull)
                .map(TaskMetrics::getReworkRate)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (reworks.isEmpty()) {
            return 0.0;
        }
        return roundTwoDecimals(reworks.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
