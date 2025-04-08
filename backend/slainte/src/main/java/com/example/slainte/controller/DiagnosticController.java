package com.example.slainte.controller;

import com.example.slainte.service.ChromaDBDirectClient;
import com.example.slainte.service.ChromaTestService;
import com.example.slainte.service.KnowledgeBaseService;
import com.example.slainte.service.RawChromaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for diagnosing and testing ChromaDB connection issues
 */
@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChromaDBDirectClient chromaDBDirectClient;
    private final RawChromaService rawChromaService;
    private final ChromaTestService chromaTestService;
    
    public DiagnosticController(
            KnowledgeBaseService knowledgeBaseService, 
            ChromaDBDirectClient chromaDBDirectClient,
            RawChromaService rawChromaService,
            ChromaTestService chromaTestService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.chromaDBDirectClient = chromaDBDirectClient;
        this.rawChromaService = rawChromaService;
        this.chromaTestService = chromaTestService;
    }

    /**
     * Test the ChromaDB connection and return diagnostics
     */
    @GetMapping("/chroma-test")
    public ResponseEntity<Map<String, Object>> testChromaDB() {
        Map<String, Object> diagnostics = chromaDBDirectClient.testConnection();
        return ResponseEntity.ok(diagnostics);
    }
    
    /**
     * Test a simple search operation
     */
    @PostMapping("/search-test")
    public ResponseEntity<Map<String, Object>> testSearch(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String query = request.getOrDefault("query", "test query");
            String result = knowledgeBaseService.search(query, 1);
            
            response.put("success", true);
            response.put("query", query);
            response.put("result", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get debug information about the last context retrieved
     */
    @GetMapping("/last-context")
    public ResponseEntity<Map<String, Object>> getLastContextInfo() {
        return ResponseEntity.ok(knowledgeBaseService.getLastContextInfo());
    }
    
    /**
     * Run a minimal test query to ChromaDB with new test service
     */
    @GetMapping("/minimal-test")
    public ResponseEntity<String> minimalTest() {
        return ResponseEntity.ok(chromaTestService.runMinimalTest());
    }
    
    /**
     * Test ChromaDB heartbeat
     */
    @GetMapping("/heartbeat")
    public ResponseEntity<String> heartbeatTest() {
        return ResponseEntity.ok(chromaTestService.testHeartbeat());
    }
    
    /**
     * Test with the most basic raw HTTP connection approach
     */
    @GetMapping("/raw-query")
    public ResponseEntity<String> rawQueryTest() {
        return ResponseEntity.ok(rawChromaService.testHardcodedQuery());
    }
}