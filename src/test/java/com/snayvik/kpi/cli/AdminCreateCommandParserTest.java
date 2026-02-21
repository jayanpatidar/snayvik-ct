package com.snayvik.kpi.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminCreateCommandParserTest {

    @Test
    void parsesInlineOptions() {
        AdminCreateCommandParser.ParsedAdminCreateCommand parsed =
                AdminCreateCommandParser.parse(new String[] {
                        "--username=admin",
                        "--password=Secret@123",
                        "--spring.datasource.url=jdbc:postgresql://localhost:5432/snayvik_kpi"
                });

        assertThat(parsed.username()).isEqualTo("admin");
        assertThat(parsed.password()).isEqualTo("Secret@123");
        assertThat(parsed.springArguments()).containsExactly("--spring.datasource.url=jdbc:postgresql://localhost:5432/snayvik_kpi");
    }

    @Test
    void parsesSplitOptions() {
        AdminCreateCommandParser.ParsedAdminCreateCommand parsed =
                AdminCreateCommandParser.parse(new String[] {
                        "--username", "admin",
                        "--password", "Secret@123"
                });

        assertThat(parsed.username()).isEqualTo("admin");
        assertThat(parsed.password()).isEqualTo("Secret@123");
        assertThat(parsed.springArguments()).isEmpty();
    }

    @Test
    void rejectsMissingUsername() {
        assertThatThrownBy(() -> AdminCreateCommandParser.parse(new String[] {"--password", "Secret@123"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--username");
    }

    @Test
    void rejectsMissingPassword() {
        assertThatThrownBy(() -> AdminCreateCommandParser.parse(new String[] {"--username", "admin"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--password");
    }
}
