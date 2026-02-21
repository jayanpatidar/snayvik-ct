package com.snayvik.kpi.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.snayvik.kpi.ingress.audit.WebhookEventRepository;
import com.snayvik.kpi.threshold.ThresholdNotificationMarkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private ThresholdNotificationMarkerRepository thresholdNotificationMarkerRepository;

    private RetentionService retentionService;

    @BeforeEach
    void setUp() {
        retentionService = new RetentionService(
                webhookEventRepository,
                thresholdNotificationMarkerRepository,
                90,
                120);
    }

    @Test
    void appliesRetentionCutoffs() {
        retentionService.applyRetentionPolicies();
        verify(webhookEventRepository).deleteByReceivedAtBefore(any());
        verify(thresholdNotificationMarkerRepository).deleteByWindowStartBefore(any());
    }
}
