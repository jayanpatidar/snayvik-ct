package com.snayvik.kpi.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "integration_connections")
public class IntegrationConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_name", nullable = false, unique = true, length = 32)
    private String systemName;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "settings_json", nullable = false, columnDefinition = "text")
    private String settingsJson = "{}";

    @Column(name = "secret_ciphertext", columnDefinition = "text")
    private String secretCiphertext;

    @Column(name = "secret_nonce", length = 128)
    private String secretNonce;

    @Column(name = "secret_updated_at")
    private Instant secretUpdatedAt;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy = "system";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }

    public String getSecretCiphertext() {
        return secretCiphertext;
    }

    public void setSecretCiphertext(String secretCiphertext) {
        this.secretCiphertext = secretCiphertext;
    }

    public String getSecretNonce() {
        return secretNonce;
    }

    public void setSecretNonce(String secretNonce) {
        this.secretNonce = secretNonce;
    }

    public Instant getSecretUpdatedAt() {
        return secretUpdatedAt;
    }

    public void setSecretUpdatedAt(Instant secretUpdatedAt) {
        this.secretUpdatedAt = secretUpdatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
