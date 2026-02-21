package com.snayvik.kpi.api;

import com.snayvik.kpi.integration.BoardMappingAdminService;
import com.snayvik.kpi.integration.IntegrationConnectionService;
import com.snayvik.kpi.integration.IntegrationOAuthService;
import com.snayvik.kpi.integration.IntegrationSystem;
import com.snayvik.kpi.integration.RepoMappingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/kpi/admin/integrations")
public class IntegrationAdminController {

    private final IntegrationConnectionService integrationConnectionService;
    private final IntegrationOAuthService integrationOAuthService;
    private final RepoMappingService repoMappingService;
    private final BoardMappingAdminService boardMappingAdminService;

    public IntegrationAdminController(
            IntegrationConnectionService integrationConnectionService,
            IntegrationOAuthService integrationOAuthService,
            RepoMappingService repoMappingService,
            BoardMappingAdminService boardMappingAdminService) {
        this.integrationConnectionService = integrationConnectionService;
        this.integrationOAuthService = integrationOAuthService;
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

    @GetMapping("/oauth/{system}/authorize-url")
    public IntegrationOAuthService.OAuthAuthorizationUrlResponse authorizeUrl(
            @PathVariable IntegrationSystem system,
            HttpServletRequest request,
            HttpSession session) {
        try {
            return integrationOAuthService.createAuthorizationUrl(system, request, session);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/oauth/{system}/callback")
    public void oauthCallback(
            @PathVariable IntegrationSystem system,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @RequestParam(name = "errorDescription", required = false) String legacyErrorDescription,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String actor = authentication == null ? "oauth-callback" : authentication.getName();
        String callbackErrorDescription = (errorDescription == null || errorDescription.isBlank())
                ? legacyErrorDescription
                : errorDescription;
        IntegrationOAuthService.OAuthCallbackResult result = integrationOAuthService.handleCallback(
                system,
                code,
                state,
                error,
                callbackErrorDescription,
                actor,
                request.getSession(false));
        response.sendRedirect(buildUiRedirect(system, result));
    }

    private String buildUiRedirect(
            IntegrationSystem system,
            IntegrationOAuthService.OAuthCallbackResult result) {
        String message = result.message() == null ? "" : result.message();
        return "/admin/integrations?oauthSystem="
                + system.name()
                + "&oauthStatus="
                + (result.success() ? "connected" : "failed")
                + "&oauthMessage="
                + URLEncoder.encode(message, StandardCharsets.UTF_8);
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
