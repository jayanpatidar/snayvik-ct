package com.snayvik.kpi.kpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.snayvik.kpi.domain.TaskKeyExtractor;
import com.snayvik.kpi.ingress.audit.WebhookEvent;
import com.snayvik.kpi.ingress.audit.WebhookEventRepository;
import com.snayvik.kpi.ingress.persistence.BoardMappingRepository;
import com.snayvik.kpi.policy.PolicyEvaluationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecalculationDispatchService {

    private final WebhookEventRepository webhookEventRepository;
    private final TaskKeyExtractor taskKeyExtractor;
    private final BoardMappingRepository boardMappingRepository;
    private final KpiComputationService kpiComputationService;
    private final PolicyEvaluationService policyEvaluationService;

    public RecalculationDispatchService(
            WebhookEventRepository webhookEventRepository,
            TaskKeyExtractor taskKeyExtractor,
            BoardMappingRepository boardMappingRepository,
            KpiComputationService kpiComputationService,
            PolicyEvaluationService policyEvaluationService) {
        this.webhookEventRepository = webhookEventRepository;
        this.taskKeyExtractor = taskKeyExtractor;
        this.boardMappingRepository = boardMappingRepository;
        this.kpiComputationService = kpiComputationService;
        this.policyEvaluationService = policyEvaluationService;
    }

    @Transactional
    public void dispatchByEventId(Long eventId) {
        if (eventId == null) {
            return;
        }
        WebhookEvent event = webhookEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }

        Set<String> taskKeys = resolveTaskKeys(event);
        for (String taskKey : taskKeys) {
            kpiComputationService.recomputeTaskMetrics(taskKey);
            policyEvaluationService.evaluateTask(taskKey);
        }
        if (taskKeys.isEmpty()) {
            kpiComputationService.recomputeAllTaskMetrics();
        }
        policyEvaluationService.evaluateEvent(eventId);
    }

    private Set<String> resolveTaskKeys(WebhookEvent event) {
        JsonNode payload = event.getPayload();
        if ("MONDAY".equalsIgnoreCase(event.getSource())) {
            String boardId = payload.at("/event/boardId").asText("");
            if (boardId.isBlank()) {
                boardId = payload.at("/event/board_id").asText("");
            }
            String pulseId = payload.at("/event/pulseId").asText("");
            if (pulseId.isBlank()) {
                pulseId = payload.at("/event/pulse_id").asText("");
            }
            if (!boardId.isBlank() && !pulseId.isBlank()) {
                String finalBoardId = boardId;
                String finalPulseId = pulseId;
                return boardMappingRepository.findByBoardId(finalBoardId)
                        .map(mapping -> java.util.Set.of(mapping.getPrefix() + "-" + finalPulseId))
                        .orElse(java.util.Set.of());
            }
            return java.util.Set.of();
        }

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
        return taskKeyExtractor.extractTaskKeys(candidates);
    }

    private void addCandidate(List<String> candidates, String value) {
        if (value != null && !value.isBlank()) {
            candidates.add(value);
        }
    }
}
