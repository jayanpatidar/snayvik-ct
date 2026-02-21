package com.snayvik.kpi.api;

import com.snayvik.kpi.time.TimeSession;
import com.snayvik.kpi.time.TimeGovernanceService;
import com.snayvik.kpi.time.TimeSessionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/time")
public class TimeTrackingController {

    private final TimeSessionService timeSessionService;
    private final TimeGovernanceService timeGovernanceService;

    public TimeTrackingController(TimeSessionService timeSessionService, TimeGovernanceService timeGovernanceService) {
        this.timeSessionService = timeSessionService;
        this.timeGovernanceService = timeGovernanceService;
    }

    @PostMapping("/start")
    public TimeSession start(@RequestBody StartStopRequest request) {
        return timeSessionService.start(request.taskKey(), request.userId());
    }

    @PostMapping("/stop")
    public TimeSession stop(@RequestBody StartStopRequest request) {
        return timeSessionService.stop(request.userId(), request.taskKey());
    }

    @PostMapping("/manual")
    public TimeSession manual(@RequestBody ManualTimeRequest request) {
        return timeSessionService.addManual(
                request.taskKey(),
                request.userId(),
                Instant.parse(request.startTime()),
                Instant.parse(request.endTime()),
                request.reason());
    }

    @GetMapping("/task/{taskKey}")
    public List<TimeSession> byTask(@PathVariable String taskKey) {
        return timeSessionService.sessionsForTask(taskKey);
    }

    @GetMapping("/user/{userId}")
    public List<TimeSession> byUser(@PathVariable String userId) {
        return timeSessionService.sessionsForUser(userId);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "time-tracking");
    }

    @GetMapping("/summary")
    public List<Map<String, Object>> summary() {
        return timeGovernanceService.summaryByTask();
    }

    public record StartStopRequest(String taskKey, String userId) {
    }

    public record ManualTimeRequest(String taskKey, String userId, String startTime, String endTime, String reason) {
    }
}
