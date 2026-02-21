package com.snayvik.kpi.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.integrations.oauth")
public class IntegrationOAuthProperties {

    private ProviderProperties github = new ProviderProperties(
            "https://github.com/login/oauth/authorize",
            "https://github.com/login/oauth/access_token",
            "repo,read:org");

    private ProviderProperties monday = new ProviderProperties(
            "https://auth.monday.com/oauth2/authorize",
            "https://auth.monday.com/oauth2/token",
            "");

    private ProviderProperties slack = new ProviderProperties(
            "https://slack.com/oauth/v2/authorize",
            "https://slack.com/api/oauth.v2.access",
            "");

    public ProviderProperties getGithub() {
        return github;
    }

    public void setGithub(ProviderProperties github) {
        this.github = github;
    }

    public ProviderProperties getMonday() {
        return monday;
    }

    public void setMonday(ProviderProperties monday) {
        this.monday = monday;
    }

    public ProviderProperties getSlack() {
        return slack;
    }

    public void setSlack(ProviderProperties slack) {
        this.slack = slack;
    }

    public ProviderProperties providerFor(IntegrationSystem system) {
        return switch (system) {
            case GITHUB -> github;
            case MONDAY -> monday;
            case SLACK -> slack;
            case EMAIL -> null;
        };
    }

    public static class ProviderProperties {
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String authorizationUri = "";
        private String tokenUri = "";
        private String scopes = "";
        private String redirectUri = "";

        public ProviderProperties() {
        }

        public ProviderProperties(String authorizationUri, String tokenUri, String scopes) {
            this.authorizationUri = authorizationUri;
            this.tokenUri = tokenUri;
            this.scopes = scopes;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}
