package com.snayvik.kpi.threshold;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThresholdChangeLogRepository extends JpaRepository<ThresholdChangeLog, Long> {

    List<ThresholdChangeLog> findTop100ByOrderByChangedAtDesc();
}
