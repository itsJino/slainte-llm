package com.example.slainte.controller;

import com.example.slainte.service.KnowledgeBaseService;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping("/search")
    public String search(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        return knowledgeBaseService.search(query);
    }
}
