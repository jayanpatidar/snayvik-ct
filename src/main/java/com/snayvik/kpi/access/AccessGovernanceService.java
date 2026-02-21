package com.snayvik.kpi.access;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessGovernanceService {

    private final UserAccountRepository userAccountRepository;
    private final UserSkillGroupRepository userSkillGroupRepository;
    private final AccessAuditLogRepository accessAuditLogRepository;
    private final List<ExternalAccessAdapter> externalAccessAdapters;

    public AccessGovernanceService(
            UserAccountRepository userAccountRepository,
            UserSkillGroupRepository userSkillGroupRepository,
            AccessAuditLogRepository accessAuditLogRepository,
            List<ExternalAccessAdapter> externalAccessAdapters) {
        this.userAccountRepository = userAccountRepository;
        this.userSkillGroupRepository = userSkillGroupRepository;
        this.accessAuditLogRepository = accessAuditLogRepository;
        this.externalAccessAdapters = externalAccessAdapters;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> listActiveUsers() {
        return userAccountRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<AccessAuditLog> listAuditLogs() {
        return accessAuditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    @Transactional
    public UserAccount deactivateUser(String targetUserId, String performedByUserId) {
        if (targetUserId.equals(performedByUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Self-lockout protection: cannot deactivate yourself");
        }

        UserAccount userAccount = userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!userAccount.isActive()) {
            return userAccount;
        }

        boolean targetIsAdmin = userSkillGroupRepository.countAdminMembership(targetUserId) > 0;
        long activeAdminCount = userSkillGroupRepository.countActiveAdmins();
        if (targetIsAdmin && activeAdminCount <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Last-admin removal is blocked");
        }

        for (ExternalAccessAdapter adapter : externalAccessAdapters) {
            AccessAuditLog auditLog = new AccessAuditLog();
            auditLog.setUserId(targetUserId);
            auditLog.setSystem(adapter.systemName());
            auditLog.setAction("REVOKE");
            auditLog.setTarget(userAccount.getEmail());
            auditLog.setPerformedBy(performedByUserId);
            auditLog.setTimestamp(Instant.now());
            try {
                adapter.revokeAllAccess(userAccount);
                auditLog.setStatus("SUCCESS");
            } catch (Exception exception) {
                auditLog.setStatus("FAILED");
            }
            accessAuditLogRepository.save(auditLog);
        }

        userAccount.setActive(false);
        userAccount.setDeactivatedAt(Instant.now());
        return userAccountRepository.save(userAccount);
    }
}
