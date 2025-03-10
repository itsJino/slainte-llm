package com.example.slainte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ChromaDBService {
    private static final Logger logger = LoggerFactory.getLogger(ChromaDBService.class);
    
    private final String CHROMADB_URL = "http://localhost:8000/api/v1/collections/66ca948c-7e05-428e-b12b-b9619030733f/query";
    private final RestTemplate restTemplate;

    public ChromaDBService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Query ChromaDB with the embedding vector.
     * Results are cached by the embedding signature to improve performance.
     */
    @Cacheable(value = "chromaResults", key = "#queryEmbedding.hashCode() + '-' + #nResults")
    public String queryDatabase(List<Double> queryEmbedding, int nResults) {
        try {
            // Create the request exactly as specified in ChromaDB API documentation
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query_embeddings", List.of(queryEmbedding));
            requestBody.put("n_results", nResults);
            requestBody.put("include", List.of("documents", "metadatas", "distances"));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                CHROMADB_URL,
                HttpMethod.POST, 
                requestEntity, 
                Map.class
            );
            
            if (response.getBody() != null) {
                return processChromaDBResponse(response.getBody());
            } else {
                return "No results from ChromaDB.";
            }
        } catch (Exception e) {
            logger.error("Error querying ChromaDB", e);
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
    private String processChromaDBResponse(Map responseBody) {
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
            
            // Build the result string, filtering by relevance if possible
            StringBuilder resultBuilder = new StringBuilder();
            for (int i = 0; i < documents.size(); i++) {
                String document = documents.get(i);
                resultBuilder.append(document).append("\n\n");
            }
            
            String result = resultBuilder.toString().trim();
            return result.isEmpty() ? "No relevant documents found." : result;
            
        } catch (Exception e) {
            logger.error("Error processing ChromaDB response", e);
            return "Error processing search results: " + e.getMessage();
        }
    }
}