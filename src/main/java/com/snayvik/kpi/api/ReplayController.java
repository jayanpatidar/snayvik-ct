package com.snayvik.kpi.api;

import com.snayvik.kpi.ingress.audit.WebhookEvent;
import com.snayvik.kpi.ingress.audit.WebhookEventRepository;
import com.snayvik.kpi.ingress.audit.WebhookEventSource;
import com.snayvik.kpi.ingress.queue.RecalculationJobPublisher;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/kpi/admin/replay")
public class ReplayController {

    private final WebhookEventRepository webhookEventRepository;
    private final RecalculationJobPublisher recalculationJobPublisher;

    public ReplayController(
            WebhookEventRepository webhookEventRepository,
            RecalculationJobPublisher recalculationJobPublisher) {
        this.webhookEventRepository = webhookEventRepository;
        this.recalculationJobPublisher = recalculationJobPublisher;
    }

    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> replay(@PathVariable Long eventId) {
        WebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        WebhookEventSource source;
        try {
            source = WebhookEventSource.valueOf(event.getSource().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported event source for replay");
        }
        recalculationJobPublisher.publish(eventId, source);
        return Map.of("status", "requeued", "eventId", eventId, "source", source.name());
    }
}
