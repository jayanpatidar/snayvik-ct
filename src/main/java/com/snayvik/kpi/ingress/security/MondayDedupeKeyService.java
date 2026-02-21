package com.snayvik.kpi.ingress.security;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class MondayDedupeKeyService {

    public String resolveDedupeKey(JsonNode payload, String rawPayload) {
        String eventId = findText(payload, "/event/id");
        if (!eventId.isBlank()) {
            return eventId;
        }

        String triggerUuid = findText(payload, "/event/triggerUuid");
        if (!triggerUuid.isBlank()) {
            return triggerUuid;
        }

        String subscriptionId = findText(payload, "/subscriptionId");
        if (!subscriptionId.isBlank()) {
            return subscriptionId + ":" + hashPayload(rawPayload);
        }

        return hashPayload(rawPayload);
    }

    private String findText(JsonNode payload, String pointer) {
        JsonNode node = payload.at(pointer);
        if (node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private String hashPayload(String rawPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((rawPayload == null ? "" : rawPayload).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash monday payload for dedupe key", exception);
        }
    }
}
