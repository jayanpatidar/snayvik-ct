package com.snayvik.kpi.threshold;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThresholdNotificationMarkerRepository extends JpaRepository<ThresholdNotificationMarker, Long> {

    Optional<ThresholdNotificationMarker> findByThresholdIdAndUserIdAndWindowStart(
            Long thresholdId,
            String userId,
            LocalDate windowStart);

    long deleteByWindowStartBefore(LocalDate cutoffDate);
}
