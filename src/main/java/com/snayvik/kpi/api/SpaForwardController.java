package com.snayvik.kpi.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SpaForwardController {

    @GetMapping({"/", "/{*path}"})
    public String forwardToIndex(@PathVariable(required = false) String path) {
        if (path != null
                && (path.startsWith("api/")
                        || path.startsWith("webhooks/")
                        || path.startsWith("actuator/")
                        || path.contains("."))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}
