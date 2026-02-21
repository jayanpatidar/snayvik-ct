package com.snayvik.kpi.ingress.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MondayDedupeKeyServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MondayDedupeKeyService mondayDedupeKeyService;

    @BeforeEach
    void setUp() {
        mondayDedupeKeyService = new MondayDedupeKeyService();
    }

    @Test
    void usesEventIdWhenPresent() throws Exception {
        String payload = """
                {"event":{"id":"evt-123","triggerUuid":"trig-9"}}
                """;

        String dedupeKey = mondayDedupeKeyService.resolveDedupeKey(objectMapper.readTree(payload), payload);

        assertThat(dedupeKey).isEqualTo("evt-123");
    }

    @Test
    void usesTriggerUuidWhenEventIdMissing() throws Exception {
        String payload = """
                {"event":{"triggerUuid":"trig-9"}}
                """;

        String dedupeKey = mondayDedupeKeyService.resolveDedupeKey(objectMapper.readTree(payload), payload);

        assertThat(dedupeKey).isEqualTo("trig-9");
    }

    @Test
    void fallsBackToStableHashWhenNoStableIdentifierExists() throws Exception {
        String payload = """
                {"event":{"type":"change_column_value"}}
                """;

        String first = mondayDedupeKeyService.resolveDedupeKey(objectMapper.readTree(payload), payload);
        String second = mondayDedupeKeyService.resolveDedupeKey(objectMapper.readTree(payload), payload);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }
}
