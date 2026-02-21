package com.snayvik.kpi.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snayvik.kpi.ingress.audit.WebhookEventSource;
import com.snayvik.kpi.ingress.audit.WebhookEventStoreService;
import com.snayvik.kpi.ingress.audit.WebhookStoreResult;
import com.snayvik.kpi.ingress.persistence.MondayTaskPersistenceService;
import com.snayvik.kpi.ingress.queue.RecalculationJobPublisher;
import com.snayvik.kpi.ingress.security.MondayDedupeKeyService;
import com.snayvik.kpi.ingress.security.MondayWebhookAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MondayWebhookControllerTest {

    @Mock
    private WebhookEventStoreService webhookEventStoreService;

    @Mock
    private MondayWebhookAuthService mondayWebhookAuthService;

    @Mock
    private MondayDedupeKeyService mondayDedupeKeyService;

    @Mock
    private RecalculationJobPublisher recalculationJobPublisher;

    @Mock
    private MondayTaskPersistenceService mondayTaskPersistenceService;

    private MondayWebhookController mondayWebhookController;

    @BeforeEach
    void setUp() {
        mondayWebhookController = new MondayWebhookController(
                webhookEventStoreService,
                mondayTaskPersistenceService,
                recalculationJobPublisher,
                mondayWebhookAuthService,
                mondayDedupeKeyService,
                new ObjectMapper());
    }

    @Test
    void returnsChallengeWithoutAuthValidation() {
        ResponseEntity<java.util.Map<String, Object>> response =
                mondayWebhookController.receive(null, null, "{\"challenge\":\"abc\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("challenge", "abc");
    }

    @Test
    void rejectsRequestWhenTokenIsInvalid() {
        when(mondayWebhookAuthService.isAuthorized("Bearer bad-token", null)).thenReturn(false);

        assertThatThrownBy(() ->
                        mondayWebhookController.receive("Bearer bad-token", null, "{\"event\":{\"id\":\"evt-1\"}}"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void acceptsRequestWhenTokenValidAndStoresEvent() {
        when(mondayWebhookAuthService.isAuthorized("Bearer good", null)).thenReturn(true);
        when(mondayDedupeKeyService.resolveDedupeKey(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("evt-123");
        when(webhookEventStoreService.storeEvent(
                        org.mockito.ArgumentMatchers.eq(WebhookEventSource.MONDAY),
                        org.mockito.ArgumentMatchers.eq("evt-123"),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(new WebhookStoreResult(27L, false));

        ResponseEntity<java.util.Map<String, Object>> response =
                mondayWebhookController.receive("Bearer good", null, "{\"event\":{\"id\":\"evt-123\"}}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "accepted");
        assertThat(response.getBody()).containsEntry("source", "monday");
        assertThat(response.getBody()).containsEntry("duplicate", false);
        assertThat(response.getBody()).containsEntry("eventId", 27L);
        assertThat(response.getBody()).containsEntry("queued", true);
        verify(webhookEventStoreService)
                .storeEvent(
                        org.mockito.ArgumentMatchers.eq(WebhookEventSource.MONDAY),
                        org.mockito.ArgumentMatchers.eq("evt-123"),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any());
        verify(mondayTaskPersistenceService).persistFromWebhook(org.mockito.ArgumentMatchers.any());
        verify(recalculationJobPublisher).publish(27L, WebhookEventSource.MONDAY);
    }

    @Test
    void doesNotQueueDuplicateEvent() {
        when(mondayWebhookAuthService.isAuthorized("Bearer good", null)).thenReturn(true);
        when(mondayDedupeKeyService.resolveDedupeKey(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("evt-123");
        when(webhookEventStoreService.storeEvent(
                        org.mockito.ArgumentMatchers.eq(WebhookEventSource.MONDAY),
                        org.mockito.ArgumentMatchers.eq("evt-123"),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(new WebhookStoreResult(null, true));

        ResponseEntity<java.util.Map<String, Object>> response =
                mondayWebhookController.receive("Bearer good", null, "{\"event\":{\"id\":\"evt-123\"}}");

        assertThat(response.getBody()).containsEntry("duplicate", true);
        assertThat(response.getBody()).containsEntry("queued", false);
        verify(mondayTaskPersistenceService, never()).persistFromWebhook(org.mockito.ArgumentMatchers.any());
        verify(recalculationJobPublisher, never()).publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
