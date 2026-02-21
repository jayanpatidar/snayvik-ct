package com.snayvik.kpi.ingress.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.snayvik.kpi.domain.TaskKeyExtractor;
import com.snayvik.kpi.sync.GitHubCommitSnapshot;
import com.snayvik.kpi.sync.GitHubPullRequestSnapshot;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitHubActivityPersistenceService {

    private final TaskKeyExtractor taskKeyExtractor;
    private final BoardMappingRepository boardMappingRepository;
    private final TaskRepository taskRepository;
    private final PullRequestActivityRepository pullRequestActivityRepository;
    private final CommitActivityRepository commitActivityRepository;

    public GitHubActivityPersistenceService(
            TaskKeyExtractor taskKeyExtractor,
            BoardMappingRepository boardMappingRepository,
            TaskRepository taskRepository,
            PullRequestActivityRepository pullRequestActivityRepository,
            CommitActivityRepository commitActivityRepository) {
        this.taskKeyExtractor = taskKeyExtractor;
        this.boardMappingRepository = boardMappingRepository;
        this.taskRepository = taskRepository;
        this.pullRequestActivityRepository = pullRequestActivityRepository;
        this.commitActivityRepository = commitActivityRepository;
    }

    @Transactional
    public void persistFromWebhook(JsonNode payload) {
        String repository = resolveRepository(payload);
        if (repository.isBlank()) {
            return;
        }

        Set<String> taskKeys = taskKeyExtractor.extractTaskKeys(collectTaskKeyCandidates(payload));
        String linkableTaskKey = resolveLinkableTaskKey(taskKeys);

        persistPullRequest(payload, repository, linkableTaskKey);
        persistCommits(payload, repository, linkableTaskKey);
    }

    @Transactional
    public String persistFromSnapshot(GitHubPullRequestSnapshot snapshot) {
        if (snapshot == null || snapshot.repository() == null || snapshot.repository().isBlank()) {
            return null;
        }
        if (snapshot.prNumber() <= 0) {
            return null;
        }

        Set<String> taskKeys = taskKeyExtractor.extractTaskKeys(snapshotCandidates(snapshot));
        String linkableTaskKey = resolveLinkableTaskKey(taskKeys);

        PullRequestActivity pullRequestActivity = pullRequestActivityRepository
                .findByRepositoryAndPrNumber(snapshot.repository(), snapshot.prNumber())
                .orElseGet(PullRequestActivity::new);
        pullRequestActivity.setRepository(snapshot.repository());
        pullRequestActivity.setPrNumber(snapshot.prNumber());
        pullRequestActivity.setTaskKey(linkableTaskKey);
        pullRequestActivity.setOpenedAt(snapshot.openedAt());
        pullRequestActivity.setMergedAt(snapshot.mergedAt());
        pullRequestActivity.setReopenCount(Math.max(snapshot.reopenCount(), 0));
        pullRequestActivityRepository.save(pullRequestActivity);
        updateTaskFromPullRequest(linkableTaskKey, pullRequestActivity);

        for (GitHubCommitSnapshot commitSnapshot : snapshot.commits()) {
            if (commitSnapshot == null || commitSnapshot.commitHash() == null || commitSnapshot.commitHash().isBlank()) {
                continue;
            }
            CommitActivity commitActivity = commitActivityRepository
                    .findByRepositoryAndCommitHash(snapshot.repository(), commitSnapshot.commitHash())
                    .orElseGet(CommitActivity::new);
            commitActivity.setRepository(snapshot.repository());
            commitActivity.setCommitHash(commitSnapshot.commitHash());
            commitActivity.setTaskKey(linkableTaskKey);
            commitActivity.setAuthor(commitSnapshot.author());
            commitActivity.setCommittedAt(commitSnapshot.committedAt());
            commitActivityRepository.save(commitActivity);
            updateTaskFromCommit(linkableTaskKey, commitActivity);
        }

        return linkableTaskKey;
    }

    private String resolveLinkableTaskKey(Set<String> taskKeys) {
        if (taskKeys.size() != 1) {
            return null;
        }

        String taskKey = taskKeys.iterator().next();
        ensureTaskExistsWhenBoardMappingExists(taskKey);
        if (taskRepository.existsById(taskKey)) {
            return taskKey;
        }
        return null;
    }

    private void ensureTaskExistsWhenBoardMappingExists(String taskKey) {
        if (taskRepository.existsById(taskKey)) {
            return;
        }
        String[] parts = taskKey.split("-", 2);
        if (parts.length != 2) {
            return;
        }

        String prefix = parts[0];
        String pulseId = parts[1];

        boardMappingRepository.findById(prefix).ifPresent(mapping -> {
            Task task = new Task();
            task.setTaskKey(taskKey);
            task.setPrefix(prefix);
            task.setPulseId(pulseId);
            task.setBoardId(mapping.getBoardId());
            task.setStatus("IN_PROGRESS");
            taskRepository.save(task);
        });
    }

    private void persistPullRequest(JsonNode payload, String repository, String taskKey) {
        JsonNode pullRequestNode = payload.get("pull_request");
        if (pullRequestNode == null || pullRequestNode.isNull()) {
            return;
        }

        int prNumber = payload.path("number").asInt(0);
        if (prNumber <= 0) {
            return;
        }

        PullRequestActivity pullRequestActivity = pullRequestActivityRepository
                .findByRepositoryAndPrNumber(repository, prNumber)
                .orElseGet(PullRequestActivity::new);

        pullRequestActivity.setRepository(repository);
        pullRequestActivity.setPrNumber(prNumber);
        pullRequestActivity.setTaskKey(taskKey);
        pullRequestActivity.setOpenedAt(parseInstant(pullRequestNode.path("created_at").asText(null)));
        pullRequestActivity.setMergedAt(parseInstant(pullRequestNode.path("merged_at").asText(null)));
        pullRequestActivity.setReopenCount(resolveReopenCount(payload, pullRequestActivity.getReopenCount()));

        pullRequestActivityRepository.save(pullRequestActivity);
        updateTaskFromPullRequest(taskKey, pullRequestActivity);
    }

    private int resolveReopenCount(JsonNode payload, Integer current) {
        int existing = current == null ? 0 : current;
        String action = payload.path("action").asText("");
        if ("reopened".equalsIgnoreCase(action)) {
            return existing + 1;
        }
        return existing;
    }

    private void persistCommits(JsonNode payload, String repository, String taskKey) {
        JsonNode commitsNode = payload.get("commits");
        if (commitsNode == null || !commitsNode.isArray()) {
            return;
        }

        for (JsonNode commitNode : commitsNode) {
            String commitHash = commitNode.path("id").asText("");
            if (commitHash.isBlank()) {
                continue;
            }

            CommitActivity commitActivity = commitActivityRepository
                    .findByRepositoryAndCommitHash(repository, commitHash)
                    .orElseGet(CommitActivity::new);

            commitActivity.setRepository(repository);
            commitActivity.setCommitHash(commitHash);
            commitActivity.setTaskKey(taskKey);
            commitActivity.setAuthor(resolveCommitAuthor(commitNode));
            commitActivity.setCommittedAt(parseInstant(commitNode.path("timestamp").asText(null)));
            commitActivityRepository.save(commitActivity);
            updateTaskFromCommit(taskKey, commitActivity);
        }
    }

    private void updateTaskFromPullRequest(String taskKey, PullRequestActivity pullRequestActivity) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskRepository.findById(taskKey).ifPresent(task -> {
            if (task.getStartedAt() == null && pullRequestActivity.getOpenedAt() != null) {
                task.setStartedAt(pullRequestActivity.getOpenedAt());
            }
            if (pullRequestActivity.getMergedAt() != null) {
                if (task.getMergedAt() == null || pullRequestActivity.getMergedAt().isBefore(task.getMergedAt())) {
                    task.setMergedAt(pullRequestActivity.getMergedAt());
                }
            }
            taskRepository.save(task);
        });
    }

    private void updateTaskFromCommit(String taskKey, CommitActivity commitActivity) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskRepository.findById(taskKey).ifPresent(task -> {
            if (task.getStartedAt() == null && commitActivity.getCommittedAt() != null) {
                task.setStartedAt(commitActivity.getCommittedAt());
            }
            if (commitActivity.getCommittedAt() != null) {
                if (task.getFirstCommitAt() == null || commitActivity.getCommittedAt().isBefore(task.getFirstCommitAt())) {
                    task.setFirstCommitAt(commitActivity.getCommittedAt());
                }
            }
            taskRepository.save(task);
        });
    }

    private String resolveRepository(JsonNode payload) {
        String fullName = payload.at("/repository/full_name").asText("");
        if (!fullName.isBlank()) {
            return fullName;
        }
        return payload.at("/repository/name").asText("");
    }

    private List<String> collectTaskKeyCandidates(JsonNode payload) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, payload.at("/pull_request/title").asText(null));
        addCandidate(candidates, payload.at("/pull_request/head/ref").asText(null));
        addCandidate(candidates, payload.at("/ref").asText(null));
        addCandidate(candidates, payload.at("/head_commit/message").asText(null));

        JsonNode commitsNode = payload.get("commits");
        if (commitsNode != null && commitsNode.isArray()) {
            for (JsonNode commitNode : commitsNode) {
                addCandidate(candidates, commitNode.path("message").asText(null));
            }
        }
        return candidates;
    }

    private List<String> snapshotCandidates(GitHubPullRequestSnapshot snapshot) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, snapshot.title());
        addCandidate(candidates, snapshot.branchName());
        for (GitHubCommitSnapshot commitSnapshot : snapshot.commits()) {
            if (commitSnapshot == null) {
                continue;
            }
            addCandidate(candidates, commitSnapshot.message());
        }
        return candidates;
    }

    private void addCandidate(List<String> candidates, String candidate) {
        if (candidate != null && !candidate.isBlank()) {
            candidates.add(candidate);
        }
    }

    private String resolveCommitAuthor(JsonNode commitNode) {
        String authorName = commitNode.at("/author/name").asText("");
        if (!authorName.isBlank()) {
            return authorName;
        }
        String username = commitNode.at("/author/username").asText("");
        if (!username.isBlank()) {
            return username;
        }
        return null;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
