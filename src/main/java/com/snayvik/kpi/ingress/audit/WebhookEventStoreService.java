package com.snayvik.kpi.ingress.audit;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookEventStoreService {

    private final WebhookEventRepository webhookEventRepository;

    public WebhookEventStoreService(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }

    @Transactional
    public WebhookStoreResult storeEvent(WebhookEventSource source, String dedupeKey, String correlationId, JsonNode payload) {
        if (webhookEventRepository.existsBySourceAndDedupeKey(source.name(), dedupeKey)) {
            return new WebhookStoreResult(null, true);
        }

        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setSource(source.name());
        webhookEvent.setDedupeKey(dedupeKey);
        webhookEvent.setCorrelationId(correlationId);
        webhookEvent.setPayload(payload);
        webhookEvent.setStatus(WebhookEventStatus.RECEIVED.name());

        try {
            WebhookEvent stored = webhookEventRepository.save(webhookEvent);
            return new WebhookStoreResult(stored.getId(), false);
        } catch (DataIntegrityViolationException exception) {
            return new WebhookStoreResult(null, true);
        }
    }
}
