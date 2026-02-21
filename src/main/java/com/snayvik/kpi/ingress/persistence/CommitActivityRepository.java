package com.snayvik.kpi.ingress.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitActivityRepository extends JpaRepository<CommitActivity, Long> {

    Optional<CommitActivity> findByRepositoryAndCommitHash(String repository, String commitHash);

    List<CommitActivity> findAllByTaskKey(String taskKey);
}
