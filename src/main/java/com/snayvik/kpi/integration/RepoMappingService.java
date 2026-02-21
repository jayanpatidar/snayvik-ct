package com.snayvik.kpi.integration;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepoMappingService {

    private final RepoMappingRepository repoMappingRepository;
    private final Clock clock;

    public RepoMappingService(RepoMappingRepository repoMappingRepository, Clock clock) {
        this.repoMappingRepository = repoMappingRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RepoMappingView> listMappings() {
        return repoMappingRepository.findAllByOrderByRepositoryAsc().stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public List<RepoMappingView> replaceMappings(List<RepoMappingUpsertRequest> requests, String actor) {
        repoMappingRepository.deleteAllInBatch();
        Instant now = Instant.now(clock);
        String updatedBy = normalizeActor(actor);

        if (requests != null) {
            for (RepoMappingUpsertRequest request : requests) {
                if (request == null || request.repository() == null || request.repository().isBlank()) {
                    continue;
                }
                RepoMapping mapping = new RepoMapping();
                mapping.setRepository(request.repository().trim());
                mapping.setEnabled(request.enabled() == null || request.enabled());
                mapping.setAllowedPrefixes(toCsv(request.allowedPrefixes()));
                mapping.setUpdatedBy(updatedBy);
                mapping.setUpdatedAt(now);
                repoMappingRepository.save(mapping);
            }
        }
        return listMappings();
    }

    private RepoMappingView toView(RepoMapping mapping) {
        return new RepoMappingView(
                mapping.getRepository(),
                mapping.isEnabled(),
                fromCsv(mapping.getAllowedPrefixes()),
                mapping.getUpdatedBy(),
                mapping.getUpdatedAt());
    }

    private String normalizeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "admin-ui";
        }
        return actor.trim();
    }

    private String toCsv(List<String> allowedPrefixes) {
        if (allowedPrefixes == null || allowedPrefixes.isEmpty()) {
            return "";
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String prefix : allowedPrefixes) {
            if (prefix == null || prefix.isBlank()) {
                continue;
            }
            normalized.add(prefix.trim().toUpperCase());
        }
        return String.join(",", normalized);
    }

    private List<String> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public record RepoMappingView(
            String repository,
            boolean enabled,
            List<String> allowedPrefixes,
            String updatedBy,
            Instant updatedAt) {
    }

    public record RepoMappingUpsertRequest(
            String repository,
            Boolean enabled,
            List<String> allowedPrefixes) {
    }
}
