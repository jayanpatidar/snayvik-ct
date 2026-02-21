package com.snayvik.kpi.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationConnectionService {

    private static final TypeReference<Map<String, String>> SETTINGS_TYPE = new TypeReference<>() {};

    private final IntegrationConnectionRepository integrationConnectionRepository;
    private final IntegrationSecretCryptoService integrationSecretCryptoService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IntegrationConnectionService(
            IntegrationConnectionRepository integrationConnectionRepository,
            IntegrationSecretCryptoService integrationSecretCryptoService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.integrationConnectionRepository = integrationConnectionRepository;
        this.integrationSecretCryptoService = integrationSecretCryptoService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<IntegrationConnectionView> listConnections() {
        return integrationConnectionRepository.findAllByOrderBySystemNameAsc().stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public IntegrationConnectionView getConnection(IntegrationSystem system) {
        IntegrationConnection connection = integrationConnectionRepository
                .findBySystemName(system.name())
                .orElseGet(() -> emptyConnection(system));
        return toView(connection);
    }

    @Transactional
    public IntegrationConnectionView upsertConnection(
            IntegrationSystem system,
            IntegrationConnectionUpsertRequest request,
            String actor) {
        IntegrationConnection connection = integrationConnectionRepository
                .findBySystemName(system.name())
                .orElseGet(() -> emptyConnection(system));

        if (request.active() != null) {
            connection.setActive(request.active());
        }
        if (request.settings() != null) {
            connection.setSettingsJson(writeSettings(request.settings()));
        }
        if (request.clearSecret() != null && request.clearSecret()) {
            connection.setSecretCiphertext(null);
            connection.setSecretNonce(null);
            connection.setSecretUpdatedAt(null);
        }
        if (request.secret() != null && !request.secret().isBlank()) {
            IntegrationSecretCryptoService.EncryptedSecret encrypted =
                    integrationSecretCryptoService.encrypt(request.secret());
            connection.setSecretCiphertext(encrypted.ciphertextBase64());
            connection.setSecretNonce(encrypted.nonceBase64());
            connection.setSecretUpdatedAt(Instant.now(clock));
        }

        connection.setUpdatedBy(normalizeActor(actor));
        connection.setUpdatedAt(Instant.now(clock));
        IntegrationConnection saved = integrationConnectionRepository.save(connection);
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public IntegrationConnectionTestResult testConnection(IntegrationSystem system) {
        IntegrationConnection connection = integrationConnectionRepository
                .findBySystemName(system.name())
                .orElseGet(() -> emptyConnection(system));

        Map<String, String> settings = readSettings(connection.getSettingsJson());
        boolean hasSecret = connection.getSecretCiphertext() != null
                && connection.getSecretNonce() != null;

        List<String> missing = switch (system) {
            case GITHUB -> collectMissing(hasSecret, settings, "org");
            case MONDAY -> collectMissing(hasSecret, settings);
            case SLACK -> collectMissing(hasSecret, settings, "channel");
            case EMAIL -> collectMissing(hasSecret, settings, "smtpHost", "fromAddress");
        };
        if (!missing.isEmpty()) {
            return new IntegrationConnectionTestResult(false, "Missing required config", missing);
        }
        return new IntegrationConnectionTestResult(true, "Configuration looks valid", List.of());
    }

    @Transactional(readOnly = true)
    public String loadDecryptedSecret(IntegrationSystem system) {
        IntegrationConnection connection = integrationConnectionRepository
                .findBySystemName(system.name())
                .orElse(null);
        if (connection == null) {
            return null;
        }
        return integrationSecretCryptoService.decrypt(connection.getSecretCiphertext(), connection.getSecretNonce());
    }

    private List<String> collectMissing(boolean hasSecret, Map<String, String> settings, String... requiredKeys) {
        List<String> missing = new java.util.ArrayList<>();
        if (!hasSecret) {
            missing.add("secret");
        }
        for (String key : requiredKeys) {
            String value = settings.get(key);
            if (value == null || value.isBlank()) {
                missing.add(key);
            }
        }
        return missing;
    }

    private IntegrationConnection emptyConnection(IntegrationSystem system) {
        IntegrationConnection connection = new IntegrationConnection();
        connection.setSystemName(system.name());
        connection.setSettingsJson("{}");
        connection.setUpdatedAt(Instant.now(clock));
        connection.setUpdatedBy("system");
        return connection;
    }

    private IntegrationConnectionView toView(IntegrationConnection connection) {
        return new IntegrationConnectionView(
                connection.getSystemName(),
                connection.isActive(),
                readSettings(connection.getSettingsJson()),
                connection.getSecretCiphertext() != null && connection.getSecretNonce() != null,
                connection.getSecretUpdatedAt(),
                connection.getUpdatedBy(),
                connection.getUpdatedAt());
    }

    private String writeSettings(Map<String, String> settings) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            normalized.put(entry.getKey().trim(), value);
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize integration settings", exception);
        }
    }

    private Map<String, String> readSettings(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(settingsJson, SETTINGS_TYPE);
            if (parsed == null) {
                return Map.of();
            }
            return parsed;
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String normalizeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "admin-ui";
        }
        return actor.trim();
    }

    public record IntegrationConnectionView(
            String system,
            boolean active,
            Map<String, String> settings,
            boolean hasSecret,
            Instant secretUpdatedAt,
            String updatedBy,
            Instant updatedAt) {
    }

    public record IntegrationConnectionUpsertRequest(
            Boolean active,
            Map<String, String> settings,
            String secret,
            Boolean clearSecret) {
    }

    public record IntegrationConnectionTestResult(
            boolean success,
            String message,
            List<String> missing) {
    }
}
