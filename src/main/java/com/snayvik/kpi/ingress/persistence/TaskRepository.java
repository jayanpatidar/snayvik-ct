package com.snayvik.kpi.ingress.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, String> {

    List<Task> findAllByPrefix(String prefix);
}
