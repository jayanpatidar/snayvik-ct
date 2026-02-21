package com.snayvik.kpi.api;

import com.snayvik.kpi.ingress.audit.WebhookEventRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/system")
public class SystemHealthController {

    private final StringRedisTemplate stringRedisTemplate;
    private final WebhookEventRepository webhookEventRepository;
    private final String queueName;

    public SystemHealthController(
            StringRedisTemplate stringRedisTemplate,
            WebhookEventRepository webhookEventRepository,
            @Value("${app.ingestion.queue-name:kpi:recalc:jobs}") String queueName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.webhookEventRepository = webhookEventRepository;
        this.queueName = queueName;
    }

    @GetMapping("/health-summary")
    public Map<String, Object> healthSummary() {
        Long backlog = stringRedisTemplate.opsForList().size(queueName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("queueName", queueName);
        response.put("queueBacklog", backlog == null ? 0L : backlog);
        response.put("webhookReceived", webhookEventRepository.countByStatus("RECEIVED"));
        response.put("webhookProcessed", webhookEventRepository.countByStatus("PROCESSED"));
        response.put("webhookFailed", webhookEventRepository.countByStatus("FAILED"));
        return response;
    }
}
