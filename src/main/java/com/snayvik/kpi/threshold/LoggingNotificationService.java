package com.snayvik.kpi.threshold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void sendEmail(String target, String subject, String body) {
        logger.info("EMAIL notification target={} subject={} body={}", target, subject, body);
    }

    @Override
    public void sendSlack(String target, String message) {
        logger.info("SLACK notification target={} message={}", target, message);
    }
}
