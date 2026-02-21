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
import com.snayvik.kpi.ingress.persistence.GitHubActivityPersistenceService;
import com.snayvik.kpi.ingress.queue.RecalculationJobPublisher;
import com.snayvik.kpi.ingress.security.GitHubWebhookAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GitHubWebhookControllerTest {

    @Mock
    private WebhookEventStoreService webhookEventStoreService;

    @Mock
    private GitHubWebhookAuthService gitHubWebhookAuthService;

    @Mock
    private RecalculationJobPublisher recalculationJobPublisher;

    @Mock
    private GitHubActivityPersistenceService gitHubActivityPersistenceService;

    private GitHubWebhookController gitHubWebhookController;

    @BeforeEach
    void setUp() {
        gitHubWebhookController =
                new GitHubWebhookController(
                        webhookEventStoreService,
                        gitHubActivityPersistenceService,
                        recalculationJobPublisher,
                        gitHubWebhookAuthService,
                        new ObjectMapper());
    }

    @Test
    void returnsAcceptedWhenSignatureValidAndDeliveryPresent() {
        when(gitHubWebhookAuthService.isValidSignature("{\"action\":\"opened\"}", "sha256=ok")).thenReturn(true);
        when(webhookEventStoreService.storeEvent(
                        org.mockito.ArgumentMatchers.eq(WebhookEventSource.GITHUB),
                        org.mockito.ArgumentMatchers.eq("delivery-1"),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(new WebhookStoreResult(11L, false));

        ResponseEntity<java.util.Map<String, Object>> response = gitHubWebhookController.receive(
                "delivery-1",
                "sha256=ok",
                "{\"action\":\"opened\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "accepted");
        assertThat(response.getBody()).containsEntry("source", "github");
        assertThat(response.getBody()).containsEntry("duplicate", false);
        assertThat(response.getBody()).containsEntry("eventId", 11L);
        assertThat(response.getBody()).containsEntry("queued", true);
        verify(webhookEventStoreService)
                .storeEvent(
                        org.mockito.ArgumentMatchers.eq(WebhookEventSource.GITHUB),
                        org.mockito.ArgumentMatchers.eq("delivery-1"),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any());
        verify(gitHubActivityPersistenceService).persistFromWebhook(org.mockito.ArgumentMatchers.any());
        verify(recalculationJobPublisher).publish(11L, WebhookEventSource.GITHUB);
    }

    @Test
    void rejectsRequestWhenSignatureIsInvalid() {
        when(gitHubWebhookAuthService.isValidSignature("{\"action\":\"opened\"}", "sha256=bad")).thenReturn(false);

        assertThatThrownBy(() -> gitHubWebhookController.receive("delivery-1", "sha256=bad", "{\"action\":\"opened\"}"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rejectsRequestWhenDeliveryHeaderMissing() {
        assertThatThrownBy(() -> gitHubWebhookController.receive(null, "sha256=ok", "{\"action\":\"opened\"}"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void doesNotQueueDuplicateEvent() {
        when(gitHubWebhookAuthService.isValidSignature("{\"action\":\"opened\"}", "sha256=ok")).thenReturn(true);
        when(webhookEventStoreService.storeEvent(
                        org.mockito.ArgumentMatchers.eq(WebhookEventSource.GITHUB),
                        org.mockito.ArgumentMatchers.eq("delivery-1"),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(new WebhookStoreResult(null, true));

        ResponseEntity<java.util.Map<String, Object>> response = gitHubWebhookController.receive(
                "delivery-1",
                "sha256=ok",
                "{\"action\":\"opened\"}");

        assertThat(response.getBody()).containsEntry("duplicate", true);
        assertThat(response.getBody()).containsEntry("queued", false);
        verify(gitHubActivityPersistenceService, never()).persistFromWebhook(org.mockito.ArgumentMatchers.any());
        verify(recalculationJobPublisher, never()).publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
