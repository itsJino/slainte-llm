package com.example.slainte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ChromaDBLowLevelService {
    private static final Logger logger = LoggerFactory.getLogger(ChromaDBLowLevelService.class);
    
    private final String CHROMADB_HOST = "http://localhost:8000";
    private final String COLLECTION_NAME = "health_assistant";
    private final String COLLECTION_UUID = "4b704b22-bbe9-4f7c-a8d2-9c5cb5e6cc1b";
    private final String CHROMADB_URL;
    private final ObjectMapper objectMapper;

    public ChromaDBLowLevelService() {
        // Use collection UUID instead of name in the URL
        this.CHROMADB_URL = CHROMADB_HOST + "/api/v1/collections/" + COLLECTION_UUID + "/query";
        this.objectMapper = new ObjectMapper();
        logger.info("ChromaDB low-level service initialized with URL: {}", CHROMADB_URL);
    }

    /**
     * Query ChromaDB with the embedding vector using low-level HttpURLConnection
     */
    @Cacheable(value = "chromaResults", key = "#queryEmbedding.hashCode() + '-' + #nResults")
    public String queryDatabase(List<Double> queryEmbedding, int nResults) {
        HttpURLConnection conn = null;
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
            
            // Create the URL
            URL url = new URL(CHROMADB_URL);
            
            // Open connection
            conn = (HttpURLConnection) url.openConnection();
            
            // Setup the request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            
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
            
            // Write the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Get the response
            int responseCode = conn.getResponseCode();
            logger.debug("ChromaDB response code: {}", responseCode);
            
            // Read the response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            
            if (responseCode >= 200 && responseCode < 300) {
                Map<String, Object> responseBody = objectMapper.readValue(response.toString(), Map.class);
                return processChromaDBResponse(responseBody);
            } else {
                logger.error("ChromaDB error response: {} - {}", responseCode, response.toString());
                return "Error from ChromaDB: " + response.toString();
            }
            
        } catch (Exception e) {
            logger.error("Error querying ChromaDB: {}", e.getMessage(), e);
            return "Error querying ChromaDB: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * Get raw results from ChromaDB for programmatic access
     */
    public Map<String, Object> getRawResults(List<Double> queryEmbedding, int nResults) {
        HttpURLConnection conn = null;
        try {
            logger.info("Getting raw results from ChromaDB with embedding of size: {} for {} results", 
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
            
            // Create the URL
            URL url = new URL(CHROMADB_URL);
            
            // Open connection
            conn = (HttpURLConnection) url.openConnection();
            
            // Setup the request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            
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
            
            if (responseCode >= 200 && responseCode < 300) {
                return objectMapper.readValue(response.toString(), Map.class);
            } else {
                logger.error("ChromaDB error fetching raw results: {} - {}", responseCode, response.toString());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Error from ChromaDB: " + response.toString());
                return errorResponse;
            }
            
        } catch (Exception e) {
            logger.error("Error getting raw results from ChromaDB: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error: " + e.getMessage());
            return errorResponse;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
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
    
    /**
     * Extract just the documents from the ChromaDB response for simpler access
     */
    @SuppressWarnings("unchecked")
    public List<String> extractDocuments(Map<String, Object> responseBody) {
        try {
            if (!responseBody.containsKey("documents")) {
                return Collections.emptyList();
            }
            
            List<List<String>> documentsList = (List<List<String>>) responseBody.get("documents");
            if (documentsList.isEmpty() || documentsList.get(0).isEmpty()) {
                return Collections.emptyList();
            }
            
            return documentsList.get(0);
        } catch (Exception e) {
            logger.error("Error extracting documents from ChromaDB response", e);
            return Collections.emptyList();
        }
    }
}