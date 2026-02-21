package com.snayvik.kpi.api;

import com.snayvik.kpi.policy.PolicyViolation;
import com.snayvik.kpi.policy.PolicyViolationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/violations")
public class PolicyViolationController {

    private final PolicyViolationService policyViolationService;

    public PolicyViolationController(PolicyViolationService policyViolationService) {
        this.policyViolationService = policyViolationService;
    }

    @GetMapping
    public List<PolicyViolation> list(@RequestParam(name = "resolved", defaultValue = "false") boolean resolved) {
        return policyViolationService.listByResolved(resolved);
    }

    @PostMapping("/{violationId}/resolve")
    public Map<String, Object> resolve(
            @PathVariable Long violationId,
            @RequestBody ResolveViolationRequest request) {
        PolicyViolation violation = policyViolationService.resolveViolation(
                violationId,
                request.resolvedBy(),
                request.reason());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", violation.getId());
        response.put("resolved", violation.isResolved());
        response.put("resolvedBy", violation.getResolvedBy());
        response.put("resolvedReason", violation.getResolvedReason());
        response.put("resolvedAt", violation.getResolvedAt());
        return response;
    }

    public record ResolveViolationRequest(String resolvedBy, String reason) {
    }
}
