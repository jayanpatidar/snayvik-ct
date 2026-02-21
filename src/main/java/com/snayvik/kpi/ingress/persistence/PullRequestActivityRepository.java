package com.snayvik.kpi.ingress.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PullRequestActivityRepository extends JpaRepository<PullRequestActivity, Long> {

    Optional<PullRequestActivity> findByRepositoryAndPrNumber(String repository, Integer prNumber);

    List<PullRequestActivity> findAllByTaskKey(String taskKey);
}
