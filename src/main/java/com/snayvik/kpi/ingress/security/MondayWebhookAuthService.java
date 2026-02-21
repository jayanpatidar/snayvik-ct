package com.snayvik.kpi.ingress.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MondayWebhookAuthService {

    private final String webhookToken;

    public MondayWebhookAuthService(@Value("${app.integrations.monday.webhook-token}") String webhookToken) {
        this.webhookToken = webhookToken;
    }

    public boolean isAuthorized(String authorizationHeader, String mondayTokenHeader) {
        String normalizedAuthorization = normalizeToken(authorizationHeader);
        String normalizedMondayToken = normalizeToken(mondayTokenHeader);
        return webhookToken.equals(normalizedAuthorization) || webhookToken.equals(normalizedMondayToken);
    }

    private String normalizeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        String token = rawToken.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token.substring(7).trim();
        }
        return token;
    }
}
