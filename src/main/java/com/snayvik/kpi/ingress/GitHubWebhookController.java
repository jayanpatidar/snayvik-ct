package com.snayvik.kpi.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snayvik.kpi.ingress.audit.WebhookEventSource;
import com.snayvik.kpi.ingress.audit.WebhookEventStoreService;
import com.snayvik.kpi.ingress.audit.WebhookStoreResult;
import com.snayvik.kpi.ingress.persistence.GitHubActivityPersistenceService;
import com.snayvik.kpi.ingress.queue.RecalculationJobPublisher;
import com.snayvik.kpi.ingress.security.GitHubWebhookAuthService;
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
@RequestMapping("/webhooks/github")
public class GitHubWebhookController {

    private final WebhookEventStoreService webhookEventStoreService;
    private final GitHubActivityPersistenceService gitHubActivityPersistenceService;
    private final RecalculationJobPublisher recalculationJobPublisher;
    private final GitHubWebhookAuthService gitHubWebhookAuthService;
    private final ObjectMapper objectMapper;

    public GitHubWebhookController(
            WebhookEventStoreService webhookEventStoreService,
            GitHubActivityPersistenceService gitHubActivityPersistenceService,
            RecalculationJobPublisher recalculationJobPublisher,
            GitHubWebhookAuthService gitHubWebhookAuthService,
            ObjectMapper objectMapper) {
        this.webhookEventStoreService = webhookEventStoreService;
        this.gitHubActivityPersistenceService = gitHubActivityPersistenceService;
        this.recalculationJobPublisher = recalculationJobPublisher;
        this.gitHubWebhookAuthService = gitHubWebhookAuthService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) String rawPayload) {
        if (deliveryId == null || deliveryId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing X-GitHub-Delivery header");
        }
        if (!gitHubWebhookAuthService.isValidSignature(rawPayload, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid GitHub webhook signature");
        }

        JsonNode payload = parsePayload(rawPayload);
        WebhookStoreResult result = webhookEventStoreService.storeEvent(
                WebhookEventSource.GITHUB,
                deliveryId,
                UUID.randomUUID().toString(),
                payload);
        boolean queued = false;
        if (!result.duplicate() && result.eventId() != null) {
            gitHubActivityPersistenceService.persistFromWebhook(payload);
            recalculationJobPublisher.publish(result.eventId(), WebhookEventSource.GITHUB);
            queued = true;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "accepted");
        response.put("source", "github");
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
