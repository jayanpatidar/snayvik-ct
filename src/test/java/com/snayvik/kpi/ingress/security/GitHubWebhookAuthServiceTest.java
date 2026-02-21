package com.snayvik.kpi.ingress.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class GitHubWebhookAuthServiceTest {

    @Test
    void validatesMatchingSha256Signature() throws Exception {
        GitHubWebhookAuthService authService = new GitHubWebhookAuthService("secret-value");
        String payload = "{\"action\":\"opened\"}";
        String signature = "sha256=" + hmacSha256Hex("secret-value", payload);

        boolean valid = authService.isValidSignature(payload, signature);

        assertThat(valid).isTrue();
    }

    @Test
    void rejectsInvalidSignature() {
        GitHubWebhookAuthService authService = new GitHubWebhookAuthService("secret-value");

        boolean valid = authService.isValidSignature("{\"action\":\"opened\"}", "sha256=invalid");

        assertThat(valid).isFalse();
    }

    private static String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
