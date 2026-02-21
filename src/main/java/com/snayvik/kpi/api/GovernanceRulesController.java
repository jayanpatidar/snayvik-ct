package com.snayvik.kpi.api;

import com.snayvik.kpi.threshold.ThresholdPolicy;
import com.snayvik.kpi.threshold.ThresholdPolicyRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/governance-rules")
public class GovernanceRulesController {

    private final ThresholdPolicyRepository thresholdPolicyRepository;

    public GovernanceRulesController(ThresholdPolicyRepository thresholdPolicyRepository) {
        this.thresholdPolicyRepository = thresholdPolicyRepository;
    }

    @GetMapping
    public Map<String, Object> rules() {
        List<ThresholdPolicy> thresholds = thresholdPolicyRepository.findAll();
        Instant lastUpdated = thresholds.stream()
                .map(ThresholdPolicy::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(Instant.now());
        return Map.of(
                "lastUpdatedAt", lastUpdated.toString(),
                "rules", List.of(
                        Map.of("code", "MISSING_TASK_KEY", "description", "PR/branch/commit must include one task_key."),
                        Map.of("code", "MULTIPLE_TASK_KEYS", "description", "Single PR must map to one task_key."),
                        Map.of("code", "DONE_WITHOUT_MERGE", "description", "Done status requires at least one merged PR."),
                        Map.of("code", "DRIFT_AFTER_START", "description", "Description changes after start are drift violations."),
                        Map.of("code", "UNTRACKED_WORK", "description", "GitHub activity exists with no tracked time."),
                        Map.of("code", "NO_TIME_ON_COMPLETED_TASK", "description", "Completed task with merged PR has zero tracked time."),
                        Map.of("code", "UNJUSTIFIED_TIME", "description", "Tracked time exists without meaningful activity.")),
                "thresholds", thresholds.stream().map(this::thresholdDto).toList());
    }

    private Map<String, Object> thresholdDto(ThresholdPolicy thresholdPolicy) {
        return Map.of(
                "id", thresholdPolicy.getId(),
                "violationType", thresholdPolicy.getViolationType(),
                "thresholdCount", thresholdPolicy.getThresholdCount(),
                "timeWindowDays", thresholdPolicy.getTimeWindowDays(),
                "escalationLevel", thresholdPolicy.getEscalationLevel(),
                "notifyEmail", thresholdPolicy.getNotifyEmail() == null ? "" : thresholdPolicy.getNotifyEmail(),
                "notifySlack", thresholdPolicy.getNotifySlack() == null ? "" : thresholdPolicy.getNotifySlack(),
                "active", thresholdPolicy.isActive(),
                "updatedAt", thresholdPolicy.getUpdatedAt() == null ? "" : thresholdPolicy.getUpdatedAt().toString());
    }
}
