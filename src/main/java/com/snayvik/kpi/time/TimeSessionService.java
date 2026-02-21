package com.snayvik.kpi.time;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class TimeSessionService {

    public static final String SOURCE_LIVE = "LIVE";
    public static final String SOURCE_MANUAL_EDIT = "MANUAL_EDIT";

    private final TimeSessionRepository timeSessionRepository;

    public TimeSessionService(TimeSessionRepository timeSessionRepository) {
        this.timeSessionRepository = timeSessionRepository;
    }

    @Transactional
    public TimeSession start(String taskKey, String userId) {
        timeSessionRepository.findFirstByUserIdAndEndTimeIsNullOrderByStartTimeDesc(userId).ifPresent(openSession -> {
            stop(userId, openSession.getTaskKey());
        });
        TimeSession session = new TimeSession();
        session.setTaskKey(taskKey);
        session.setUserId(userId);
        session.setStartTime(Instant.now());
        session.setSource(SOURCE_LIVE);
        return timeSessionRepository.save(session);
    }

    @Transactional
    public TimeSession stop(String userId, String taskKey) {
        TimeSession session = timeSessionRepository.findFirstByUserIdAndEndTimeIsNullOrderByStartTimeDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active time session"));
        if (taskKey != null && !taskKey.isBlank() && !taskKey.equals(session.getTaskKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Active session belongs to different task");
        }
        Instant end = Instant.now();
        session.setEndTime(end);
        session.setDurationMinutes((int) Math.max(1, Duration.between(session.getStartTime(), end).toMinutes()));
        return timeSessionRepository.save(session);
    }

    @Transactional
    public TimeSession addManual(
            String taskKey,
            String userId,
            Instant startTime,
            Instant endTime,
            String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual edit requires reason");
        }
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid manual time range");
        }
        TimeSession session = new TimeSession();
        session.setTaskKey(taskKey);
        session.setUserId(userId);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setDurationMinutes((int) Math.max(1, Duration.between(startTime, endTime).toMinutes()));
        session.setSource(SOURCE_MANUAL_EDIT);
        session.setEditReason(reason);
        return timeSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<TimeSession> sessionsForTask(String taskKey) {
        return timeSessionRepository.findByTaskKeyOrderByStartTimeDesc(taskKey);
    }

    @Transactional(readOnly = true)
    public List<TimeSession> sessionsForUser(String userId) {
        return timeSessionRepository.findByUserIdOrderByStartTimeDesc(userId);
    }
}
