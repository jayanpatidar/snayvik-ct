package com.snayvik.kpi.ingress.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snayvik.kpi.domain.TaskKeyExtractor;
import com.snayvik.kpi.sync.GitHubCommitSnapshot;
import com.snayvik.kpi.sync.GitHubPullRequestSnapshot;
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
class GitHubActivityPersistenceServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BoardMappingRepository boardMappingRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PullRequestActivityRepository pullRequestActivityRepository;

    @Mock
    private CommitActivityRepository commitActivityRepository;

    private GitHubActivityPersistenceService gitHubActivityPersistenceService;

    @BeforeEach
    void setUp() {
        gitHubActivityPersistenceService = new GitHubActivityPersistenceService(
                new TaskKeyExtractor("([A-Z]{2,10}-[a-zA-Z0-9]+)"),
                boardMappingRepository,
                taskRepository,
                pullRequestActivityRepository,
                commitActivityRepository);
    }

    @Test
    void linksPrAndCommitToTaskWhenSingleTaskKeyAndBoardMappingExists() throws Exception {
        String payload = """
                {
                  "action": "opened",
                  "number": 14,
                  "repository": {"full_name": "snayvik/repo"},
                  "pull_request": {
                    "title": "feat AB-123 improve metrics",
                    "head": {"ref": "feature/AB-123-branch"},
                    "created_at": "2026-02-20T12:00:00Z",
                    "merged_at": null
                  },
                  "commits": [
                    {"id": "c1", "message": "AB-123 initial", "author": {"name": "Jay"}, "timestamp": "2026-02-20T12:10:00Z"}
                  ]
                }
                """;

        BoardMapping boardMapping = org.mockito.Mockito.mock(BoardMapping.class);
        when(boardMapping.getBoardId()).thenReturn("98765");
        when(boardMappingRepository.findById("AB")).thenReturn(Optional.of(boardMapping));
        when(taskRepository.existsById("AB-123")).thenReturn(false, true);
        when(pullRequestActivityRepository.findByRepositoryAndPrNumber("snayvik/repo", 14)).thenReturn(Optional.empty());
        when(commitActivityRepository.findByRepositoryAndCommitHash("snayvik/repo", "c1")).thenReturn(Optional.empty());

        gitHubActivityPersistenceService.persistFromWebhook(objectMapper.readTree(payload));

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskKey()).isEqualTo("AB-123");
        assertThat(taskCaptor.getValue().getBoardId()).isEqualTo("98765");

        ArgumentCaptor<PullRequestActivity> prCaptor = ArgumentCaptor.forClass(PullRequestActivity.class);
        verify(pullRequestActivityRepository).save(prCaptor.capture());
        assertThat(prCaptor.getValue().getTaskKey()).isEqualTo("AB-123");
        assertThat(prCaptor.getValue().getRepository()).isEqualTo("snayvik/repo");

        ArgumentCaptor<CommitActivity> commitCaptor = ArgumentCaptor.forClass(CommitActivity.class);
        verify(commitActivityRepository).save(commitCaptor.capture());
        assertThat(commitCaptor.getValue().getTaskKey()).isEqualTo("AB-123");
        assertThat(commitCaptor.getValue().getCommitHash()).isEqualTo("c1");
    }

    @Test
    void keepsTaskLinkNullWhenMultipleTaskKeysDetected() throws Exception {
        String payload = """
                {
                  "number": 15,
                  "repository": {"full_name": "snayvik/repo"},
                  "pull_request": {
                    "title": "mix AB-123 and CD-456",
                    "head": {"ref": "feature/mix"}
                  }
                }
                """;

        when(pullRequestActivityRepository.findByRepositoryAndPrNumber("snayvik/repo", 15)).thenReturn(Optional.empty());

        gitHubActivityPersistenceService.persistFromWebhook(objectMapper.readTree(payload));

        verify(taskRepository, never()).save(org.mockito.ArgumentMatchers.any());
        ArgumentCaptor<PullRequestActivity> prCaptor = ArgumentCaptor.forClass(PullRequestActivity.class);
        verify(pullRequestActivityRepository).save(prCaptor.capture());
        assertThat(prCaptor.getValue().getTaskKey()).isNull();
    }

    @Test
    void upsertsFromSnapshot() {
        BoardMapping boardMapping = org.mockito.Mockito.mock(BoardMapping.class);
        when(boardMapping.getBoardId()).thenReturn("98765");
        when(boardMappingRepository.findById("AB")).thenReturn(Optional.of(boardMapping));
        when(taskRepository.existsById("AB-123")).thenReturn(false, true);
        when(pullRequestActivityRepository.findByRepositoryAndPrNumber("snayvik/repo", 21)).thenReturn(Optional.empty());
        when(commitActivityRepository.findByRepositoryAndCommitHash("snayvik/repo", "c123")).thenReturn(Optional.empty());

        GitHubPullRequestSnapshot snapshot = new GitHubPullRequestSnapshot(
                "snayvik/repo",
                21,
                "feat AB-123 add phase1 sync",
                "feature/AB-123-sync",
                Instant.parse("2026-02-19T10:00:00Z"),
                Instant.parse("2026-02-19T14:00:00Z"),
                1,
                List.of(new GitHubCommitSnapshot(
                        "c123",
                        "jay",
                        "AB-123 sync commit",
                        Instant.parse("2026-02-19T11:00:00Z"))));

        String taskKey = gitHubActivityPersistenceService.persistFromSnapshot(snapshot);

        assertThat(taskKey).isEqualTo("AB-123");
        ArgumentCaptor<PullRequestActivity> prCaptor = ArgumentCaptor.forClass(PullRequestActivity.class);
        verify(pullRequestActivityRepository).save(prCaptor.capture());
        assertThat(prCaptor.getValue().getTaskKey()).isEqualTo("AB-123");
        assertThat(prCaptor.getValue().getReopenCount()).isEqualTo(1);

        ArgumentCaptor<CommitActivity> commitCaptor = ArgumentCaptor.forClass(CommitActivity.class);
        verify(commitActivityRepository).save(commitCaptor.capture());
        assertThat(commitCaptor.getValue().getTaskKey()).isEqualTo("AB-123");
        assertThat(commitCaptor.getValue().getCommitHash()).isEqualTo("c123");
    }
}
