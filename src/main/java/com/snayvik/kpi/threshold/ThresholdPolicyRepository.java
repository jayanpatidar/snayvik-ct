package com.snayvik.kpi.threshold;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThresholdPolicyRepository extends JpaRepository<ThresholdPolicy, Long> {

    List<ThresholdPolicy> findByActiveTrue();
}
