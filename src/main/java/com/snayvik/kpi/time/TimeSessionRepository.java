package com.snayvik.kpi.time;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeSessionRepository extends JpaRepository<TimeSession, Long> {

    Optional<TimeSession> findFirstByUserIdAndEndTimeIsNullOrderByStartTimeDesc(String userId);

    List<TimeSession> findByTaskKeyOrderByStartTimeDesc(String taskKey);

    List<TimeSession> findByUserIdOrderByStartTimeDesc(String userId);

    @Query("select coalesce(sum(s.durationMinutes), 0) from TimeSession s where s.taskKey = :taskKey")
    Long sumDurationByTaskKey(@Param("taskKey") String taskKey);
}
