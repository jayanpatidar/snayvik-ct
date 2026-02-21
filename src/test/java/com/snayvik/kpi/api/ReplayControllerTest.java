package com.snayvik.kpi.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snayvik.kpi.ingress.audit.WebhookEvent;
import com.snayvik.kpi.ingress.audit.WebhookEventRepository;
import com.snayvik.kpi.ingress.audit.WebhookEventSource;
import com.snayvik.kpi.ingress.queue.RecalculationJobPublisher;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReplayControllerTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private RecalculationJobPublisher recalculationJobPublisher;

    private ReplayController replayController;

    @BeforeEach
    void setUp() {
        replayController = new ReplayController(webhookEventRepository, recalculationJobPublisher);
    }

    @Test
    void requeuesEvent() {
        WebhookEvent event = new WebhookEvent();
        event.setSource("GITHUB");
        when(webhookEventRepository.findById(101L)).thenReturn(Optional.of(event));

        Map<String, Object> response = replayController.replay(101L);

        assertThat(response).containsEntry("status", "requeued");
        verify(recalculationJobPublisher).publish(101L, WebhookEventSource.GITHUB);
    }

    @Test
    void failsWhenEventMissing() {
        when(webhookEventRepository.findById(202L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replayController.replay(202L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
