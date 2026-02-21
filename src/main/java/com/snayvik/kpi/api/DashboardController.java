package com.snayvik.kpi.api;

import com.snayvik.kpi.kpi.DailySnapshot;
import com.snayvik.kpi.kpi.DailySnapshotService;
import com.snayvik.kpi.kpi.TaskMetrics;
import com.snayvik.kpi.kpi.TaskMetricsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/dashboard")
public class DashboardController {

    private final TaskMetricsRepository taskMetricsRepository;
    private final DailySnapshotService dailySnapshotService;

    public DashboardController(TaskMetricsRepository taskMetricsRepository, DailySnapshotService dailySnapshotService) {
        this.taskMetricsRepository = taskMetricsRepository;
        this.dailySnapshotService = dailySnapshotService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam(name = "days", defaultValue = "14") int days) {
        List<TaskMetrics> metrics = taskMetricsRepository.findAll();
        List<DailySnapshot> snapshots = dailySnapshotService.getSnapshotsForLastDays(days);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("taskCount", metrics.size());
        response.put("avgLeadTimeSeconds", averageLong(metrics.stream().map(TaskMetrics::getLeadTimeSeconds).toList()));
        response.put("avgRiskScore", averageDouble(metrics.stream().map(TaskMetrics::getRiskScore).toList()));
        response.put("avgReworkRate", averageDouble(metrics.stream().map(TaskMetrics::getReworkRate).toList()));
        response.put("snapshots", snapshots.stream().map(this::snapshotDto).toList());
        return response;
    }

    @GetMapping("/snapshots")
    public List<Map<String, Object>> snapshots(@RequestParam(name = "days", defaultValue = "30") int days) {
        return dailySnapshotService.getSnapshotsForLastDays(days).stream().map(this::snapshotDto).toList();
    }

    private Map<String, Object> snapshotDto(DailySnapshot snapshot) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", snapshot.getSnapshotDate().toString());
        row.put("prefix", snapshot.getPrefix());
        row.put("avgLeadTime", snapshot.getAvgLeadTime());
        row.put("avgRiskScore", snapshot.getAvgRiskScore());
        row.put("driftRate", snapshot.getDriftRate());
        row.put("reworkRate", snapshot.getReworkRate());
        return row;
    }

    private long averageLong(List<Long> values) {
        List<Long> nonNull = values.stream().filter(java.util.Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return 0L;
        }
        return Math.round(nonNull.stream().mapToLong(Long::longValue).average().orElse(0.0));
    }

    private double averageDouble(List<Double> values) {
        List<Double> nonNull = values.stream().filter(java.util.Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return 0.0;
        }
        return Math.round(nonNull.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0) / 100.0;
    }
}
