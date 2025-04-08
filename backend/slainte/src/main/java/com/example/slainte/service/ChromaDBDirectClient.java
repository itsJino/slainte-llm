package com.example.slainte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * A low-level direct HTTP client for ChromaDB to bypass RestTemplate issues
 */
@Service
public class ChromaDBDirectClient {
    private static final Logger logger = LoggerFactory.getLogger(ChromaDBDirectClient.class);
    
    private final String CHROMADB_HOST = "http://localhost:8000";
    private final String COLLECTION_NAME = "health_assistant";
    // The collection UUID from your test results
    private final String COLLECTION_UUID = "4b704b22-bbe9-4f7c-a8d2-9c5cb5e6cc1b";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public ChromaDBDirectClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        logger.info("ChromaDB direct client initialized");
    }
    
    /**
     * Query ChromaDB with the embedding vector directly using Java's HttpClient
     */
    @Cacheable(value = "chromaDirectResults", key = "#queryEmbedding.hashCode() + '-' + #nResults")
    public String queryDatabase(List<Double> queryEmbedding, int nResults) {
        try {
            logger.info("Querying ChromaDB directly with embedding of size: {} for {} results", 
                    queryEmbedding.size(), nResults);
            
            // Build the ChromaDB query URL - using UUID instead of name
            String url = CHROMADB_HOST + "/api/v1/collections/" + COLLECTION_UUID + "/query";
            
            // Create the query body
            Map<String, Object> requestBody = new HashMap<>();
            
            // For ChromaDB, query_embeddings is a single array of arrays
            List<List<Double>> embeddings = new ArrayList<>();
            embeddings.add(queryEmbedding);
            requestBody.put("query_embeddings", embeddings);
            requestBody.put("n_results", nResults);
            
            // Add include parameters as array
            List<String> include = Arrays.asList("documents", "metadatas", "distances");
            requestBody.put("include", include);
            
            // Convert the map to a JSON string
            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            logger.debug("Request JSON: {}", jsonRequest);
            
            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();
            
            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Check the response status
            int statusCode = response.statusCode();
            logger.debug("ChromaDB response status: {}", statusCode);
            logger.debug("ChromaDB raw response: {}", response.body());
            
            if (statusCode >= 200 && statusCode < 300) {
                try {
                    // Parse the response
                    Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                    return processChromaDBResponse(responseBody);
                } catch (Exception e) {
                    logger.error("Error parsing ChromaDB response: {}", e.getMessage());
                    return "Error parsing ChromaDB response: " + e.getMessage() + "\nRaw response: " + response.body();
                }
            } else {
                logger.error("ChromaDB error response: {} - {}", statusCode, response.body());
                return "Error from ChromaDB: " + response.body();
            }
        } catch (Exception e) {
            logger.error("Error querying ChromaDB directly: {}", e.getMessage(), e);
            return "Error querying ChromaDB: " + e.getMessage();
        }
    }
    
    /**
     * Minimal raw query test with hardcoded values to test ChromaDB
     */
    public String testMinimalQuery() {
        try {
            // Use UUID instead of name
            String url = CHROMADB_HOST + "/api/v1/collections/" + COLLECTION_UUID + "/query";
            
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
            return "Error in minimal query test: " + e.getMessage();
        }
    }
    
    /**
     * Simple test method to check if ChromaDB is alive and the collection exists
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. Test basic connection
            String healthUrl = CHROMADB_HOST + "/api/v1/heartbeat";
            HttpRequest healthRequest = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> healthResponse = httpClient.send(
                    healthRequest, 
                    HttpResponse.BodyHandlers.ofString()
            );
            
            result.put("connection", healthResponse.statusCode() == 200 ? "OK" : "Failed");
            result.put("heartbeat", healthResponse.body());
            
            // 2. Test if collection exists
            String collectionUrl = CHROMADB_HOST + "/api/v1/collections";
            HttpRequest collectionRequest = HttpRequest.newBuilder()
                    .uri(URI.create(collectionUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> collectionResponse = httpClient.send(
                    collectionRequest, 
                    HttpResponse.BodyHandlers.ofString()
            );
            
            result.put("collections_response", collectionResponse.body());
            
            // Check if our collection exists
            List<Map<String, Object>> collections;
            try {
                // Try to parse as an array first
                collections = objectMapper.readValue(
                        collectionResponse.body(), 
                        List.class
                );
            } catch (Exception e) {
                logger.debug("Failed to parse collections as array, trying as map", e);
                // Fall back to parsing as a map
                Map<String, Object> collectionsData = objectMapper.readValue(
                        collectionResponse.body(), 
                        Map.class
                );
                collections = (List<Map<String, Object>>) collectionsData.get("collections");
            }
            
            // Process the collections list to find our target collection
            boolean collectionExists = false;
            
            if (collections != null) {
                for (Map<String, Object> collection : collections) {
                    if (COLLECTION_NAME.equals(collection.get("name"))) {
                        collectionExists = true;
                        result.put("collection_details", collection);
                        break;
                    }
                }
            }
            
            result.put("collection_exists", collectionExists);
            
            // If the collection exists, get schema/info about it
            if (collectionExists) {
                // Use UUID instead of name for collection info
                String collectionInfoUrl = CHROMADB_HOST + "/api/v1/collections/" + COLLECTION_UUID;
                HttpRequest infoRequest = HttpRequest.newBuilder()
                        .uri(URI.create(collectionInfoUrl))
                        .GET()
                        .build();
                
                HttpResponse<String> infoResponse = httpClient.send(
                        infoRequest, 
                        HttpResponse.BodyHandlers.ofString()
                );
                
                result.put("collection_info", objectMapper.readValue(infoResponse.body(), Map.class));
            }
            
            result.put("status", "success");
            return result;
            
        } catch (Exception e) {
            logger.error("Error testing ChromaDB connection: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    @SuppressWarnings("unchecked")
    private String processChromaDBResponse(Map<String, Object> responseBody) {
        try {
            if (!responseBody.containsKey("documents")) {
                return "No documents in response.";
            }
            
            List<List<String>> documentsList = (List<List<String>>) responseBody.get("documents");
            if (documentsList.isEmpty() || documentsList.get(0).isEmpty()) {
                return "No documents found.";
            }
            
            List<String> documents = documentsList.get(0);
            List<Double> distances = new ArrayList<>();
            List<Map<String, Object>> metadatas = new ArrayList<>();
            
            // Get distances if available
            if (responseBody.containsKey("distances")) {
                try {
                    List<List<Double>> distancesList = (List<List<Double>>) responseBody.get("distances");
                    if (!distancesList.isEmpty()) {
                        distances = distancesList.get(0);
                    }
                } catch (Exception e) {
                    logger.warn("Couldn't process distances", e);
                }
            }
            
            // Get metadata if available
            if (responseBody.containsKey("metadatas")) {
                try {
                    List<List<Map<String, Object>>> metadatasList = 
                        (List<List<Map<String, Object>>>) responseBody.get("metadatas");
                    if (!metadatasList.isEmpty()) {
                        metadatas = metadatasList.get(0);
                    }
                } catch (Exception e) {
                    logger.warn("Couldn't process metadatas", e);
                }
            }
            
            // Build the result string with metadata
            StringBuilder resultBuilder = new StringBuilder();
            for (int i = 0; i < documents.size(); i++) {
                String document = documents.get(i);
                resultBuilder.append(document);
                
                // Add source metadata if available
                if (i < metadatas.size() && metadatas.get(i) != null) {
                    Map<String, Object> metadata = metadatas.get(i);
                    if (metadata.containsKey("source")) {
                        resultBuilder.append("\n[Source: ").append(metadata.get("source")).append("]");
                    }
                }
                
                // Add distance if available (for debugging)
                if (i < distances.size()) {
                    double distance = distances.get(i);
                    logger.debug("Document {} distance: {}", i, distance);
                }
                
                // Add separator between documents
                if (i < documents.size() - 1) {
                    resultBuilder.append("\n\n---\n\n");
                }
            }
            
            String result = resultBuilder.toString().trim();
            
            // Log a preview of the result
            String preview = result.length() > 100 ? result.substring(0, 100) + "..." : result;
            logger.info("ChromaDB returned {} documents. Preview: {}", documents.size(), preview);
            
            return result.isEmpty() ? "No relevant documents found." : result;
            
        } catch (Exception e) {
            logger.error("Error processing ChromaDB response", e);
            return "Error processing search results: " + e.getMessage();
        }
    }
}