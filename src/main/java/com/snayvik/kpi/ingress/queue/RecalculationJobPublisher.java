package com.snayvik.kpi.ingress.queue;

import com.snayvik.kpi.ingress.audit.WebhookEventSource;

public interface RecalculationJobPublisher {

    void publish(Long eventId, WebhookEventSource source);
}
