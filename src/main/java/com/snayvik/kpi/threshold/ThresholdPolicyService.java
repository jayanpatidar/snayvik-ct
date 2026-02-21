package com.snayvik.kpi.threshold;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThresholdPolicyService {

    private final ThresholdPolicyRepository thresholdPolicyRepository;
    private final ThresholdChangeLogRepository thresholdChangeLogRepository;
    private final ThresholdNotificationMarkerRepository thresholdNotificationMarkerRepository;
    private final com.snayvik.kpi.policy.PolicyViolationRepository policyViolationRepository;
    private final NotificationService notificationService;

    public ThresholdPolicyService(
            ThresholdPolicyRepository thresholdPolicyRepository,
            ThresholdChangeLogRepository thresholdChangeLogRepository,
            ThresholdNotificationMarkerRepository thresholdNotificationMarkerRepository,
            com.snayvik.kpi.policy.PolicyViolationRepository policyViolationRepository,
            NotificationService notificationService) {
        this.thresholdPolicyRepository = thresholdPolicyRepository;
        this.thresholdChangeLogRepository = thresholdChangeLogRepository;
        this.thresholdNotificationMarkerRepository = thresholdNotificationMarkerRepository;
        this.policyViolationRepository = policyViolationRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ThresholdPolicy> listPolicies() {
        return thresholdPolicyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ThresholdChangeLog> listChangeLog() {
        return thresholdChangeLogRepository.findTop100ByOrderByChangedAtDesc();
    }

    @Transactional
    public ThresholdPolicy upsertPolicy(ThresholdPolicy request, String changedBy) {
        ThresholdPolicy policy = request.getId() == null
                ? new ThresholdPolicy()
                : thresholdPolicyRepository.findById(request.getId()).orElse(new ThresholdPolicy());

        String oldValue = toLogValue(policy);
        policy.setViolationType(request.getViolationType());
        policy.setThresholdCount(request.getThresholdCount());
        policy.setTimeWindowDays(request.getTimeWindowDays());
        policy.setEscalationLevel(request.getEscalationLevel());
        policy.setNotifyEmail(request.getNotifyEmail());
        policy.setNotifySlack(request.getNotifySlack());
        policy.setActive(request.isActive());
        policy.setUpdatedBy(changedBy);
        policy.setUpdatedAt(Instant.now());
        ThresholdPolicy saved = thresholdPolicyRepository.save(policy);

        ThresholdChangeLog changeLog = new ThresholdChangeLog();
        changeLog.setThresholdId(saved.getId());
        changeLog.setOldValue(oldValue);
        changeLog.setNewValue(toLogValue(saved));
        changeLog.setChangedBy(changedBy);
        changeLog.setChangedAt(Instant.now());
        thresholdChangeLogRepository.save(changeLog);
        return saved;
    }

    @Scheduled(cron = "${app.thresholds.cron:0 */10 * * * *}")
    @Transactional
    public void evaluateThresholds() {
        List<ThresholdPolicy> activePolicies = thresholdPolicyRepository.findByActiveTrue();
        for (ThresholdPolicy policy : activePolicies) {
            Instant since = Instant.now().minusSeconds(policy.getTimeWindowDays().longValue() * 86400L);
            LocalDate windowStart = since.atZone(ZoneOffset.UTC).toLocalDate();

            List<com.snayvik.kpi.policy.PolicyViolationRepository.UserViolationCount> counts =
                    policyViolationRepository.countByTypeGroupedByUserSince(policy.getViolationType(), since);

            for (com.snayvik.kpi.policy.PolicyViolationRepository.UserViolationCount count : counts) {
                if (count.getTotal() < policy.getThresholdCount()) {
                    continue;
                }
                String userId = count.getUserId();
                boolean alreadyNotified = thresholdNotificationMarkerRepository
                        .findByThresholdIdAndUserIdAndWindowStart(policy.getId(), userId, windowStart)
                        .isPresent();
                if (alreadyNotified) {
                    continue;
                }
                String message = "Threshold reached for " + policy.getViolationType()
                        + " count=" + count.getTotal() + " windowDays=" + policy.getTimeWindowDays();
                if (policy.getNotifyEmail() != null && !policy.getNotifyEmail().isBlank()) {
                    notificationService.sendEmail(policy.getNotifyEmail(), "Snayvik KPI Threshold Alert", message);
                }
                if (policy.getNotifySlack() != null && !policy.getNotifySlack().isBlank()) {
                    notificationService.sendSlack(policy.getNotifySlack(), message + " user=" + userId);
                }

                ThresholdNotificationMarker marker = new ThresholdNotificationMarker();
                marker.setThresholdId(policy.getId());
                marker.setUserId(userId);
                marker.setWindowStart(windowStart);
                marker.setLastNotifiedAt(Instant.now());
                thresholdNotificationMarkerRepository.save(marker);
            }
        }
    }

    private String toLogValue(ThresholdPolicy policy) {
        if (policy.getViolationType() == null) {
            return "EMPTY";
        }
        return policy.getViolationType() + ":" + policy.getThresholdCount() + ":" + policy.getTimeWindowDays()
                + ":" + policy.getEscalationLevel() + ":" + policy.isActive();
    }
}
