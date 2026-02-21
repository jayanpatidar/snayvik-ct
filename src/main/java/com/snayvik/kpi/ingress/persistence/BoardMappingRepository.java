package com.snayvik.kpi.ingress.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardMappingRepository extends JpaRepository<BoardMapping, String> {

    Optional<BoardMapping> findByBoardId(String boardId);
}
