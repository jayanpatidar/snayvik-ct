package com.snayvik.kpi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationConnectionServiceTest {

    @Mock
    private IntegrationConnectionRepository integrationConnectionRepository;

    private IntegrationConnectionService integrationConnectionService;

    @BeforeEach
    void setUp() {
        IntegrationSecretCryptoService cryptoService =
                new IntegrationSecretCryptoService("U25heXZpa0RlZmF1bHRJbnRlZ3JhdGlvbktleTEyMzQ1Njc4OTA=");
        integrationConnectionService = new IntegrationConnectionService(
                integrationConnectionRepository,
                cryptoService,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-02-21T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void upsertEncryptsSecretAndPersistsSettings() {
        when(integrationConnectionRepository.findBySystemName("SLACK")).thenReturn(Optional.empty());
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IntegrationConnectionService.IntegrationConnectionView view = integrationConnectionService.upsertConnection(
                IntegrationSystem.SLACK,
                new IntegrationConnectionService.IntegrationConnectionUpsertRequest(
                        true,
                        Map.of("channel", "#alerts"),
                        "slack-secret",
                        false),
                "admin-ui");

        assertThat(view.system()).isEqualTo("SLACK");
        assertThat(view.active()).isTrue();
        assertThat(view.settings()).containsEntry("channel", "#alerts");
        assertThat(view.hasSecret()).isTrue();

        ArgumentCaptor<IntegrationConnection> captor = ArgumentCaptor.forClass(IntegrationConnection.class);
        verify(integrationConnectionRepository).save(captor.capture());
        IntegrationConnection saved = captor.getValue();
        assertThat(saved.getSecretCiphertext()).isNotBlank();
        assertThat(saved.getSecretNonce()).isNotBlank();
    }

    @Test
    void testConnectionValidatesRequiredSettings() {
        IntegrationConnection connection = new IntegrationConnection();
        connection.setSystemName("EMAIL");
        connection.setSettingsJson("{\"smtpHost\":\"smtp.mail.local\"}");
        connection.setSecretCiphertext(null);
        connection.setSecretNonce(null);

        when(integrationConnectionRepository.findBySystemName("EMAIL")).thenReturn(Optional.of(connection));

        IntegrationConnectionService.IntegrationConnectionTestResult result =
                integrationConnectionService.testConnection(IntegrationSystem.EMAIL);

        assertThat(result.success()).isFalse();
        assertThat(result.missing()).contains("secret", "fromAddress");
    }
}
