package com.snayvik.kpi.integration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IntegrationSecretCryptoService {

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_LENGTH = 12;

    private final SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public IntegrationSecretCryptoService(
            @Value("${app.security.integration-encryption-key:U25heXZpa0RlZmF1bHRJbnRlZ3JhdGlvbktleTEyMzQ1Njc4OTA=}")
            String configuredKey) {
        this.secretKeySpec = new SecretKeySpec(resolveKeyBytes(configuredKey), "AES");
    }

    public EncryptedSecret encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext must not be null");
        }
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptedSecret(
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(nonce));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encrypt integration secret", exception);
        }
    }

    public String decrypt(String ciphertextBase64, String nonceBase64) {
        if (ciphertextBase64 == null || nonceBase64 == null) {
            return null;
        }
        try {
            byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);
            byte[] nonce = Base64.getDecoder().decode(nonceBase64);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to decrypt integration secret", exception);
        }
    }

    private byte[] resolveKeyBytes(String configuredKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to deterministic hash-based key derivation.
        }
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to derive integration encryption key", exception);
        }
    }

    public record EncryptedSecret(String ciphertextBase64, String nonceBase64) {
    }
}
