package com.snayvik.kpi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepoMappingServiceTest {

    @Mock
    private RepoMappingRepository repoMappingRepository;

    private RepoMappingService repoMappingService;

    @BeforeEach
    void setUp() {
        repoMappingService = new RepoMappingService(
                repoMappingRepository,
                Clock.fixed(Instant.parse("2026-02-21T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void replaceMappingsNormalizesPrefixesAndPersists() {
        when(repoMappingRepository.save(any(RepoMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repoMappingRepository.findAllByOrderByRepositoryAsc()).thenReturn(List.of());

        repoMappingService.replaceMappings(
                List.of(new RepoMappingService.RepoMappingUpsertRequest(
                        "snayvik/core-api",
                        true,
                        List.of("ab", " cd "))),
                "admin-ui");

        verify(repoMappingRepository).deleteAllInBatch();
        ArgumentCaptor<RepoMapping> captor = ArgumentCaptor.forClass(RepoMapping.class);
        verify(repoMappingRepository).save(captor.capture());
        assertThat(captor.getValue().getRepository()).isEqualTo("snayvik/core-api");
        assertThat(captor.getValue().getAllowedPrefixes()).isEqualTo("AB,CD");
        assertThat(captor.getValue().isEnabled()).isTrue();
    }
}
