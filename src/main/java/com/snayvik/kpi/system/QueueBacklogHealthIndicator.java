package com.snayvik.kpi.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component("queueBacklog")
public class QueueBacklogHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate stringRedisTemplate;
    private final String queueName;
    private final long criticalBacklog;

    public QueueBacklogHealthIndicator(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.ingestion.queue-name:kpi:recalc:jobs}") String queueName,
            @Value("${app.health.queue-backlog-critical:1000}") long criticalBacklog) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.queueName = queueName;
        this.criticalBacklog = criticalBacklog;
    }

    @Override
    public Health health() {
        try {
            Long size = stringRedisTemplate.opsForList().size(queueName);
            long backlog = size == null ? 0L : size;
            if (backlog > criticalBacklog) {
                return Health.down().withDetail("queue", queueName).withDetail("backlog", backlog).build();
            }
            return Health.up().withDetail("queue", queueName).withDetail("backlog", backlog).build();
        } catch (Exception exception) {
            return Health.down(exception).withDetail("queue", queueName).build();
        }
    }
}
