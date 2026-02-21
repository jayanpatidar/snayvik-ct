package com.snayvik.kpi.ingress.audit;

public record WebhookStoreResult(Long eventId, boolean duplicate) {
}
