package com.snayvik.kpi.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.snayvik.kpi.ingress.persistence.Task;
import com.snayvik.kpi.ingress.persistence.TaskRepository;
import com.snayvik.kpi.kpi.DailySnapshot;
import com.snayvik.kpi.kpi.DailySnapshotService;
import com.snayvik.kpi.kpi.TaskMetrics;
import com.snayvik.kpi.kpi.TaskMetricsRepository;
import com.snayvik.kpi.policy.PolicyViolation;
import com.snayvik.kpi.policy.PolicyViolationRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardOverviewServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMetricsRepository taskMetricsRepository;

    @Mock
    private DailySnapshotService dailySnapshotService;

    @Mock
    private PolicyViolationRepository policyViolationRepository;

    private DashboardOverviewService dashboardOverviewService;

    @BeforeEach
    void setUp() {
        dashboardOverviewService = new DashboardOverviewService(
                taskRepository,
                taskMetricsRepository,
                dailySnapshotService,
                policyViolationRepository);
    }

    @Test
    void buildsDetailedOverviewPayload() {
        when(taskRepository.findAll()).thenReturn(List.of(
                task("SNAY-1", "SNAY", "Done", 1.0),
                task("SNAY-2", "SNAY", "In Progress", 0.0),
                task("GOV-1", "GOV", "Done", 2.0)));
        when(taskMetricsRepository.findAll()).thenReturn(List.of(
                metric("SNAY-1", 7200L, 20.0, 0.15, 4, 1.0),
                metric("SNAY-2", 10800L, 67.5, 0.25, 8, 2.0),
                metric("GOV-1", 14400L, 42.0, 0.05, 3, 0.0)));
        when(dailySnapshotService.getSnapshotsForLastDays(14)).thenReturn(List.of(snapshot("2026-02-20", "SNAY", 7800L, 30.0)));
        when(policyViolationRepository.findByResolvedOrderByCreatedAtDesc(false)).thenReturn(List.of(
                violation("HIGH"),
                violation("WARN"),
                violation("WARN")));

        DashboardOverviewService.DashboardOverviewResponse response = dashboardOverviewService.buildOverview(14);

        assertThat(response.taskCount()).isEqualTo(3);
        assertThat(response.avgLeadTimeSeconds()).isEqualTo(10800L);
        assertThat(response.avgRiskScore()).isEqualTo(43.17);
        assertThat(response.avgReworkRate()).isEqualTo(0.15);
        assertThat(response.riskBands().green()).isEqualTo(1);
        assertThat(response.riskBands().yellow()).isEqualTo(1);
        assertThat(response.riskBands().red()).isEqualTo(1);
        assertThat(response.prefixSummary())
                .extracting(DashboardOverviewService.PrefixSummaryRow::prefix)
                .containsExactly("GOV", "SNAY");
        assertThat(response.topRiskTasks()).hasSize(3);
        assertThat(response.topRiskTasks().get(0).taskKey()).isEqualTo("SNAY-2");
        assertThat(response.statusBreakdown())
                .extracting(DashboardOverviewService.StatusSummaryRow::status)
                .containsExactly("Done", "In Progress");
        assertThat(response.openViolations().totalOpen()).isEqualTo(3);
        assertThat(response.openViolations().high()).isEqualTo(1);
        assertThat(response.openViolations().warn()).isEqualTo(2);
        assertThat(response.snapshots()).hasSize(1);
    }

    @Test
    void handlesEmptyDatasets() {
        when(taskRepository.findAll()).thenReturn(List.of());
        when(taskMetricsRepository.findAll()).thenReturn(List.of());
        when(dailySnapshotService.getSnapshotsForLastDays(7)).thenReturn(List.of());
        when(policyViolationRepository.findByResolvedOrderByCreatedAtDesc(false)).thenReturn(List.of());

        DashboardOverviewService.DashboardOverviewResponse response = dashboardOverviewService.buildOverview(7);

        assertThat(response.taskCount()).isZero();
        assertThat(response.avgLeadTimeSeconds()).isZero();
        assertThat(response.avgRiskScore()).isZero();
        assertThat(response.avgReworkRate()).isZero();
        assertThat(response.riskBands().unknown()).isZero();
        assertThat(response.statusBreakdown()).isEmpty();
        assertThat(response.prefixSummary()).isEmpty();
        assertThat(response.topRiskTasks()).isEmpty();
        assertThat(response.openViolations().totalOpen()).isZero();
    }

    private Task task(String taskKey, String prefix, String status, double driftScore) {
        Task task = new Task();
        task.setTaskKey(taskKey);
        task.setPrefix(prefix);
        task.setPulseId(taskKey + "-pulse");
        task.setBoardId(prefix + "-board");
        task.setStatus(status);
        task.setDriftScore(driftScore);
        return task;
    }

    private TaskMetrics metric(
            String taskKey,
            long leadTimeSeconds,
            double riskScore,
            double reworkRate,
            int commitCount,
            double driftScore) {
        TaskMetrics metrics = new TaskMetrics();
        metrics.setTaskKey(taskKey);
        metrics.setLeadTimeSeconds(leadTimeSeconds);
        metrics.setRiskScore(riskScore);
        metrics.setReworkRate(reworkRate);
        metrics.setCommitCount(commitCount);
        metrics.setDriftScore(driftScore);
        return metrics;
    }

    private PolicyViolation violation(String severity) {
        PolicyViolation violation = new PolicyViolation();
        violation.setSeverity(severity);
        violation.setType("TEST");
        violation.setMessage("test");
        return violation;
    }

    private DailySnapshot snapshot(String date, String prefix, long avgLeadTime, double avgRiskScore) {
        DailySnapshot snapshot = new DailySnapshot();
        snapshot.setSnapshotDate(LocalDate.parse(date));
        snapshot.setPrefix(prefix);
        snapshot.setAvgLeadTime(avgLeadTime);
        snapshot.setAvgRiskScore(avgRiskScore);
        snapshot.setDriftRate(0.2);
        snapshot.setReworkRate(0.1);
        return snapshot;
    }
}
