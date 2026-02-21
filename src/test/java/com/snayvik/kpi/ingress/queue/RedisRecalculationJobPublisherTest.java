package com.snayvik.kpi.ingress.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snayvik.kpi.ingress.audit.WebhookEventSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisRecalculationJobPublisherTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    private RedisRecalculationJobPublisher redisRecalculationJobPublisher;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        redisRecalculationJobPublisher =
                new RedisRecalculationJobPublisher(stringRedisTemplate, new ObjectMapper(), "kpi:recalc:jobs");
    }

    @Test
    void pushesSerializedJobToConfiguredQueue() {
        redisRecalculationJobPublisher.publish(101L, WebhookEventSource.GITHUB);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq("kpi:recalc:jobs"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"eventId\":101");
        assertThat(payloadCaptor.getValue()).contains("\"source\":\"GITHUB\"");
    }
}
