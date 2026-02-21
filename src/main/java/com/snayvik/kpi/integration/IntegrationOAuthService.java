package com.snayvik.kpi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class IntegrationOAuthService {

    private static final String SESSION_STATE_PREFIX = "integration.oauth.state.";
    private static final String SESSION_REDIRECT_PREFIX = "integration.oauth.redirect.";

    private final IntegrationOAuthProperties oauthProperties;
    private final IntegrationConnectionService integrationConnectionService;
    private final RestOperations restOperations;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public IntegrationOAuthService(
            IntegrationOAuthProperties oauthProperties,
            IntegrationConnectionService integrationConnectionService,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            Clock clock) {
        this(
                oauthProperties,
                integrationConnectionService,
                restTemplateBuilder.build(),
                objectMapper,
                clock);
    }

    IntegrationOAuthService(
            IntegrationOAuthProperties oauthProperties,
            IntegrationConnectionService integrationConnectionService,
            RestOperations restOperations,
            ObjectMapper objectMapper,
            Clock clock) {
        this.oauthProperties = oauthProperties;
        this.integrationConnectionService = integrationConnectionService;
        this.restOperations = restOperations;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public OAuthAuthorizationUrlResponse createAuthorizationUrl(
            IntegrationSystem system,
            HttpServletRequest request,
            HttpSession session) {
        IntegrationOAuthProperties.ProviderProperties provider = validateProvider(system);
        String redirectUri = resolveRedirectUri(provider, request, system);
        String state = generateState(system);

        session.setAttribute(stateKey(system), state);
        session.setAttribute(redirectUriKey(system), redirectUri);
        session.setAttribute(stateCreatedAtKey(system), Instant.now(clock).toString());

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", provider.getClientId().trim())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state);

        String scopes = normalizeScopes(provider.getScopes());
        if (!scopes.isBlank()) {
            builder.queryParam("scope", scopes);
        }

        return new OAuthAuthorizationUrlResponse(builder.encode(StandardCharsets.UTF_8).build().toUriString());
    }

    public OAuthCallbackResult handleCallback(
            IntegrationSystem system,
            String code,
            String state,
            String error,
            String errorDescription,
            String actor,
            HttpSession session) {
        if (isUnsupportedForOAuth(system)) {
            return OAuthCallbackResult.failed("SSO is not supported for " + system.name());
        }
        if (session == null) {
            return OAuthCallbackResult.failed("Your session expired. Start SSO connect again.");
        }

        IntegrationOAuthProperties.ProviderProperties provider;
        try {
            provider = validateProvider(system);
        } catch (IllegalArgumentException exception) {
            return OAuthCallbackResult.failed(exception.getMessage());
        }

        String expectedState = asString(session.getAttribute(stateKey(system)));
        String redirectUri = asString(session.getAttribute(redirectUriKey(system)));
        clearSessionOAuthState(system, session);

        if (expectedState.isBlank() || state == null || !Objects.equals(expectedState, state)) {
            return OAuthCallbackResult.failed("Invalid OAuth state. Start SSO connect again.");
        }
        if (error != null && !error.isBlank()) {
            String providerMessage = errorDescription == null || errorDescription.isBlank()
                    ? error
                    : error + ": " + errorDescription;
            return OAuthCallbackResult.failed("OAuth provider returned an error: " + providerMessage);
        }
        if (code == null || code.isBlank()) {
            return OAuthCallbackResult.failed("Missing OAuth authorization code");
        }
        if (redirectUri.isBlank()) {
            return OAuthCallbackResult.failed("Missing OAuth redirect context. Start SSO connect again.");
        }

        try {
            String tokenResponse = exchangeCodeForAccessToken(provider, code.trim(), redirectUri);
            String accessToken = extractAccessToken(tokenResponse);
            if (accessToken.isBlank()) {
                return OAuthCallbackResult.failed("Token exchange succeeded but no access_token was returned");
            }

            integrationConnectionService.upsertConnection(
                    system,
                    new IntegrationConnectionService.IntegrationConnectionUpsertRequest(
                            true,
                            null,
                            accessToken,
                            false),
                    actor);

            return OAuthCallbackResult.success("Connected via SSO");
        } catch (RestClientException clientException) {
            return OAuthCallbackResult.failed("Token exchange failed: " + clientException.getMessage());
        } catch (Exception exception) {
            return OAuthCallbackResult.failed("SSO callback failed: " + exception.getMessage());
        }
    }

    private String exchangeCodeForAccessToken(
            IntegrationOAuthProperties.ProviderProperties provider,
            String code,
            String redirectUri) {
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("grant_type", "authorization_code");
        payload.add("code", code);
        payload.add("redirect_uri", redirectUri);
        payload.add("client_id", provider.getClientId().trim());
        payload.add("client_secret", provider.getClientSecret().trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("Accept", "application/json");

        ResponseEntity<String> response = restOperations.exchange(
                URI.create(provider.getTokenUri().trim()),
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);

        return response.getBody() == null ? "" : response.getBody();
    }

    private String extractAccessToken(String tokenResponse) throws Exception {
        if (tokenResponse == null || tokenResponse.isBlank()) {
            return "";
        }
        String normalized = tokenResponse.trim();
        if (normalized.startsWith("{")) {
            JsonNode jsonNode = objectMapper.readTree(normalized);
            JsonNode accessTokenNode = jsonNode.get("access_token");
            return accessTokenNode == null ? "" : accessTokenNode.asText("");
        }
        Map<String, String> formValues = Arrays.stream(normalized.split("&"))
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> decode(parts[0]),
                        parts -> decode(parts[1]),
                        (left, right) -> right,
                        LinkedHashMap::new));
        return formValues.getOrDefault("access_token", "");
    }

    private String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private IntegrationOAuthProperties.ProviderProperties validateProvider(IntegrationSystem system) {
        if (isUnsupportedForOAuth(system)) {
            throw new IllegalArgumentException("SSO is not supported for " + system.name());
        }
        IntegrationOAuthProperties.ProviderProperties provider = oauthProperties.providerFor(system);
        if (provider == null) {
            throw new IllegalArgumentException("No OAuth configuration found for " + system.name());
        }
        if (!provider.isEnabled()) {
            throw new IllegalArgumentException(system.name() + " SSO is disabled");
        }
        if (isBlank(provider.getClientId())
                || isBlank(provider.getClientSecret())
                || isBlank(provider.getAuthorizationUri())
                || isBlank(provider.getTokenUri())) {
            throw new IllegalArgumentException(system.name() + " SSO is missing client or endpoint configuration");
        }
        return provider;
    }

    private boolean isUnsupportedForOAuth(IntegrationSystem system) {
        return system == IntegrationSystem.EMAIL;
    }

    private String resolveRedirectUri(
            IntegrationOAuthProperties.ProviderProperties provider,
            HttpServletRequest request,
            IntegrationSystem system) {
        if (!isBlank(provider.getRedirectUri())) {
            return provider.getRedirectUri().trim();
        }
        return UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getServerName())
                .port(request.getServerPort())
                .path(request.getContextPath())
                .path("/api/kpi/admin/integrations/oauth/")
                .path(system.name())
                .path("/callback")
                .build()
                .toUriString();
    }

    private String normalizeScopes(String rawScopes) {
        if (rawScopes == null || rawScopes.isBlank()) {
            return "";
        }
        return Arrays.stream(rawScopes.split("[,\\s]+"))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .distinct()
                .collect(Collectors.joining(" "));
    }

    private String generateState(IntegrationSystem system) {
        byte[] bytes = (system.name() + ":" + Instant.now(clock) + ":" + java.util.UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String stateKey(IntegrationSystem system) {
        return SESSION_STATE_PREFIX + system.name();
    }

    private String redirectUriKey(IntegrationSystem system) {
        return SESSION_REDIRECT_PREFIX + system.name();
    }

    private String stateCreatedAtKey(IntegrationSystem system) {
        return SESSION_STATE_PREFIX + system.name() + ".createdAt";
    }

    private void clearSessionOAuthState(IntegrationSystem system, HttpSession session) {
        session.removeAttribute(stateKey(system));
        session.removeAttribute(redirectUriKey(system));
        session.removeAttribute(stateCreatedAtKey(system));
    }

    private String asString(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record OAuthAuthorizationUrlResponse(String authorizationUrl) {
    }

    public record OAuthCallbackResult(boolean success, String message) {
        static OAuthCallbackResult success(String message) {
            return new OAuthCallbackResult(true, message);
        }

        static OAuthCallbackResult failed(String message) {
            return new OAuthCallbackResult(false, message);
        }
    }
}
