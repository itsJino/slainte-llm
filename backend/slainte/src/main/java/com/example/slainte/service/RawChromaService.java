package com.example.slainte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A raw HTTP service for ChromaDB using Java's HttpURLConnection
 */
@Service
public class RawChromaService {
    private static final Logger logger = LoggerFactory.getLogger(RawChromaService.class);
    
    private final String CHROMADB_HOST = "http://localhost:8000";
    private final String COLLECTION_NAME = "health_assistant";
    // The collection UUID from your test results
    private final String COLLECTION_UUID = "4b704b22-bbe9-4f7c-a8d2-9c5cb5e6cc1b";
    
    /**
     * Perform a raw query to ChromaDB using Java's HttpURLConnection
     */
    public String rawQuery(List<Double> embedding, int nResults) {
        HttpURLConnection conn = null;
        try {
            // Create the URL for the query endpoint - using UUID instead of name
            URL url = new URL(CHROMADB_HOST + "/api/v1/collections/" + COLLECTION_UUID + "/query");
            
            // Open connection
            conn = (HttpURLConnection) url.openConnection();
            
            // Setup the request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            
            // Limit the embedding size for simplicity in this test
            int embeddingSize = Math.min(embedding.size(), 10);
            
            // Build a minimal JSON request
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"query_embeddings\":[[");
            
            // Add the first few elements of the embedding
            for (int i = 0; i < embeddingSize; i++) {
                jsonBuilder.append(embedding.get(i));
                if (i < embeddingSize - 1) {
                    jsonBuilder.append(",");
                }
            }
            
            jsonBuilder.append("]],\"n_results\":").append(nResults).append("}");
            
            String jsonRequest = jsonBuilder.toString();
            
            logger.info("Raw ChromaDB query: {}", jsonRequest);
            
            // Write the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Get the response
            int responseCode = conn.getResponseCode();
            
            // Read the response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            
            logger.info("ChromaDB raw response code: {}", responseCode);
            logger.info("ChromaDB raw response: {}", response.toString());
            
            return "Response Code: " + responseCode + "\n\nResponse Body: " + response.toString();
            
        } catch (Exception e) {
            logger.error("Error executing raw ChromaDB query: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * Test method with hardcoded values
     */
    public String testHardcodedQuery() {
        HttpURLConnection conn = null;
        try {
            // Create the URL for the query endpoint - using UUID instead of name
            URL url = new URL(CHROMADB_HOST + "/api/v1/collections/" + COLLECTION_UUID + "/query");
            
            // Open connection
            conn = (HttpURLConnection) url.openConnection();
            
            // Setup the request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            
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
            String jsonRequest = "{\"query_embeddings\":["+ embeddingBuilder.toString() +"],\"n_results\":1}";
            
            logger.info("Hardcoded ChromaDB query: {}", jsonRequest);
            
            // Write the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Get the response
            int responseCode = conn.getResponseCode();
            
            // Read the response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            
            logger.info("ChromaDB hardcoded response code: {}", responseCode);
            logger.info("ChromaDB hardcoded response: {}", response.toString());
            
            return "Response Code: " + responseCode + "\n\nResponse Body: " + response.toString();
            
        } catch (Exception e) {
            logger.error("Error executing hardcoded ChromaDB query: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}