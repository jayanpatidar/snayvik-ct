package com.snayvik.kpi.system;

import com.snayvik.kpi.ingress.audit.WebhookEventRepository;
import com.snayvik.kpi.threshold.ThresholdNotificationMarkerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetentionService {

    private final WebhookEventRepository webhookEventRepository;
    private final ThresholdNotificationMarkerRepository thresholdNotificationMarkerRepository;
    private final int webhookRetentionDays;
    private final int markerRetentionDays;

    public RetentionService(
            WebhookEventRepository webhookEventRepository,
            ThresholdNotificationMarkerRepository thresholdNotificationMarkerRepository,
            @Value("${app.retention.webhook-days:90}") int webhookRetentionDays,
            @Value("${app.retention.threshold-marker-days:120}") int markerRetentionDays) {
        this.webhookEventRepository = webhookEventRepository;
        this.thresholdNotificationMarkerRepository = thresholdNotificationMarkerRepository;
        this.webhookRetentionDays = webhookRetentionDays;
        this.markerRetentionDays = markerRetentionDays;
    }

    @Scheduled(cron = "${app.retention.cron:0 20 1 * * *}")
    @Transactional
    public void applyRetentionPolicies() {
        Instant webhookCutoff = Instant.now().minusSeconds(webhookRetentionDays * 86400L);
        webhookEventRepository.deleteByReceivedAtBefore(webhookCutoff);

        LocalDate markerCutoff = LocalDate.now(ZoneOffset.UTC).minusDays(markerRetentionDays);
        thresholdNotificationMarkerRepository.deleteByWindowStartBefore(markerCutoff);
    }
}
