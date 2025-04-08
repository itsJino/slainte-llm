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

@Service
public class ChromaDBService {
    private static final Logger logger = LoggerFactory.getLogger(ChromaDBService.class);
    
    private final String CHROMADB_HOST = "http://localhost:8000";
    private final String COLLECTION_NAME = "health_assistant";
    private final String COLLECTION_UUID = "4b704b22-bbe9-4f7c-a8d2-9c5cb5e6cc1b";
    private final String CHROMADB_URL;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChromaDBService() {
        // Use collection UUID instead of name in the URL
        this.CHROMADB_URL = CHROMADB_HOST + "/api/v1/collections/" + COLLECTION_UUID + "/query";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        logger.info("ChromaDB service initialized with URL: {}", CHROMADB_URL);
    }

    /**
     * Query ChromaDB with the embedding vector.
     * Results are cached by the embedding signature to improve performance.
     */
    @Cacheable(value = "chromaResults", key = "#queryEmbedding.hashCode() + '-' + #nResults")
    public String queryDatabase(List<Double> queryEmbedding, int nResults) {
        try {
            logger.info("Querying ChromaDB with embedding of size: {} for {} results", 
                    queryEmbedding.size(), nResults);
            
            // Validate embedding dimensions
            if (queryEmbedding.size() != 768) {
                logger.warn("Embedding dimension {} does not match required dimension 768", queryEmbedding.size());
                // Pad or truncate if needed - padding is safer
                if (queryEmbedding.size() < 768) {
                    // Pad with zeros
                    List<Double> paddedEmbedding = new ArrayList<>(queryEmbedding);
                    while (paddedEmbedding.size() < 768) {
                        paddedEmbedding.add(0.0);
                    }
                    queryEmbedding = paddedEmbedding;
                    logger.info("Padded embedding to 768 dimensions");
                } else {
                    // Truncate
                    queryEmbedding = queryEmbedding.subList(0, 768);
                    logger.info("Truncated embedding to 768 dimensions");
                }
            }
            
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
            
            // Create HTTP request with Java's HttpClient
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHROMADB_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();
            
            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Check status code and response
            int statusCode = response.statusCode();
            logger.debug("ChromaDB response status: {}", statusCode);
            
            if (statusCode >= 200 && statusCode < 300) {
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                return processChromaDBResponse(responseBody);
            } else {
                logger.error("ChromaDB error response: {} - {}", statusCode, response.body());
                return "Error from ChromaDB: " + response.body();
            }
        } catch (Exception e) {
            logger.error("Error querying ChromaDB: {}", e.getMessage(), e);
            return "Error querying ChromaDB: " + e.getMessage();
        }
    }
    
    /**
     * Async version of queryDatabase for parallel processing
     * Uses CompletableFuture for better integration with existing code
     */
    public CompletableFuture<String> queryDatabaseAsync(List<Double> queryEmbedding, int nResults) {
        return CompletableFuture.supplyAsync(() -> {
            return queryDatabase(queryEmbedding, nResults);
        });
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