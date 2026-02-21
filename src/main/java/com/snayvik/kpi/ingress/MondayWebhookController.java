package com.snayvik.kpi.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snayvik.kpi.ingress.audit.WebhookEventSource;
import com.snayvik.kpi.ingress.audit.WebhookEventStoreService;
import com.snayvik.kpi.ingress.audit.WebhookStoreResult;
import com.snayvik.kpi.ingress.persistence.MondayTaskPersistenceService;
import com.snayvik.kpi.ingress.queue.RecalculationJobPublisher;
import com.snayvik.kpi.ingress.security.MondayDedupeKeyService;
import com.snayvik.kpi.ingress.security.MondayWebhookAuthService;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/webhooks/monday")
public class MondayWebhookController {

    private final WebhookEventStoreService webhookEventStoreService;
    private final MondayTaskPersistenceService mondayTaskPersistenceService;
    private final RecalculationJobPublisher recalculationJobPublisher;
    private final MondayWebhookAuthService mondayWebhookAuthService;
    private final MondayDedupeKeyService mondayDedupeKeyService;
    private final ObjectMapper objectMapper;

    public MondayWebhookController(
            WebhookEventStoreService webhookEventStoreService,
            MondayTaskPersistenceService mondayTaskPersistenceService,
            RecalculationJobPublisher recalculationJobPublisher,
            MondayWebhookAuthService mondayWebhookAuthService,
            MondayDedupeKeyService mondayDedupeKeyService,
            ObjectMapper objectMapper) {
        this.webhookEventStoreService = webhookEventStoreService;
        this.mondayTaskPersistenceService = mondayTaskPersistenceService;
        this.recalculationJobPublisher = recalculationJobPublisher;
        this.mondayWebhookAuthService = mondayWebhookAuthService;
        this.mondayDedupeKeyService = mondayDedupeKeyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Monday-Token", required = false) String mondayTokenHeader,
            @RequestBody(required = false) String rawPayload) {
        JsonNode payload = parsePayload(rawPayload);
        JsonNode challengeNode = payload.get("challenge");
        if (challengeNode != null && !challengeNode.isNull()) {
            return ResponseEntity.ok(Map.of("challenge", challengeNode.asText()));
        }

        if (!mondayWebhookAuthService.isAuthorized(authorizationHeader, mondayTokenHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid monday webhook token");
        }

        String dedupeKey = mondayDedupeKeyService.resolveDedupeKey(payload, rawPayload);
        WebhookStoreResult result = webhookEventStoreService.storeEvent(
                WebhookEventSource.MONDAY,
                dedupeKey,
                UUID.randomUUID().toString(),
                payload);
        boolean queued = false;
        if (!result.duplicate() && result.eventId() != null) {
            mondayTaskPersistenceService.persistFromWebhook(payload);
            recalculationJobPublisher.publish(result.eventId(), WebhookEventSource.MONDAY);
            queued = true;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "accepted");
        response.put("source", "monday");
        response.put("duplicate", result.duplicate());
        response.put("eventId", result.eventId());
        response.put("queued", queued);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    private JsonNode parsePayload(String rawPayload) {
        try {
            if (rawPayload == null || rawPayload.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(rawPayload);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON payload", exception);
        }
    }
}
