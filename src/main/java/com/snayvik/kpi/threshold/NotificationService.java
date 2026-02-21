package com.snayvik.kpi.threshold;

public interface NotificationService {

    void sendEmail(String target, String subject, String body);

    void sendSlack(String target, String message);
}
