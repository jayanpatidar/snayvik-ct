package com.snayvik.kpi.policy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, Long> {

    boolean existsByTaskKeyAndTypeAndResolvedFalse(String taskKey, String type);

    List<PolicyViolation> findByResolvedOrderByCreatedAtDesc(boolean resolved);
}
