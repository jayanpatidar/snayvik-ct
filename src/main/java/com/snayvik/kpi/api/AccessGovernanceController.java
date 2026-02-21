package com.snayvik.kpi.api;

import com.snayvik.kpi.access.AccessAuditLog;
import com.snayvik.kpi.access.AccessGovernanceService;
import com.snayvik.kpi.access.UserAccount;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/admin/access")
public class AccessGovernanceController {

    private final AccessGovernanceService accessGovernanceService;

    public AccessGovernanceController(AccessGovernanceService accessGovernanceService) {
        this.accessGovernanceService = accessGovernanceService;
    }

    @GetMapping("/users")
    public List<UserAccount> users() {
        return accessGovernanceService.listActiveUsers();
    }

    @GetMapping("/audit")
    public List<AccessAuditLog> audit() {
        return accessGovernanceService.listAuditLogs();
    }

    @PostMapping("/users/{userId}/deactivate")
    public UserAccount deactivate(
            @PathVariable String userId,
            @RequestBody DeactivateRequest request) {
        return accessGovernanceService.deactivateUser(userId, request.performedBy());
    }

    public record DeactivateRequest(String performedBy) {
    }
}
