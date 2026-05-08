package com.festapp.dashboard.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiDocsForwardController {

    @GetMapping("/api-docs")
    public String apiDocs() {
        return "forward:/v3/api-docs";
    }
}
