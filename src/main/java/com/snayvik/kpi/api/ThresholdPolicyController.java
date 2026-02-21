package com.snayvik.kpi.api;

import com.snayvik.kpi.threshold.ThresholdChangeLog;
import com.snayvik.kpi.threshold.ThresholdPolicy;
import com.snayvik.kpi.threshold.ThresholdPolicyService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/admin/policies")
public class ThresholdPolicyController {

    private final ThresholdPolicyService thresholdPolicyService;

    public ThresholdPolicyController(ThresholdPolicyService thresholdPolicyService) {
        this.thresholdPolicyService = thresholdPolicyService;
    }

    @GetMapping
    public List<ThresholdPolicy> listPolicies() {
        return thresholdPolicyService.listPolicies();
    }

    @GetMapping("/changes")
    public List<ThresholdChangeLog> listChanges() {
        return thresholdPolicyService.listChangeLog();
    }

    @PostMapping
    public ThresholdPolicy upsertPolicy(
            @RequestBody ThresholdPolicy thresholdPolicy,
            @RequestHeader(name = "X-Actor", required = false) String actor) {
        String changedBy = actor == null || actor.isBlank() ? "system-admin" : actor;
        return thresholdPolicyService.upsertPolicy(thresholdPolicy, changedBy);
    }
}
