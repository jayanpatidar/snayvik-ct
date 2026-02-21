package com.snayvik.kpi.api;

import com.snayvik.kpi.integration.BoardMappingAdminService;
import com.snayvik.kpi.integration.IntegrationConnectionService;
import com.snayvik.kpi.integration.IntegrationSystem;
import com.snayvik.kpi.integration.RepoMappingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/admin/integrations")
public class IntegrationAdminController {

    private final IntegrationConnectionService integrationConnectionService;
    private final RepoMappingService repoMappingService;
    private final BoardMappingAdminService boardMappingAdminService;

    public IntegrationAdminController(
            IntegrationConnectionService integrationConnectionService,
            RepoMappingService repoMappingService,
            BoardMappingAdminService boardMappingAdminService) {
        this.integrationConnectionService = integrationConnectionService;
        this.repoMappingService = repoMappingService;
        this.boardMappingAdminService = boardMappingAdminService;
    }

    @GetMapping("/connections")
    public List<IntegrationConnectionService.IntegrationConnectionView> listConnections() {
        return integrationConnectionService.listConnections();
    }

    @GetMapping("/connections/{system}")
    public IntegrationConnectionService.IntegrationConnectionView getConnection(@PathVariable IntegrationSystem system) {
        return integrationConnectionService.getConnection(system);
    }

    @PutMapping("/connections/{system}")
    public IntegrationConnectionService.IntegrationConnectionView upsertConnection(
            @PathVariable IntegrationSystem system,
            @RequestBody IntegrationConnectionService.IntegrationConnectionUpsertRequest request,
            @RequestHeader(name = "X-Actor", required = false) String actor) {
        return integrationConnectionService.upsertConnection(system, request, actor);
    }

    @PostMapping("/connections/{system}/test")
    public IntegrationConnectionService.IntegrationConnectionTestResult testConnection(@PathVariable IntegrationSystem system) {
        return integrationConnectionService.testConnection(system);
    }

    @GetMapping("/repositories")
    public List<RepoMappingService.RepoMappingView> listRepositories() {
        return repoMappingService.listMappings();
    }

    @PutMapping("/repositories")
    public List<RepoMappingService.RepoMappingView> replaceRepositories(
            @RequestBody List<RepoMappingService.RepoMappingUpsertRequest> requests,
            @RequestHeader(name = "X-Actor", required = false) String actor) {
        return repoMappingService.replaceMappings(requests, actor);
    }

    @GetMapping("/boards")
    public List<BoardMappingAdminService.BoardMappingView> listBoards() {
        return boardMappingAdminService.listMappings();
    }

    @PutMapping("/boards")
    public List<BoardMappingAdminService.BoardMappingView> replaceBoards(
            @RequestBody List<BoardMappingAdminService.BoardMappingUpsertRequest> requests) {
        return boardMappingAdminService.replaceMappings(requests);
    }
}
