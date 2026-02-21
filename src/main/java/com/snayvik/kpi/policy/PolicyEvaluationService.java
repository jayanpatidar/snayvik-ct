package com.snayvik.kpi.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.snayvik.kpi.domain.TaskKeyExtractor;
import com.snayvik.kpi.ingress.audit.WebhookEvent;
import com.snayvik.kpi.ingress.audit.WebhookEventRepository;
import com.snayvik.kpi.ingress.persistence.BoardMappingRepository;
import com.snayvik.kpi.ingress.persistence.PullRequestActivityRepository;
import com.snayvik.kpi.ingress.persistence.Task;
import com.snayvik.kpi.ingress.persistence.TaskRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyEvaluationService {

    private final TaskRepository taskRepository;
    private final PullRequestActivityRepository pullRequestActivityRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final TaskKeyExtractor taskKeyExtractor;
    private final BoardMappingRepository boardMappingRepository;
    private final PolicyViolationService policyViolationService;

    public PolicyEvaluationService(
            TaskRepository taskRepository,
            PullRequestActivityRepository pullRequestActivityRepository,
            WebhookEventRepository webhookEventRepository,
            TaskKeyExtractor taskKeyExtractor,
            BoardMappingRepository boardMappingRepository,
            PolicyViolationService policyViolationService) {
        this.taskRepository = taskRepository;
        this.pullRequestActivityRepository = pullRequestActivityRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.taskKeyExtractor = taskKeyExtractor;
        this.boardMappingRepository = boardMappingRepository;
        this.policyViolationService = policyViolationService;
    }

    @Transactional
    public void evaluateTask(String taskKey) {
        Task task = taskRepository.findById(taskKey).orElse(null);
        if (task == null) {
            return;
        }
        boolean isDone = task.getStatus() != null && task.getStatus().equalsIgnoreCase("done");
        boolean hasMergedPr = pullRequestActivityRepository.findAllByTaskKey(taskKey).stream()
                .anyMatch(pr -> pr.getMergedAt() != null);
        if (isDone && !hasMergedPr) {
            policyViolationService.createViolation(
                    taskKey,
                    null,
                    ViolationType.DONE_WITHOUT_MERGE,
                    "HIGH",
                    "Task is marked Done without a merged pull request.");
        }
    }

    @Transactional
    public void evaluateEvent(Long eventId) {
        WebhookEvent event = webhookEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }
        if ("GITHUB".equalsIgnoreCase(event.getSource())) {
            evaluateGithubEvent(event);
            return;
        }
        if ("MONDAY".equalsIgnoreCase(event.getSource())) {
            evaluateMondayEvent(event);
        }
    }

    private void evaluateGithubEvent(WebhookEvent event) {
        JsonNode payload = event.getPayload();
        Set<String> taskKeys = taskKeyExtractor.extractTaskKeys(githubCandidates(payload));
        String actor = payload.at("/sender/login").asText(null);
        if (taskKeys.isEmpty()) {
            policyViolationService.createViolation(
                    null,
                    actor,
                    ViolationType.MISSING_TASK_KEY,
                    "MEDIUM",
                    "Missing task_key in PR title, branch name, or commit messages.");
            return;
        }
        if (taskKeys.size() > 1) {
            policyViolationService.createViolation(
                    null,
                    actor,
                    ViolationType.MULTIPLE_TASK_KEYS,
                    "MEDIUM",
                    "Multiple task_keys detected in one GitHub event.");
        }
    }

    private void evaluateMondayEvent(WebhookEvent event) {
        JsonNode payload = event.getPayload();
        boolean descriptionChange = isDescriptionChange(payload);
        if (!descriptionChange) {
            return;
        }
        String taskKey = resolveMondayTaskKey(payload);
        if (taskKey == null) {
            return;
        }
        taskRepository.findById(taskKey).ifPresent(task -> {
            if (task.getFirstCommitAt() != null) {
                double driftScore = task.getDriftScore() == null ? 0.0 : task.getDriftScore();
                task.setDriftScore(driftScore + 10.0);
                taskRepository.save(task);
                policyViolationService.createViolation(
                        taskKey,
                        null,
                        ViolationType.DRIFT_AFTER_START,
                        "MEDIUM",
                        "Task description changed after work started.");
            }
        });
    }

    private List<String> githubCandidates(JsonNode payload) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, payload.at("/pull_request/title").asText(null));
        addCandidate(candidates, payload.at("/pull_request/head/ref").asText(null));
        addCandidate(candidates, payload.at("/ref").asText(null));
        JsonNode commits = payload.get("commits");
        if (commits != null && commits.isArray()) {
            for (JsonNode commit : commits) {
                addCandidate(candidates, commit.path("message").asText(null));
            }
        }
        return candidates;
    }

    private void addCandidate(List<String> candidates, String value) {
        if (value != null && !value.isBlank()) {
            candidates.add(value);
        }
    }

    private boolean isDescriptionChange(JsonNode payload) {
        String type = payload.at("/event/type").asText("");
        String columnTitle = payload.at("/event/columnTitle").asText("");
        String columnId = payload.at("/event/columnId").asText("");
        return type.equalsIgnoreCase("change_column_value")
                && (columnTitle.equalsIgnoreCase("description")
                || columnId.equalsIgnoreCase("description")
                || columnId.equalsIgnoreCase("long_text"));
    }

    private String resolveMondayTaskKey(JsonNode payload) {
        String boardId = payload.at("/event/boardId").asText("");
        if (boardId.isBlank()) {
            boardId = payload.at("/event/board_id").asText("");
        }
        String pulseId = payload.at("/event/pulseId").asText("");
        if (pulseId.isBlank()) {
            pulseId = payload.at("/event/pulse_id").asText("");
        }
        if (boardId.isBlank() || pulseId.isBlank()) {
            return null;
        }
        String finalBoardId = boardId;
        String finalPulseId = pulseId;
        return boardMappingRepository.findByBoardId(finalBoardId)
                .map(mapping -> mapping.getPrefix() + "-" + finalPulseId)
                .orElse(null);
    }
}
