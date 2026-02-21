package com.snayvik.kpi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snayvik.kpi.ingress.persistence.BoardMapping;
import com.snayvik.kpi.ingress.persistence.BoardMappingRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardMappingAdminServiceTest {

    @Mock
    private BoardMappingRepository boardMappingRepository;

    private BoardMappingAdminService boardMappingAdminService;

    @BeforeEach
    void setUp() {
        boardMappingAdminService = new BoardMappingAdminService(boardMappingRepository);
    }

    @Test
    void replaceMappingsUppercasesPrefix() {
        when(boardMappingRepository.save(any(BoardMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(boardMappingRepository.findAll()).thenReturn(List.of());

        boardMappingAdminService.replaceMappings(
                List.of(new BoardMappingAdminService.BoardMappingUpsertRequest("ab", "1001", "Board A")));

        verify(boardMappingRepository).deleteAllInBatch();
        ArgumentCaptor<BoardMapping> captor = ArgumentCaptor.forClass(BoardMapping.class);
        verify(boardMappingRepository).save(captor.capture());
        assertThat(captor.getValue().getPrefix()).isEqualTo("AB");
        assertThat(captor.getValue().getBoardId()).isEqualTo("1001");
    }
}
