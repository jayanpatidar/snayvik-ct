package com.snayvik.kpi.policy;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snayvik.kpi.domain.TaskKeyExtractor;
import com.snayvik.kpi.ingress.audit.WebhookEvent;
import com.snayvik.kpi.ingress.audit.WebhookEventRepository;
import com.snayvik.kpi.ingress.persistence.BoardMapping;
import com.snayvik.kpi.ingress.persistence.BoardMappingRepository;
import com.snayvik.kpi.ingress.persistence.PullRequestActivity;
import com.snayvik.kpi.ingress.persistence.PullRequestActivityRepository;
import com.snayvik.kpi.ingress.persistence.Task;
import com.snayvik.kpi.ingress.persistence.TaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyEvaluationServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PullRequestActivityRepository pullRequestActivityRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private BoardMappingRepository boardMappingRepository;

    @Mock
    private PolicyViolationService policyViolationService;

    private PolicyEvaluationService policyEvaluationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        policyEvaluationService = new PolicyEvaluationService(
                taskRepository,
                pullRequestActivityRepository,
                webhookEventRepository,
                new TaskKeyExtractor("([A-Z]{2,10}-[a-zA-Z0-9]+)"),
                boardMappingRepository,
                policyViolationService);
    }

    @Test
    void createsDoneWithoutMergeViolation() {
        Task task = new Task();
        task.setTaskKey("AB-1");
        task.setStatus("Done");
        when(taskRepository.findById("AB-1")).thenReturn(Optional.of(task));
        when(pullRequestActivityRepository.findAllByTaskKey("AB-1")).thenReturn(List.of(new PullRequestActivity()));

        policyEvaluationService.evaluateTask("AB-1");

        verify(policyViolationService).createViolation(
                "AB-1",
                null,
                ViolationType.DONE_WITHOUT_MERGE,
                "HIGH",
                "Task is marked Done without a merged pull request.");
    }

    @Test
    void createsMissingTaskKeyViolationFromGithubEvent() throws Exception {
        WebhookEvent event = new WebhookEvent();
        event.setSource("GITHUB");
        event.setPayload(objectMapper.readTree("{\"pull_request\":{\"title\":\"missing\"},\"sender\":{\"login\":\"dev1\"}}"));
        when(webhookEventRepository.findById(11L)).thenReturn(Optional.of(event));

        policyEvaluationService.evaluateEvent(11L);

        verify(policyViolationService).createViolation(
                null,
                "dev1",
                ViolationType.MISSING_TASK_KEY,
                "MEDIUM",
                "Missing task_key in PR title, branch name, or commit messages.");
    }

    @Test
    void createsDriftViolationWhenDescriptionChangesAfterFirstCommit() throws Exception {
        WebhookEvent event = new WebhookEvent();
        event.setSource("MONDAY");
        event.setPayload(objectMapper.readTree("""
                {"event":{"type":"change_column_value","columnId":"description","boardId":"100","pulseId":"22"}}
                """));
        when(webhookEventRepository.findById(12L)).thenReturn(Optional.of(event));

        BoardMapping mapping = org.mockito.Mockito.mock(BoardMapping.class);
        when(mapping.getPrefix()).thenReturn("AB");
        when(boardMappingRepository.findByBoardId("100")).thenReturn(Optional.of(mapping));

        Task task = new Task();
        task.setTaskKey("AB-22");
        task.setFirstCommitAt(Instant.now().minusSeconds(300));
        task.setDriftScore(5.0);
        when(taskRepository.findById("AB-22")).thenReturn(Optional.of(task));

        policyEvaluationService.evaluateEvent(12L);

        verify(taskRepository).save(task);
        verify(policyViolationService).createViolation(
                "AB-22",
                null,
                ViolationType.DRIFT_AFTER_START,
                "MEDIUM",
                "Task description changed after work started.");
    }
}
