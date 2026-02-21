package com.snayvik.kpi.policy;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyViolationService {

    private final PolicyViolationRepository policyViolationRepository;

    public PolicyViolationService(PolicyViolationRepository policyViolationRepository) {
        this.policyViolationRepository = policyViolationRepository;
    }

    @Transactional
    public void createViolation(String taskKey, String userId, String type, String severity, String message) {
        if (taskKey != null && !taskKey.isBlank()
                && policyViolationRepository.existsByTaskKeyAndTypeAndResolvedFalse(taskKey, type)) {
            return;
        }
        PolicyViolation violation = new PolicyViolation();
        violation.setTaskKey(taskKey);
        violation.setUserId(userId);
        violation.setType(type);
        violation.setSeverity(severity);
        violation.setMessage(message);
        policyViolationRepository.save(violation);
    }

    @Transactional
    public PolicyViolation resolveViolation(Long violationId, String resolvedBy, String reason) {
        PolicyViolation violation = policyViolationRepository.findById(violationId).orElseThrow();
        violation.setResolved(true);
        violation.setResolvedBy(resolvedBy);
        violation.setResolvedReason(reason);
        violation.setResolvedAt(Instant.now());
        return policyViolationRepository.save(violation);
    }

    @Transactional(readOnly = true)
    public List<PolicyViolation> listByResolved(boolean resolved) {
        return policyViolationRepository.findByResolvedOrderByCreatedAtDesc(resolved);
    }
}
