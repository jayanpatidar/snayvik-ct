package com.snayvik.kpi.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/dashboard")
public class DashboardController {

    private final DashboardOverviewService dashboardOverviewService;

    public DashboardController(DashboardOverviewService dashboardOverviewService) {
        this.dashboardOverviewService = dashboardOverviewService;
    }

    @GetMapping("/overview")
    public DashboardOverviewService.DashboardOverviewResponse overview(
            @RequestParam(name = "days", defaultValue = "14") int days) {
        return dashboardOverviewService.buildOverview(days);
    }

    @GetMapping("/snapshots")
    public List<DashboardOverviewService.SnapshotDto> snapshots(@RequestParam(name = "days", defaultValue = "30") int days) {
        return dashboardOverviewService.snapshotsForDays(days);
    }
}
