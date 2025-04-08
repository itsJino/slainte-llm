package com.example.slainte.controller;

import com.example.slainte.model.SearchRequest;
import com.example.slainte.model.SearchResponse;
import com.example.slainte.service.KnowledgeBaseService;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * Basic search endpoint that returns text results
     */
    @PostMapping("/search")
    public String search(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        return knowledgeBaseService.search(query);
    }
    
    /**
     * Enhanced search endpoint with custom topK parameter
     */
    @PostMapping("/search/advanced")
    public String advancedSearch(@RequestBody SearchRequest request) {
        return knowledgeBaseService.search(request.getQuery(), request.getTopK());
    }
    
    /**
     * Structured search results as JSON
     */
    @PostMapping("/search/structured")
    public ResponseEntity<SearchResponse> structuredSearch(@RequestBody SearchRequest request) {
        SearchResponse response = knowledgeBaseService.getSearchResults(
            request.getQuery(), 
            request.getTopK() > 0 ? request.getTopK() : 3
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get last context information
     */
    @GetMapping("/search/last-context")
    public ResponseEntity<Map<String, Object>> getLastContextInfo() {
        return ResponseEntity.ok(knowledgeBaseService.getLastContextInfo());
    }
    
    /**
     * Get the raw results from the last search
     */
    @GetMapping("/search/raw-results")
    public ResponseEntity<Map<String, Object>> getRawResults() {
        return ResponseEntity.ok(knowledgeBaseService.getLastRawResults());
    }
}