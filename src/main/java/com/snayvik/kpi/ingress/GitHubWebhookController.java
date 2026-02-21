package com.snayvik.kpi.ingress;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/github")
public class GitHubWebhookController {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> receive(@RequestBody(required = false) Map<String, Object> payload) {
        return Map.of(
                "status", "accepted",
                "source", "github",
                "hasPayload", payload != null && !payload.isEmpty());
    }
}
