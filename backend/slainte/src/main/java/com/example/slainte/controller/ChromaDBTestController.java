package com.example.slainte.controller;

import com.example.slainte.service.ChromaDBService;
import com.example.slainte.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/test")
public class ChromaDBTestController {

    private final ChromaDBService chromaDBService;
    private final EmbeddingService embeddingService;

    public ChromaDBTestController(ChromaDBService chromaDBService, EmbeddingService embeddingService) {
        this.chromaDBService = chromaDBService;
        this.embeddingService = embeddingService;
    }

    @GetMapping("/chromadb")
    public ResponseEntity<Map<String, Object>> testChromaDB() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Create a simple test embedding (512 dimensions of 0.1)
            List<Double> testEmbedding = new ArrayList<>();
            for (int i = 0; i < 512; i++) {
                testEmbedding.add(0.1);
            }
            
            // Query ChromaDB with the test embedding
            String chromaResponse = chromaDBService.queryDatabase(testEmbedding, 3);
            
            // Add results to response
            result.put("success", true);
            result.put("message", "ChromaDB test completed");
            result.put("response", chromaResponse);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> testQuery(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String query = request.get("query");
            if (query == null) {
                result.put("success", false);
                result.put("error", "Query is required");
                return ResponseEntity.badRequest().body(result);
            }
            
            // Generate embedding for the query
            List<Double> embedding = embeddingService.getEmbedding(query);
            
            // Query ChromaDB with the embedding
            String chromaResponse = chromaDBService.queryDatabase(embedding, 3);
            
            // Add results to response
            result.put("success", true);
            result.put("query", query);
            result.put("embeddingSize", embedding.size());
            result.put("response", chromaResponse);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}