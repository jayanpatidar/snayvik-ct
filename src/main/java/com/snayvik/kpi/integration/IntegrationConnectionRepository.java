package com.snayvik.kpi.integration;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, Long> {

    Optional<IntegrationConnection> findBySystemName(String systemName);

    List<IntegrationConnection> findAllByOrderBySystemNameAsc();
}
