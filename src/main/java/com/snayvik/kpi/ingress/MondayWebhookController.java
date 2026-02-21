package com.snayvik.kpi.ingress;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/monday")
public class MondayWebhookController {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> receive(@RequestBody(required = false) Map<String, Object> payload) {
        if (payload != null && payload.containsKey("challenge")) {
            return Map.of("challenge", payload.get("challenge"));
        }
        return Map.of(
                "status", "accepted",
                "source", "monday",
                "hasPayload", payload != null && !payload.isEmpty());
    }
}
