package com.snayvik.kpi.sync;

import com.snayvik.kpi.ingress.persistence.BoardMapping;
import com.snayvik.kpi.ingress.persistence.BoardMappingRepository;
import com.snayvik.kpi.ingress.persistence.GitHubActivityPersistenceService;
import com.snayvik.kpi.ingress.persistence.MondayTaskPersistenceService;
import com.snayvik.kpi.kpi.KpiComputationService;
import com.snayvik.kpi.policy.PolicyEvaluationService;
import com.snayvik.kpi.time.TimeGovernanceService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitialFullSyncService {

    private final SyncProperties syncProperties;
    private final BoardMappingRepository boardMappingRepository;
    private final MondaySyncClient mondaySyncClient;
    private final GitHubSyncClient gitHubSyncClient;
    private final MondayTaskPersistenceService mondayTaskPersistenceService;
    private final GitHubActivityPersistenceService gitHubActivityPersistenceService;
    private final KpiComputationService kpiComputationService;
    private final PolicyEvaluationService policyEvaluationService;
    private final TimeGovernanceService timeGovernanceService;
    private final Clock clock;

    public InitialFullSyncService(
            SyncProperties syncProperties,
            BoardMappingRepository boardMappingRepository,
            MondaySyncClient mondaySyncClient,
            GitHubSyncClient gitHubSyncClient,
            MondayTaskPersistenceService mondayTaskPersistenceService,
            GitHubActivityPersistenceService gitHubActivityPersistenceService,
            KpiComputationService kpiComputationService,
            PolicyEvaluationService policyEvaluationService,
            TimeGovernanceService timeGovernanceService,
            Clock clock) {
        this.syncProperties = syncProperties;
        this.boardMappingRepository = boardMappingRepository;
        this.mondaySyncClient = mondaySyncClient;
        this.gitHubSyncClient = gitHubSyncClient;
        this.mondayTaskPersistenceService = mondayTaskPersistenceService;
        this.gitHubActivityPersistenceService = gitHubActivityPersistenceService;
        this.kpiComputationService = kpiComputationService;
        this.policyEvaluationService = policyEvaluationService;
        this.timeGovernanceService = timeGovernanceService;
        this.clock = clock;
    }

    @Transactional
    public SyncRunReport runInitialFullSync() {
        return runSync(SyncRunType.INITIAL_FULL_SYNC);
    }

    @Transactional
    public SyncRunReport runReconciliation() {
        return runSync(SyncRunType.RECONCILIATION);
    }

    private SyncRunReport runSync(SyncRunType runType) {
        Instant startedAt = Instant.now(clock);
        if (!syncProperties.isEnabled()) {
            return SyncRunReport.disabled(runType, startedAt);
        }

        int boardsScanned = 0;
        int mondayItemsProcessed = 0;
        int githubRepositoriesScanned = 0;
        int githubPullRequestsProcessed = 0;
        int githubCommitsProcessed = 0;
        Set<String> touchedTasks = new LinkedHashSet<>();

        List<BoardMapping> mappings = boardMappingRepository.findAll();
        for (BoardMapping mapping : mappings) {
            boardsScanned++;
            List<MondayTaskSnapshot> items = mondaySyncClient.fetchBoardItems(mapping.getBoardId());
            if (items == null) {
                continue;
            }
            for (MondayTaskSnapshot item : items) {
                mondayItemsProcessed++;
                String taskKey = mondayTaskPersistenceService.upsertFromSnapshot(mapping, item);
                if (taskKey != null && !taskKey.isBlank()) {
                    touchedTasks.add(taskKey);
                }
            }
        }

        List<String> repositories = normalizeRepositories(syncProperties.getGithubRepositories());
        Instant since = Instant.now(clock).minus(Duration.ofDays(Math.max(1, syncProperties.getGithubLookbackDays())));
        for (String repository : repositories) {
            githubRepositoriesScanned++;
            List<GitHubPullRequestSnapshot> pullRequests = gitHubSyncClient.fetchPullRequests(repository, since);
            if (pullRequests == null) {
                continue;
            }
            for (GitHubPullRequestSnapshot pullRequest : pullRequests) {
                githubPullRequestsProcessed++;
                githubCommitsProcessed += pullRequest.commits().size();
                String taskKey = gitHubActivityPersistenceService.persistFromSnapshot(pullRequest);
                if (taskKey != null && !taskKey.isBlank()) {
                    touchedTasks.add(taskKey);
                }
            }
        }

        int tasksRecomputed = 0;
        for (String taskKey : touchedTasks) {
            kpiComputationService.recomputeTaskMetrics(taskKey);
            policyEvaluationService.evaluateTask(taskKey);
            timeGovernanceService.evaluateTask(taskKey);
            tasksRecomputed++;
        }

        Instant finishedAt = Instant.now(clock);
        return new SyncRunReport(
                runType,
                true,
                startedAt,
                finishedAt,
                boardsScanned,
                mondayItemsProcessed,
                githubRepositoriesScanned,
                githubPullRequestsProcessed,
                githubCommitsProcessed,
                touchedTasks.size(),
                tasksRecomputed);
    }

    private List<String> normalizeRepositories(List<String> repositories) {
        if (repositories == null) {
            return List.of();
        }
        return repositories.stream()
                .filter(repo -> repo != null && !repo.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
