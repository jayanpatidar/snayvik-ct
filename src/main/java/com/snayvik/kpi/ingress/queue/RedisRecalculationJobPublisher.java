package com.snayvik.kpi.ingress.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snayvik.kpi.ingress.audit.WebhookEventSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRecalculationJobPublisher implements RecalculationJobPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public RedisRecalculationJobPublisher(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.ingestion.queue-name:kpi:recalc:jobs}") String queueName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    @Override
    public void publish(Long eventId, WebhookEventSource source) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("eventId", eventId);
        job.put("source", source.name());
        job.put("enqueuedAt", Instant.now().toString());

        try {
            String payload = objectMapper.writeValueAsString(job);
            stringRedisTemplate.opsForList().rightPush(queueName, payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to enqueue recalculation job", exception);
        }
    }
}
