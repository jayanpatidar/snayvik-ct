package com.snayvik.kpi.kpi;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskMetricsRepository extends JpaRepository<TaskMetrics, String> {

    List<TaskMetrics> findAllByTaskKeyIn(Collection<String> taskKeys);
}
