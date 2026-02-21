package com.snayvik.kpi.kpi;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RecalculationQueueWorkerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private RecalculationDispatchService recalculationDispatchService;

    private RecalculationQueueWorker recalculationQueueWorker;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        recalculationQueueWorker = new RecalculationQueueWorker(
                stringRedisTemplate, new ObjectMapper(), recalculationDispatchService, "kpi:recalc:jobs");
    }

    @Test
    void dispatchesEventIdFromQueuePayload() {
        when(listOperations.leftPop("kpi:recalc:jobs"))
                .thenReturn("{\"eventId\":101,\"source\":\"GITHUB\"}")
                .thenReturn(null);

        recalculationQueueWorker.pollQueue();

        verify(recalculationDispatchService).dispatchByEventId(101L);
    }
}
