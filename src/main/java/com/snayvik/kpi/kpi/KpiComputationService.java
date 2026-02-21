package com.snayvik.kpi.kpi;

import com.snayvik.kpi.ingress.persistence.CommitActivity;
import com.snayvik.kpi.ingress.persistence.CommitActivityRepository;
import com.snayvik.kpi.ingress.persistence.PullRequestActivity;
import com.snayvik.kpi.ingress.persistence.PullRequestActivityRepository;
import com.snayvik.kpi.ingress.persistence.Task;
import com.snayvik.kpi.ingress.persistence.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KpiComputationService {

    private final TaskRepository taskRepository;
    private final PullRequestActivityRepository pullRequestActivityRepository;
    private final CommitActivityRepository commitActivityRepository;
    private final TaskMetricsRepository taskMetricsRepository;

    public KpiComputationService(
            TaskRepository taskRepository,
            PullRequestActivityRepository pullRequestActivityRepository,
            CommitActivityRepository commitActivityRepository,
            TaskMetricsRepository taskMetricsRepository) {
        this.taskRepository = taskRepository;
        this.pullRequestActivityRepository = pullRequestActivityRepository;
        this.commitActivityRepository = commitActivityRepository;
        this.taskMetricsRepository = taskMetricsRepository;
    }

    @Transactional
    public void recomputeTaskMetrics(String taskKey) {
        Task task = taskRepository.findById(taskKey).orElse(null);
        if (task == null) {
            return;
        }

        List<PullRequestActivity> pullRequests = pullRequestActivityRepository.findAllByTaskKey(taskKey);
        List<CommitActivity> commits = commitActivityRepository.findAllByTaskKey(taskKey);

        TaskMetrics taskMetrics = taskMetricsRepository.findById(taskKey).orElseGet(TaskMetrics::new);
        taskMetrics.setTaskKey(taskKey);

        taskMetrics.setLeadTimeSeconds(computeLeadTime(task, pullRequests));
        taskMetrics.setCycleTimeSeconds(computeCycleTime(task));
        taskMetrics.setCommitCount(commits.size());
        taskMetrics.setReworkRate(computeReworkRate(pullRequests));

        double integrityScore = computeIntegrityScore(task, pullRequests);
        taskMetrics.setIntegrityScore(integrityScore);
        taskMetrics.setDriftScore(task.getDriftScore() == null ? 0.0 : task.getDriftScore());
        taskMetrics.setRiskScore(computeRisk(taskMetrics));

        taskMetricsRepository.save(taskMetrics);
    }

    @Transactional
    public void recomputeAllTaskMetrics() {
        for (Task task : taskRepository.findAll()) {
            recomputeTaskMetrics(task.getTaskKey());
        }
    }

    private Long computeLeadTime(Task task, List<PullRequestActivity> pullRequests) {
        Instant startedAt = task.getStartedAt();
        if (startedAt == null) {
            return null;
        }

        Instant mergedAt = task.getMergedAt();
        if (mergedAt == null) {
            mergedAt = pullRequests.stream()
                    .map(PullRequestActivity::getMergedAt)
                    .filter(java.util.Objects::nonNull)
                    .min(Instant::compareTo)
                    .orElse(null);
        }
        if (mergedAt == null || mergedAt.isBefore(startedAt)) {
            return null;
        }
        return Duration.between(startedAt, mergedAt).getSeconds();
    }

    private Long computeCycleTime(Task task) {
        Instant startedAt = task.getStartedAt();
        Instant completedAt = task.getCompletedAt();
        if (startedAt == null || completedAt == null || completedAt.isBefore(startedAt)) {
            return null;
        }
        return Duration.between(startedAt, completedAt).getSeconds();
    }

    private double computeReworkRate(List<PullRequestActivity> pullRequests) {
        if (pullRequests.isEmpty()) {
            return 0.0;
        }
        int reopenTotal = pullRequests.stream()
                .map(PullRequestActivity::getReopenCount)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        return (double) reopenTotal / pullRequests.size();
    }

    private double computeIntegrityScore(Task task, List<PullRequestActivity> pullRequests) {
        String status = task.getStatus() == null ? "" : task.getStatus().trim();
        boolean done = status.equalsIgnoreCase("done");
        boolean hasMergedPr = pullRequests.stream().anyMatch(pr -> pr.getMergedAt() != null);
        if (done && !hasMergedPr) {
            return 0.0;
        }
        return 100.0;
    }

    private double computeRisk(TaskMetrics taskMetrics) {
        double driftComponent = (taskMetrics.getDriftScore() == null ? 0.0 : taskMetrics.getDriftScore()) * 0.4;
        double reworkComponent = (taskMetrics.getReworkRate() == null ? 0.0 : taskMetrics.getReworkRate()) * 100.0 * 0.4;
        double integrityPenalty = (100.0 - (taskMetrics.getIntegrityScore() == null ? 100.0 : taskMetrics.getIntegrityScore())) * 0.2;
        return roundTwoDecimals(driftComponent + reworkComponent + integrityPenalty);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
