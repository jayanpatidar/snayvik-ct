package com.snayvik.kpi.ingress.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class WebhookEventStoreServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private WebhookEventRepository webhookEventRepository;

    private WebhookEventStoreService webhookEventStoreService;

    @BeforeEach
    void setUp() {
        webhookEventStoreService = new WebhookEventStoreService(webhookEventRepository);
    }

    @Test
    void marksAsDuplicateWhenDedupeKeyAlreadyExists() {
        when(webhookEventRepository.existsBySourceAndDedupeKey("GITHUB", "delivery-1")).thenReturn(true);

        WebhookStoreResult result = webhookEventStoreService.storeEvent(
                WebhookEventSource.GITHUB,
                "delivery-1",
                "corr-1",
                objectMapper.createObjectNode().put("action", "opened"));

        assertThat(result.duplicate()).isTrue();
        assertThat(result.eventId()).isNull();
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void storesEventWhenDedupeKeyDoesNotExist() {
        when(webhookEventRepository.existsBySourceAndDedupeKey("GITHUB", "delivery-2")).thenReturn(false);
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> {
            WebhookEvent event = invocation.getArgument(0);
            event.setSource("GITHUB");
            event.setDedupeKey("delivery-2");
            event.setCorrelationId("corr-2");
            event.setStatus(WebhookEventStatus.RECEIVED.name());
            return withId(event, 42L);
        });

        WebhookStoreResult result = webhookEventStoreService.storeEvent(
                WebhookEventSource.GITHUB,
                "delivery-2",
                "corr-2",
                objectMapper.createObjectNode().put("action", "opened"));

        assertThat(result.duplicate()).isFalse();
        assertThat(result.eventId()).isEqualTo(42L);
    }

    @Test
    void marksAsDuplicateWhenUniqueConstraintRaceOccurs() {
        when(webhookEventRepository.existsBySourceAndDedupeKey("MONDAY", "event-123")).thenReturn(false);
        when(webhookEventRepository.save(any(WebhookEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        WebhookStoreResult result = webhookEventStoreService.storeEvent(
                WebhookEventSource.MONDAY,
                "event-123",
                "corr-3",
                objectMapper.createObjectNode().put("event", "change_column_value"));

        assertThat(result.duplicate()).isTrue();
        assertThat(result.eventId()).isNull();
    }

    private static WebhookEvent withId(WebhookEvent event, Long id) {
        try {
            var idField = WebhookEvent.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, id);
            return event;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set entity id in test", exception);
        }
    }
}
