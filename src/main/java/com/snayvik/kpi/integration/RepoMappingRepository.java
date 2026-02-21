package com.snayvik.kpi.integration;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoMappingRepository extends JpaRepository<RepoMapping, Long> {

    Optional<RepoMapping> findByRepository(String repository);

    List<RepoMapping> findAllByOrderByRepositoryAsc();
}
