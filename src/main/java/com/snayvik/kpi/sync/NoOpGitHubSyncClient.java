package com.snayvik.kpi.sync;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpGitHubSyncClient implements GitHubSyncClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpGitHubSyncClient.class);

    @Override
    public List<GitHubPullRequestSnapshot> fetchPullRequests(String repository, Instant since) {
        LOGGER.debug("NoOpGitHubSyncClient active; skipping GitHub full sync for repository {}", repository);
        return List.of();
    }
}
