package com.snayvik.kpi.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/governance-rules")
public class GovernanceRulesController {

    @GetMapping
    public Map<String, Object> rules() {
        return Map.of(
                "lastUpdatedAt", Instant.now().toString(),
                "rules", List.of(
                        Map.of("code", "MISSING_TASK_KEY", "description", "PR/branch/commit must include one task_key."),
                        Map.of("code", "MULTIPLE_TASK_KEYS", "description", "Single PR must map to one task_key."),
                        Map.of("code", "DONE_WITHOUT_MERGE", "description", "Done status requires at least one merged PR."),
                        Map.of("code", "DRIFT_AFTER_START", "description", "Description changes after start are drift violations.")));
    }
}
