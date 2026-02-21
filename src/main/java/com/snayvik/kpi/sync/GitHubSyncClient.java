package com.snayvik.kpi.sync;

import java.time.Instant;
import java.util.List;

public interface GitHubSyncClient {

    List<GitHubPullRequestSnapshot> fetchPullRequests(String repository, Instant since);
}
