package com.snayvik.kpi.ingress.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "board_mappings")
public class BoardMapping {

    @Id
    @Column(nullable = false, length = 16)
    private String prefix;

    @Column(name = "board_id", nullable = false, length = 64)
    private String boardId;

    @Column(name = "board_name", nullable = false, length = 255)
    private String boardName;

    public String getPrefix() {
        return prefix;
    }

    public String getBoardId() {
        return boardId;
    }

    public String getBoardName() {
        return boardName;
    }
}
