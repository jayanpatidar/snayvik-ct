package com.snayvik.kpi.time;

import com.snayvik.kpi.ingress.persistence.CommitActivityRepository;
import com.snayvik.kpi.ingress.persistence.PullRequestActivity;
import com.snayvik.kpi.ingress.persistence.PullRequestActivityRepository;
import com.snayvik.kpi.ingress.persistence.Task;
import com.snayvik.kpi.ingress.persistence.TaskRepository;
import java.util.LinkedHashMap;
import com.snayvik.kpi.policy.PolicyViolationService;
import com.snayvik.kpi.policy.ViolationType;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimeGovernanceService {

    private final TaskRepository taskRepository;
    private final PullRequestActivityRepository pullRequestActivityRepository;
    private final CommitActivityRepository commitActivityRepository;
    private final TimeSessionRepository timeSessionRepository;
    private final PolicyViolationService policyViolationService;

    public TimeGovernanceService(
            TaskRepository taskRepository,
            PullRequestActivityRepository pullRequestActivityRepository,
            CommitActivityRepository commitActivityRepository,
            TimeSessionRepository timeSessionRepository,
            PolicyViolationService policyViolationService) {
        this.taskRepository = taskRepository;
        this.pullRequestActivityRepository = pullRequestActivityRepository;
        this.commitActivityRepository = commitActivityRepository;
        this.timeSessionRepository = timeSessionRepository;
        this.policyViolationService = policyViolationService;
    }

    @Scheduled(cron = "${app.time-governance.cron:0 */15 * * * *}")
    @Transactional
    public void evaluateAllTasks() {
        for (Task task : taskRepository.findAll()) {
            evaluateTask(task.getTaskKey());
        }
    }

    @Transactional
    public void evaluateTask(String taskKey) {
        Task task = taskRepository.findById(taskKey).orElse(null);
        if (task == null) {
            return;
        }

        List<com.snayvik.kpi.ingress.persistence.CommitActivity> commits = commitActivityRepository.findAllByTaskKey(taskKey);
        List<PullRequestActivity> pullRequests = pullRequestActivityRepository.findAllByTaskKey(taskKey);
        long trackedMinutes = timeSessionRepository.sumDurationByTaskKey(taskKey);

        boolean hasEngineeringActivity = !commits.isEmpty() || !pullRequests.isEmpty();
        if (hasEngineeringActivity && trackedMinutes <= 0) {
            policyViolationService.createViolation(
                    taskKey,
                    null,
                    ViolationType.UNTRACKED_WORK,
                    "MEDIUM",
                    "Engineering activity detected with no tracked time.");
        }

        boolean isDone = task.getStatus() != null && task.getStatus().equalsIgnoreCase("done");
        boolean hasMergedPr = pullRequests.stream().anyMatch(pr -> pr.getMergedAt() != null);
        if (isDone && hasMergedPr && trackedMinutes <= 0) {
            policyViolationService.createViolation(
                    taskKey,
                    null,
                    ViolationType.NO_TIME_ON_COMPLETED_TASK,
                    "HIGH",
                    "Completed task has merged PR but no tracked time.");
        }

        if (!hasEngineeringActivity && trackedMinutes > 0) {
            policyViolationService.createViolation(
                    taskKey,
                    null,
                    ViolationType.UNJUSTIFIED_TIME,
                    "WARN",
                    "Tracked time exists without meaningful GitHub or monday activity.");
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> summaryByTask() {
        return taskRepository.findAll().stream().map(task -> {
            List<com.snayvik.kpi.ingress.persistence.CommitActivity> commits = commitActivityRepository.findAllByTaskKey(task.getTaskKey());
            List<PullRequestActivity> pullRequests = pullRequestActivityRepository.findAllByTaskKey(task.getTaskKey());
            long trackedMinutes = timeSessionRepository.sumDurationByTaskKey(task.getTaskKey());
            boolean hasActivity = !commits.isEmpty() || !pullRequests.isEmpty();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("taskKey", task.getTaskKey());
            row.put("status", task.getStatus());
            row.put("trackedMinutes", trackedMinutes);
            row.put("hasEngineeringActivity", hasActivity);
            row.put("trackedState", trackedMinutes > 0 ? "TRACKED" : "UNTRACKED");
            return row;
        }).toList();
    }
}
