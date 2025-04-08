package com.example.slainte.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * A test controller to directly interact with ChromaDB for troubleshooting
 */
@RestController
@RequestMapping("/api/test/chroma")
public class ChromaDBTestController {
    private static final Logger logger = LoggerFactory.getLogger(ChromaDBTestController.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String CHROMADB_HOST = "http://localhost:8000";
    
    public ChromaDBTestController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Endpoint to test various ChromaDB API interactions
     */
    @GetMapping("/collections")
    public ResponseEntity<Object> listCollections() {
        try {
            String url = CHROMADB_HOST + "/api/v1/collections";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                Object.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error listing collections: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Test a minimal query to ChromaDB
     */
    @GetMapping("/test-query")
    public ResponseEntity<Object> testQuery() {
        try {
            String collectionName = "health_assistant";
            String url = CHROMADB_HOST + "/api/v1/collections/" + collectionName + "/query";
            
            // Create a simple embedding vector (all 0.1 values)
            List<Double> embedding = new ArrayList<>();
            for (int i = 0; i < 768; i++) {
                embedding.add(0.1);
            }
            
            // Create the minimal required request body
            Map<String, Object> requestBody = new HashMap<>();
            List<List<Double>> embeddings = new ArrayList<>();
            embeddings.add(embedding);
            requestBody.put("query_embeddings", embeddings);
            requestBody.put("n_results", 1);
            
            // Convert to JSON
            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            logger.info("Request body: {}", jsonRequest);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                Object.class
            );
            
            logger.info("Response status: {}", response.getStatusCode());
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error testing query: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Test a direct raw query to ChromaDB
     */
    @PostMapping("/raw-query")
    public ResponseEntity<Object> rawQuery(@RequestBody String jsonBody) {
        try {
            String collectionName = "health_assistant";
            String url = CHROMADB_HOST + "/api/v1/collections/" + collectionName + "/query";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                Object.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error with raw query: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}