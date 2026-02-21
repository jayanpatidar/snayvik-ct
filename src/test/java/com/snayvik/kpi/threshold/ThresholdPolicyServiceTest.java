package com.snayvik.kpi.threshold;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snayvik.kpi.policy.PolicyViolationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThresholdPolicyServiceTest {

    @Mock
    private ThresholdPolicyRepository thresholdPolicyRepository;

    @Mock
    private ThresholdChangeLogRepository thresholdChangeLogRepository;

    @Mock
    private ThresholdNotificationMarkerRepository thresholdNotificationMarkerRepository;

    @Mock
    private PolicyViolationRepository policyViolationRepository;

    @Mock
    private NotificationService notificationService;

    private ThresholdPolicyService thresholdPolicyService;

    @BeforeEach
    void setUp() {
        thresholdPolicyService = new ThresholdPolicyService(
                thresholdPolicyRepository,
                thresholdChangeLogRepository,
                thresholdNotificationMarkerRepository,
                policyViolationRepository,
                notificationService);
    }

    @Test
    void upsertCreatesChangeLogEntry() {
        ThresholdPolicy request = new ThresholdPolicy();
        request.setViolationType("MISSING_TASK_KEY");
        request.setThresholdCount(3);
        request.setTimeWindowDays(7);
        request.setEscalationLevel("WARN");
        request.setNotifyEmail("dev@snayvik.com");
        request.setNotifySlack("slack-dev");
        request.setActive(true);

        when(thresholdPolicyRepository.save(any(ThresholdPolicy.class))).thenAnswer(invocation -> {
            ThresholdPolicy policy = invocation.getArgument(0);
            setPolicyId(policy, 5L);
            return policy;
        });

        thresholdPolicyService.upsertPolicy(request, "admin");

        verify(thresholdPolicyRepository).save(any(ThresholdPolicy.class));
        verify(thresholdChangeLogRepository).save(any(ThresholdChangeLog.class));
    }

    @Test
    void evaluateThresholdsSendsNotificationAndStoresMarker() {
        ThresholdPolicy policy = new ThresholdPolicy();
        policy.setViolationType("MISSING_TASK_KEY");
        policy.setThresholdCount(2);
        policy.setTimeWindowDays(7);
        policy.setEscalationLevel("WARN");
        policy.setNotifyEmail("dev@snayvik.com");
        policy.setNotifySlack("dev-slack");
        policy.setActive(true);
        policy.setUpdatedAt(Instant.now());
        setPolicyId(policy, 10L);

        when(thresholdPolicyRepository.findByActiveTrue()).thenReturn(List.of(policy));
        when(policyViolationRepository.countByTypeGroupedByUserSince(eq("MISSING_TASK_KEY"), any()))
                .thenReturn(List.of(new CountProjection("dev1", 3L)));
        when(thresholdNotificationMarkerRepository.findByThresholdIdAndUserIdAndWindowStart(eq(10L), eq("dev1"), any()))
                .thenReturn(Optional.empty());

        thresholdPolicyService.evaluateThresholds();

        verify(notificationService).sendEmail(eq("dev@snayvik.com"), eq("Snayvik KPI Threshold Alert"), any());
        verify(notificationService).sendSlack(eq("dev-slack"), any());
        verify(thresholdNotificationMarkerRepository).save(any(ThresholdNotificationMarker.class));
    }

    @Test
    void evaluateThresholdsSkipsAlreadyNotifiedMarker() {
        ThresholdPolicy policy = new ThresholdPolicy();
        policy.setViolationType("MISSING_TASK_KEY");
        policy.setThresholdCount(2);
        policy.setTimeWindowDays(7);
        policy.setEscalationLevel("WARN");
        policy.setNotifyEmail("dev@snayvik.com");
        policy.setNotifySlack("dev-slack");
        policy.setActive(true);
        setPolicyId(policy, 10L);

        ThresholdNotificationMarker marker = new ThresholdNotificationMarker();
        marker.setThresholdId(10L);
        marker.setUserId("dev1");
        marker.setWindowStart(LocalDate.now());

        when(thresholdPolicyRepository.findByActiveTrue()).thenReturn(List.of(policy));
        when(policyViolationRepository.countByTypeGroupedByUserSince(eq("MISSING_TASK_KEY"), any()))
                .thenReturn(List.of(new CountProjection("dev1", 3L)));
        when(thresholdNotificationMarkerRepository.findByThresholdIdAndUserIdAndWindowStart(eq(10L), eq("dev1"), any()))
                .thenReturn(Optional.of(marker));

        thresholdPolicyService.evaluateThresholds();

        verify(notificationService, never()).sendEmail(any(), any(), any());
        verify(notificationService, never()).sendSlack(any(), any());
        verify(thresholdNotificationMarkerRepository, never()).save(any());
    }

    private static void setPolicyId(ThresholdPolicy policy, Long id) {
        try {
            var idField = ThresholdPolicy.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(policy, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class CountProjection implements PolicyViolationRepository.UserViolationCount {
        private final String userId;
        private final Long total;

        private CountProjection(String userId, Long total) {
            this.userId = userId;
            this.total = total;
        }

        @Override
        public String getUserId() {
            return userId;
        }

        @Override
        public Long getTotal() {
            return total;
        }
    }
}
