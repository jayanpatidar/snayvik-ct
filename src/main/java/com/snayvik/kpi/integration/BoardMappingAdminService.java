package com.snayvik.kpi.integration;

import com.snayvik.kpi.ingress.persistence.BoardMapping;
import com.snayvik.kpi.ingress.persistence.BoardMappingRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardMappingAdminService {

    private final BoardMappingRepository boardMappingRepository;

    public BoardMappingAdminService(BoardMappingRepository boardMappingRepository) {
        this.boardMappingRepository = boardMappingRepository;
    }

    @Transactional(readOnly = true)
    public List<BoardMappingView> listMappings() {
        return boardMappingRepository.findAll().stream()
                .map(mapping -> new BoardMappingView(
                        mapping.getPrefix(),
                        mapping.getBoardId(),
                        mapping.getBoardName()))
                .toList();
    }

    @Transactional
    public List<BoardMappingView> replaceMappings(List<BoardMappingUpsertRequest> requests) {
        boardMappingRepository.deleteAllInBatch();
        if (requests != null) {
            for (BoardMappingUpsertRequest request : requests) {
                if (request == null
                        || request.prefix() == null
                        || request.prefix().isBlank()
                        || request.boardId() == null
                        || request.boardId().isBlank()) {
                    continue;
                }
                BoardMapping mapping = new BoardMapping();
                mapping.setPrefix(request.prefix().trim().toUpperCase());
                mapping.setBoardId(request.boardId().trim());
                mapping.setBoardName(request.boardName() == null ? "" : request.boardName().trim());
                boardMappingRepository.save(mapping);
            }
        }
        return listMappings();
    }

    public record BoardMappingView(
            String prefix,
            String boardId,
            String boardName) {
    }

    public record BoardMappingUpsertRequest(
            String prefix,
            String boardId,
            String boardName) {
    }
}
