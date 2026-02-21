package com.snayvik.kpi.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snayvik.kpi.ingress.persistence.CommitActivity;
import com.snayvik.kpi.ingress.persistence.CommitActivityRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KpiComputationServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PullRequestActivityRepository pullRequestActivityRepository;

    @Mock
    private CommitActivityRepository commitActivityRepository;

    @Mock
    private TaskMetricsRepository taskMetricsRepository;

    private KpiComputationService kpiComputationService;

    @BeforeEach
    void setUp() {
        kpiComputationService = new KpiComputationService(
                taskRepository, pullRequestActivityRepository, commitActivityRepository, taskMetricsRepository);
    }

    @Test
    void computesLeadCycleReworkAndRisk() {
        Task task = new Task();
        task.setTaskKey("AB-123");
        task.setStatus("Done");
        task.setStartedAt(Instant.parse("2026-02-20T10:00:00Z"));
        task.setCompletedAt(Instant.parse("2026-02-20T13:00:00Z"));
        task.setDriftScore(25.0);

        PullRequestActivity pullRequest = new PullRequestActivity();
        pullRequest.setMergedAt(Instant.parse("2026-02-20T12:00:00Z"));
        pullRequest.setReopenCount(1);

        CommitActivity commit1 = new CommitActivity();
        commit1.setCommitHash("c1");
        CommitActivity commit2 = new CommitActivity();
        commit2.setCommitHash("c2");

        when(taskRepository.findById("AB-123")).thenReturn(Optional.of(task));
        when(pullRequestActivityRepository.findAllByTaskKey("AB-123")).thenReturn(List.of(pullRequest));
        when(commitActivityRepository.findAllByTaskKey("AB-123")).thenReturn(List.of(commit1, commit2));
        when(taskMetricsRepository.findById("AB-123")).thenReturn(Optional.empty());

        kpiComputationService.recomputeTaskMetrics("AB-123");

        ArgumentCaptor<TaskMetrics> captor = ArgumentCaptor.forClass(TaskMetrics.class);
        verify(taskMetricsRepository).save(captor.capture());
        TaskMetrics metrics = captor.getValue();
        assertThat(metrics.getLeadTimeSeconds()).isEqualTo(7200L);
        assertThat(metrics.getCycleTimeSeconds()).isEqualTo(10800L);
        assertThat(metrics.getCommitCount()).isEqualTo(2);
        assertThat(metrics.getReworkRate()).isEqualTo(1.0);
        assertThat(metrics.getRiskScore()).isGreaterThan(45.0);
    }

    @Test
    void setsIntegrityToZeroWhenDoneWithoutMergedPr() {
        Task task = new Task();
        task.setTaskKey("AB-999");
        task.setStatus("Done");
        task.setStartedAt(Instant.parse("2026-02-20T10:00:00Z"));

        PullRequestActivity pullRequest = new PullRequestActivity();
        pullRequest.setMergedAt(null);
        pullRequest.setReopenCount(0);

        when(taskRepository.findById("AB-999")).thenReturn(Optional.of(task));
        when(pullRequestActivityRepository.findAllByTaskKey("AB-999")).thenReturn(List.of(pullRequest));
        when(commitActivityRepository.findAllByTaskKey("AB-999")).thenReturn(List.of());
        when(taskMetricsRepository.findById("AB-999")).thenReturn(Optional.empty());

        kpiComputationService.recomputeTaskMetrics("AB-999");

        ArgumentCaptor<TaskMetrics> captor = ArgumentCaptor.forClass(TaskMetrics.class);
        verify(taskMetricsRepository).save(captor.capture());
        assertThat(captor.getValue().getIntegrityScore()).isEqualTo(0.0);
    }
}
