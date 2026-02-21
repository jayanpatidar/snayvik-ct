package com.snayvik.kpi.time;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snayvik.kpi.ingress.persistence.CommitActivity;
import com.snayvik.kpi.ingress.persistence.CommitActivityRepository;
import com.snayvik.kpi.ingress.persistence.PullRequestActivity;
import com.snayvik.kpi.ingress.persistence.PullRequestActivityRepository;
import com.snayvik.kpi.ingress.persistence.Task;
import com.snayvik.kpi.ingress.persistence.TaskRepository;
import com.snayvik.kpi.policy.PolicyViolationService;
import com.snayvik.kpi.policy.ViolationType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeGovernanceServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PullRequestActivityRepository pullRequestActivityRepository;

    @Mock
    private CommitActivityRepository commitActivityRepository;

    @Mock
    private TimeSessionRepository timeSessionRepository;

    @Mock
    private PolicyViolationService policyViolationService;

    private TimeGovernanceService timeGovernanceService;

    @BeforeEach
    void setUp() {
        timeGovernanceService = new TimeGovernanceService(
                taskRepository,
                pullRequestActivityRepository,
                commitActivityRepository,
                timeSessionRepository,
                policyViolationService);
    }

    @Test
    void flagsUntrackedWorkWhenActivityExistsWithoutTime() {
        Task task = new Task();
        task.setTaskKey("AB-1");
        when(taskRepository.findById("AB-1")).thenReturn(Optional.of(task));
        when(commitActivityRepository.findAllByTaskKey("AB-1")).thenReturn(List.of(new CommitActivity()));
        when(pullRequestActivityRepository.findAllByTaskKey("AB-1")).thenReturn(List.of());
        when(timeSessionRepository.sumDurationByTaskKey("AB-1")).thenReturn(0L);

        timeGovernanceService.evaluateTask("AB-1");

        verify(policyViolationService).createViolation(
                "AB-1",
                null,
                ViolationType.UNTRACKED_WORK,
                "MEDIUM",
                "Engineering activity detected with no tracked time.");
    }

    @Test
    void flagsNoTimeOnCompletedTask() {
        Task task = new Task();
        task.setTaskKey("AB-2");
        task.setStatus("Done");
        PullRequestActivity pr = new PullRequestActivity();
        pr.setMergedAt(java.time.Instant.now());

        when(taskRepository.findById("AB-2")).thenReturn(Optional.of(task));
        when(commitActivityRepository.findAllByTaskKey("AB-2")).thenReturn(List.of());
        when(pullRequestActivityRepository.findAllByTaskKey("AB-2")).thenReturn(List.of(pr));
        when(timeSessionRepository.sumDurationByTaskKey("AB-2")).thenReturn(0L);

        timeGovernanceService.evaluateTask("AB-2");

        verify(policyViolationService).createViolation(
                "AB-2",
                null,
                ViolationType.NO_TIME_ON_COMPLETED_TASK,
                "HIGH",
                "Completed task has merged PR but no tracked time.");
    }
}
