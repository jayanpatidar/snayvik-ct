package com.snayvik.kpi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskKeyExtractorTest {

    private TaskKeyExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new TaskKeyExtractor("([A-Z]{2,10}-[a-zA-Z0-9]+)");
    }

    @Test
    void extractsTaskKeyFromPullRequestTitleAndBranchAndCommitMessage() {
        var taskKeys = extractor.extractTaskKeys(List.of(
                "feat: JWV-1a23v align monday item",
                "feature/JWV-1a23v-enable-replay",
                "commit for JWV-1a23v"));

        assertThat(taskKeys).containsExactly("JWV-1a23v");
    }

    @Test
    void extractsMultipleTaskKeysWhenPresent() {
        var taskKeys = extractor.extractTaskKeys(List.of(
                "merge JWV-1a23v and OPS-99X",
                "branch/OPS-99X"));

        assertThat(taskKeys).containsExactly("JWV-1a23v", "OPS-99X");
    }

    @Test
    void ignoresNullBlankAndNonMatchingValues() {
        var taskKeys = extractor.extractTaskKeys(Arrays.asList(
                null,
                "",
                "missing-key-here",
                "lowercase abc-123"));

        assertThat(taskKeys).isEmpty();
    }
}
