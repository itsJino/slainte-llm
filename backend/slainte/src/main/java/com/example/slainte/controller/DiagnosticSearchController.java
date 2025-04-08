package com.example.slainte.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.slainte.service.EmbeddingService;
import com.example.slainte.service.ChromaDBLowLevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A controller for diagnosing and testing the search functionality
 */
@RestController
@RequestMapping("/api/debug")
public class DiagnosticSearchController {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticSearchController.class);
    
    private final EmbeddingService embeddingService;
    private final ChromaDBLowLevelService chromaDBService;
    
    public DiagnosticSearchController(
            EmbeddingService embeddingService,
            ChromaDBLowLevelService chromaDBService) {
        this.embeddingService = embeddingService;
        this.chromaDBService = chromaDBService;
    }
    
    /**
     * Endpoint to manually test search with full visibility into the process
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> debugSearch(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String query = request.get("query");
            int topK = 5; // Default to 5 results
            
            // Try to parse topK if provided
            if (request.containsKey("topK")) {
                try {
                    topK = Integer.parseInt(request.get("topK"));
                } catch (NumberFormatException e) {
                    // Ignore and use default
                }
            }
            
            response.put("query", query);
            response.put("topK", topK);
            
            // Get embedding and log its details
            List<Double> embedding = embeddingService.getEmbedding(query);
            response.put("embeddingSize", embedding.size());
            response.put("embeddingFirstFew", embedding.subList(0, Math.min(5, embedding.size())));
            
            // Get raw results from ChromaDB
            Map<String, Object> rawResults = chromaDBService.getRawResults(embedding, topK);
            response.put("rawResults", rawResults);
            
            // Get formatted text results
            String textResults = chromaDBService.queryDatabase(embedding, topK);
            response.put("textResults", textResults);
            
            // Extract just the documents for easier viewing
            List<String> documents = chromaDBService.extractDocuments(rawResults);
            response.put("documents", documents);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in debug search", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get all available documents (limited to prevent overwhelming response)
     */
    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a simple embedding for a generic query
            List<Double> embedding = new ArrayList<>();
            for (int i = 0; i < 768; i++) {
                embedding.add(0.1);
            }
            
            // Get a large number of documents to see what's available
            Map<String, Object> rawResults = chromaDBService.getRawResults(embedding, 20);
            
            // Extract just the documents and metadata
            if (rawResults.containsKey("documents")) {
                response.put("documents", rawResults.get("documents"));
            }
            
            if (rawResults.containsKey("metadatas")) {
                response.put("metadatas", rawResults.get("metadatas"));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error listing documents", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Search for documents by keywords
     */
    @GetMapping("/search-by-keyword")
    public ResponseEntity<Map<String, Object>> searchByKeyword(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get embedding for the keyword
            List<Double> embedding = embeddingService.getEmbedding(keyword);
            
            // Get raw results from ChromaDB
            Map<String, Object> rawResults = chromaDBService.getRawResults(embedding, 10);
            
            // Extract just the documents and their metadata
            List<Map<String, Object>> results = new ArrayList<>();
            
            if (rawResults.containsKey("documents") && rawResults.containsKey("metadatas")) {
                List<List<String>> documents = (List<List<String>>) rawResults.get("documents");
                List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) rawResults.get("metadatas");
                
                if (!documents.isEmpty() && !metadatas.isEmpty()) {
                    List<String> docList = documents.get(0);
                    List<Map<String, Object>> metaList = metadatas.get(0);
                    
                    for (int i = 0; i < docList.size() && i < metaList.size(); i++) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("document", docList.get(i));
                        result.put("metadata", metaList.get(i));
                        results.add(result);
                    }
                }
            }
            
            response.put("keyword", keyword);
            response.put("results", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching by keyword", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}