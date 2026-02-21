package com.snayvik.kpi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class IntegrationOAuthServiceTest {

    @Mock
    private IntegrationConnectionService integrationConnectionService;

    @Mock
    private RestOperations restOperations;

    private IntegrationOAuthService integrationOAuthService;

    @BeforeEach
    void setUp() {
        IntegrationOAuthProperties properties = new IntegrationOAuthProperties();
        properties.getGithub().setEnabled(true);
        properties.getGithub().setClientId("github-client");
        properties.getGithub().setClientSecret("github-secret");
        properties.getGithub().setScopes("repo,read:org");

        integrationOAuthService = new IntegrationOAuthService(
                properties,
                integrationConnectionService,
                restOperations,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-02-21T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createAuthorizationUrlBuildsUrlAndStoresState() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        MockHttpSession session = new MockHttpSession();

        IntegrationOAuthService.OAuthAuthorizationUrlResponse response =
                integrationOAuthService.createAuthorizationUrl(IntegrationSystem.GITHUB, request, session);

        assertThat(response.authorizationUrl()).startsWith("https://github.com/login/oauth/authorize");

        var queryParams = UriComponentsBuilder.fromUriString(response.authorizationUrl()).build().getQueryParams();
        assertThat(queryParams.getFirst("client_id")).isEqualTo("github-client");
        assertThat(queryParams.getFirst("scope")).isEqualTo("repo%20read:org");
        assertThat(queryParams.getFirst("redirect_uri"))
                .isEqualTo("http://localhost:8080/api/kpi/admin/integrations/oauth/GITHUB/callback");
        assertThat(queryParams.getFirst("state")).isNotBlank();
        assertThat(session.getAttribute("integration.oauth.state.GITHUB")).isEqualTo(queryParams.getFirst("state"));
    }

    @Test
    void handleCallbackExchangesTokenAndUpdatesConnection() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        MockHttpSession session = new MockHttpSession();

        IntegrationOAuthService.OAuthAuthorizationUrlResponse response =
                integrationOAuthService.createAuthorizationUrl(IntegrationSystem.GITHUB, request, session);
        String state = UriComponentsBuilder.fromUriString(response.authorizationUrl())
                .build()
                .getQueryParams()
                .getFirst("state");

        when(restOperations.exchange(
                        any(URI.class),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"gho_test_token\"}"));

        IntegrationOAuthService.OAuthCallbackResult callbackResult = integrationOAuthService.handleCallback(
                IntegrationSystem.GITHUB,
                "oauth-code-123",
                state,
                null,
                null,
                "admin-ui",
                session);

        assertThat(callbackResult.success()).isTrue();
        assertThat(callbackResult.message()).isEqualTo("Connected via SSO");

        ArgumentCaptor<IntegrationConnectionService.IntegrationConnectionUpsertRequest> requestCaptor =
                ArgumentCaptor.forClass(IntegrationConnectionService.IntegrationConnectionUpsertRequest.class);
        verify(integrationConnectionService).upsertConnection(eq(IntegrationSystem.GITHUB), requestCaptor.capture(), eq("admin-ui"));
        assertThat(requestCaptor.getValue().active()).isTrue();
        assertThat(requestCaptor.getValue().secret()).isEqualTo("gho_test_token");
    }

    @Test
    void handleCallbackRejectsInvalidState() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("integration.oauth.state.GITHUB", "expected");
        session.setAttribute("integration.oauth.redirect.GITHUB", "http://localhost:8080/api/kpi/admin/integrations/oauth/GITHUB/callback");

        IntegrationOAuthService.OAuthCallbackResult callbackResult = integrationOAuthService.handleCallback(
                IntegrationSystem.GITHUB,
                "oauth-code-123",
                "wrong",
                null,
                null,
                "admin-ui",
                session);

        assertThat(callbackResult.success()).isFalse();
        assertThat(callbackResult.message()).contains("Invalid OAuth state");
        verify(integrationConnectionService, never()).upsertConnection(any(), any(), any());
    }
}
