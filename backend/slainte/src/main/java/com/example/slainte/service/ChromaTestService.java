package com.example.slainte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * A minimal service just for testing ChromaDB connectivity
 */
@Service
public class ChromaTestService {
    private static final Logger logger = LoggerFactory.getLogger(ChromaTestService.class);
    
    private final String CHROMADB_HOST = "http://localhost:8000";
    private final String COLLECTION_NAME = "health_assistant";
    private final HttpClient httpClient;
    
    public ChromaTestService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Run a minimal test query against ChromaDB
     */
    public String runMinimalTest() {
        try {
            String url = CHROMADB_HOST + "/api/v1/collections/" + COLLECTION_NAME + "/query";
            
            // Create a 768-dimension embedding (all filled with 0.1)
            StringBuilder embeddingBuilder = new StringBuilder();
            embeddingBuilder.append('[');
            for (int i = 0; i < 768; i++) {
                embeddingBuilder.append("0.1");
                if (i < 767) {
                    embeddingBuilder.append(',');
                }
            }
            embeddingBuilder.append(']');
            
            // Full JSON with correct dimensions
            String jsonBody = "{\"query_embeddings\":[" + embeddingBuilder.toString() + "],\"n_results\":1}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return "Status: " + response.statusCode() + "\nBody: " + response.body();
        } catch (Exception e) {
            return "Error in minimal test: " + e.getMessage();
        }
    }
    
    /**
     * Simple test to check if ChromaDB is reachable
     */
    public String testHeartbeat() {
        try {
            String url = CHROMADB_HOST + "/api/v1/heartbeat";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return "Status: " + response.statusCode() + "\nBody: " + response.body();
        } catch (Exception e) {
            return "Error checking heartbeat: " + e.getMessage();
        }
    }
}