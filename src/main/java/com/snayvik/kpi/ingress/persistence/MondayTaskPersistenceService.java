package com.snayvik.kpi.ingress.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MondayTaskPersistenceService {

    private final BoardMappingRepository boardMappingRepository;
    private final TaskRepository taskRepository;

    public MondayTaskPersistenceService(BoardMappingRepository boardMappingRepository, TaskRepository taskRepository) {
        this.boardMappingRepository = boardMappingRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void persistFromWebhook(JsonNode payload) {
        String boardId = resolveBoardId(payload);
        String pulseId = resolvePulseId(payload);
        if (boardId.isBlank() || pulseId.isBlank()) {
            return;
        }

        boardMappingRepository.findByBoardId(boardId).ifPresent(mapping -> {
            String taskKey = mapping.getPrefix() + "-" + pulseId;
            Task task = taskRepository.findById(taskKey).orElseGet(Task::new);
            task.setTaskKey(taskKey);
            task.setPrefix(mapping.getPrefix());
            task.setPulseId(pulseId);
            task.setBoardId(boardId);

            String status = resolveStatus(payload);
            if (!status.isBlank()) {
                task.setStatus(status);
            }

            taskRepository.save(task);
        });
    }

    private String resolveBoardId(JsonNode payload) {
        String eventBoardId = payload.at("/event/boardId").asText("");
        if (!eventBoardId.isBlank()) {
            return eventBoardId;
        }
        return payload.at("/event/board_id").asText("");
    }

    private String resolvePulseId(JsonNode payload) {
        String pulseId = payload.at("/event/pulseId").asText("");
        if (!pulseId.isBlank()) {
            return pulseId;
        }
        return payload.at("/event/pulse_id").asText("");
    }

    private String resolveStatus(JsonNode payload) {
        String labelStatus = payload.at("/event/value/label/text").asText("");
        if (!labelStatus.isBlank()) {
            return labelStatus;
        }
        String textStatus = payload.at("/event/value/text").asText("");
        if (!textStatus.isBlank()) {
            return textStatus;
        }
        return payload.at("/event/status").asText("");
    }
}
