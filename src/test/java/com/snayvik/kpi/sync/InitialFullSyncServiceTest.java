package com.snayvik.kpi.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snayvik.kpi.integration.RepoMappingRepository;
import com.snayvik.kpi.ingress.persistence.BoardMapping;
import com.snayvik.kpi.ingress.persistence.BoardMappingRepository;
import com.snayvik.kpi.ingress.persistence.GitHubActivityPersistenceService;
import com.snayvik.kpi.ingress.persistence.MondayTaskPersistenceService;
import com.snayvik.kpi.kpi.KpiComputationService;
import com.snayvik.kpi.policy.PolicyEvaluationService;
import com.snayvik.kpi.time.TimeGovernanceService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitialFullSyncServiceTest {

    @Mock
    private BoardMappingRepository boardMappingRepository;

    @Mock
    private RepoMappingRepository repoMappingRepository;

    @Mock
    private MondaySyncClient mondaySyncClient;

    @Mock
    private GitHubSyncClient gitHubSyncClient;

    @Mock
    private MondayTaskPersistenceService mondayTaskPersistenceService;

    @Mock
    private GitHubActivityPersistenceService gitHubActivityPersistenceService;

    @Mock
    private KpiComputationService kpiComputationService;

    @Mock
    private PolicyEvaluationService policyEvaluationService;

    @Mock
    private TimeGovernanceService timeGovernanceService;

    private SyncProperties syncProperties;
    private Clock clock;
    private InitialFullSyncService initialFullSyncService;

    @BeforeEach
    void setUp() {
        syncProperties = new SyncProperties();
        clock = Clock.fixed(Instant.parse("2026-02-21T00:00:00Z"), ZoneOffset.UTC);
        initialFullSyncService = new InitialFullSyncService(
                syncProperties,
                boardMappingRepository,
                repoMappingRepository,
                mondaySyncClient,
                gitHubSyncClient,
                mondayTaskPersistenceService,
                gitHubActivityPersistenceService,
                kpiComputationService,
                policyEvaluationService,
                timeGovernanceService,
                clock);
    }

    @Test
    void returnsDisabledReportWhenSyncDisabled() {
        syncProperties.setEnabled(false);

        SyncRunReport report = initialFullSyncService.runInitialFullSync();

        assertThat(report.enabled()).isFalse();
        assertThat(report.runType()).isEqualTo(SyncRunType.INITIAL_FULL_SYNC);
        verify(boardMappingRepository, never()).findAll();
        verify(repoMappingRepository, never()).findAllByOrderByRepositoryAsc();
    }

    @Test
    void runsFullSyncAndRecomputesTouchedTasks() {
        syncProperties.setEnabled(true);
        syncProperties.setGithubLookbackDays(90);
        syncProperties.setGithubRepositories(List.of("snayvik/repo"));

        BoardMapping mapping = mock(BoardMapping.class);
        when(mapping.getBoardId()).thenReturn("9988");
        when(boardMappingRepository.findAll()).thenReturn(List.of(mapping));
        when(repoMappingRepository.findAllByOrderByRepositoryAsc()).thenReturn(List.of());

        MondayTaskSnapshot mondayTaskSnapshot = new MondayTaskSnapshot(
                "123",
                "Done",
                Instant.parse("2026-02-20T09:00:00Z"),
                Instant.parse("2026-02-20T17:00:00Z"),
                Instant.parse("2026-02-20T16:30:00Z"));
        when(mondaySyncClient.fetchBoardItems("9988")).thenReturn(List.of(mondayTaskSnapshot));
        when(mondayTaskPersistenceService.upsertFromSnapshot(mapping, mondayTaskSnapshot)).thenReturn("AB-123");

        GitHubPullRequestSnapshot pullRequestSnapshot = new GitHubPullRequestSnapshot(
                "snayvik/repo",
                44,
                "feat AB-123 sync",
                "feature/AB-123-sync",
                Instant.parse("2026-02-20T10:00:00Z"),
                Instant.parse("2026-02-20T15:00:00Z"),
                0,
                List.of(new GitHubCommitSnapshot(
                        "hash1",
                        "jay",
                        "AB-123 commit",
                        Instant.parse("2026-02-20T11:00:00Z"))));
        when(gitHubSyncClient.fetchPullRequests(eq("snayvik/repo"), any())).thenReturn(List.of(pullRequestSnapshot));
        when(gitHubActivityPersistenceService.persistFromSnapshot(pullRequestSnapshot)).thenReturn("AB-123");

        SyncRunReport report = initialFullSyncService.runInitialFullSync();

        assertThat(report.enabled()).isTrue();
        assertThat(report.boardsScanned()).isEqualTo(1);
        assertThat(report.mondayItemsProcessed()).isEqualTo(1);
        assertThat(report.githubRepositoriesScanned()).isEqualTo(1);
        assertThat(report.githubPullRequestsProcessed()).isEqualTo(1);
        assertThat(report.githubCommitsProcessed()).isEqualTo(1);
        assertThat(report.touchedTasks()).isEqualTo(1);
        assertThat(report.tasksRecomputed()).isEqualTo(1);

        verify(kpiComputationService).recomputeTaskMetrics("AB-123");
        verify(policyEvaluationService).evaluateTask("AB-123");
        verify(timeGovernanceService).evaluateTask("AB-123");
    }
}
