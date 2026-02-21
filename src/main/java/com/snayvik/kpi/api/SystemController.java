package com.snayvik.kpi.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/system")
public class SystemController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "service", "snayvik-ct",
                "status", "UP",
                "timestamp", Instant.now().toString());
    }
}
