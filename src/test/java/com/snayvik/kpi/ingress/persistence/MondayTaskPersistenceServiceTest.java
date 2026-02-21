package com.snayvik.kpi.ingress.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snayvik.kpi.sync.MondayTaskSnapshot;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MondayTaskPersistenceServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BoardMappingRepository boardMappingRepository;

    @Mock
    private TaskRepository taskRepository;

    private MondayTaskPersistenceService mondayTaskPersistenceService;

    @BeforeEach
    void setUp() {
        mondayTaskPersistenceService = new MondayTaskPersistenceService(boardMappingRepository, taskRepository);
    }

    @Test
    void upsertsTaskWhenBoardMappingExists() throws Exception {
        String payload = """
                {
                  "event": {
                    "boardId": "9988",
                    "pulseId": "abc123",
                    "value": {"label": {"text": "Done"}}
                  }
                }
                """;

        BoardMapping mapping = org.mockito.Mockito.mock(BoardMapping.class);
        when(mapping.getPrefix()).thenReturn("JWV");
        when(boardMappingRepository.findByBoardId("9988")).thenReturn(Optional.of(mapping));
        when(taskRepository.findById("JWV-abc123")).thenReturn(Optional.empty());

        mondayTaskPersistenceService.persistFromWebhook(objectMapper.readTree(payload));

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskKey()).isEqualTo("JWV-abc123");
        assertThat(taskCaptor.getValue().getPrefix()).isEqualTo("JWV");
        assertThat(taskCaptor.getValue().getBoardId()).isEqualTo("9988");
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo("Done");
    }

    @Test
    void skipsPersistenceWhenBoardMappingMissing() throws Exception {
        String payload = """
                {
                  "event": {"boardId": "no-map", "pulseId": "abc123"}
                }
                """;
        when(boardMappingRepository.findByBoardId("no-map")).thenReturn(Optional.empty());

        mondayTaskPersistenceService.persistFromWebhook(objectMapper.readTree(payload));

        verify(taskRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void upsertsTaskFromSnapshot() {
        BoardMapping mapping = org.mockito.Mockito.mock(BoardMapping.class);
        when(mapping.getPrefix()).thenReturn("JWV");
        when(mapping.getBoardId()).thenReturn("9988");
        when(taskRepository.findById("JWV-abc123")).thenReturn(Optional.empty());

        MondayTaskSnapshot snapshot = new MondayTaskSnapshot(
                "abc123",
                "Done",
                Instant.parse("2026-02-20T08:00:00Z"),
                Instant.parse("2026-02-20T12:00:00Z"),
                Instant.parse("2026-02-20T11:00:00Z"));

        String taskKey = mondayTaskPersistenceService.upsertFromSnapshot(mapping, snapshot);

        assertThat(taskKey).isEqualTo("JWV-abc123");
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task task = taskCaptor.getValue();
        assertThat(task.getTaskKey()).isEqualTo("JWV-abc123");
        assertThat(task.getStatus()).isEqualTo("Done");
        assertThat(task.getStartedAt()).isEqualTo(Instant.parse("2026-02-20T08:00:00Z"));
        assertThat(task.getCompletedAt()).isEqualTo(Instant.parse("2026-02-20T12:00:00Z"));
    }
}
