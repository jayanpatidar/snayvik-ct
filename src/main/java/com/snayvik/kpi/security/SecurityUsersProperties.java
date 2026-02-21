package com.snayvik.kpi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityUsersProperties(Credentials user, Credentials admin) {

    public record Credentials(String username, String password) {
    }
}
