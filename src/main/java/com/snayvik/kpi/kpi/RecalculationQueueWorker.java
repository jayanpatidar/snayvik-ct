package com.snayvik.kpi.kpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecalculationQueueWorker {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RecalculationDispatchService recalculationDispatchService;
    private final String queueName;

    public RecalculationQueueWorker(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            RecalculationDispatchService recalculationDispatchService,
            @Value("${app.ingestion.queue-name:kpi:recalc:jobs}") String queueName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.recalculationDispatchService = recalculationDispatchService;
        this.queueName = queueName;
    }

    @Scheduled(fixedDelayString = "${app.ingestion.worker-delay-ms:3000}")
    public void pollQueue() {
        for (int i = 0; i < 10; i++) {
            String message = stringRedisTemplate.opsForList().leftPop(queueName);
            if (message == null) {
                break;
            }
            processMessage(message);
        }
    }

    private void processMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            Long eventId = node.path("eventId").asLong(0L);
            if (eventId > 0) {
                recalculationDispatchService.dispatchByEventId(eventId);
            }
        } catch (Exception exception) {
            // Keep worker resilient to malformed messages.
        }
    }
}
