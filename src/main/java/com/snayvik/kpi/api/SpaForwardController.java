package com.snayvik.kpi.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
        "/{path:^(?!api$|webhooks$|actuator$).*$}",
        "/{path:^(?!api$|webhooks$|actuator$).*$}/**/{subpath:[^.]*}"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
