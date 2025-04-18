package com.example.slainte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.example.slainte.model.SearchResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class KnowledgeBaseService {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);
    
    private final EmbeddingService embeddingService;
    private final ChromaDBLowLevelService chromaDBService;
    private final ExecutorService executorService;
    
    // Add a variable to store the last retrieved context
    private String lastRetrievedContext;
    private Map<String, Object> lastRawResults;
    
    // Configuration for search
    private final int DEFAULT_TOP_K = 20;
    private final int MAX_TOP_K = 20;
    
    public KnowledgeBaseService(
            EmbeddingService embeddingService, 
            ChromaDBLowLevelService chromaDBService) {
        this.embeddingService = embeddingService;
        this.chromaDBService = chromaDBService;
        this.executorService = Executors.newFixedThreadPool(4); // Thread pool for parallel processing
        this.lastRetrievedContext = "";
        this.lastRawResults = new HashMap<>();
    }
    
    /**
     * Synchronous method to search the knowledge base
     */
    public String search(String query) {
        return search(query, DEFAULT_TOP_K);
    }
    
    /**
     * Synchronous method to search the knowledge base with customizable number of results
     */
    public String search(String query, int topK) {
        try {
            // Validate parameters
            if (query == null || query.trim().isEmpty()) {
                return "Error: Query cannot be empty.";
            }
            
            // Limit topK to a reasonable range
            topK = Math.min(Math.max(1, topK), MAX_TOP_K);
            
            logger.info("Searching knowledge base for query: {} (topK: {})", query, topK);
            
            // Get embeddings
            List<Double> queryEmbedding = embeddingService.getEmbedding(query);
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                logger.warn("Failed to generate embedding for query: {}", query);
                return "Error: Failed to generate embedding.";
            }
            
            logger.info("Generated embedding of size: {}", queryEmbedding.size());
            
            // Also store the raw results for programmatic access
            this.lastRawResults = chromaDBService.getRawResults(queryEmbedding, topK);
            
            // Query ChromaDB
            String result = chromaDBService.queryDatabase(queryEmbedding, topK);
            
            // Store the retrieved context
            this.lastRetrievedContext = result;
            
            // Enhanced logging of retrieved context
            logRetrievedContext(query, result);
            
            return result;
        } catch (Exception e) {
            logger.error("Error searching knowledge base: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Enhanced logging of retrieved context
     */
    private void logRetrievedContext(String query, String result) {
        // Log a preview of the retrieved context
        String preview = result.length() > 200 ? result.substring(0, 200) + "..." : result;
        logger.info("Retrieved context for query [{}]: {}", query, preview);
        
        // Log the full content with clear boundaries for better visibility
        logger.info("==========BEGIN FULL CONTEXT==========");
        
        // Split and log by paragraphs for better readability
        String[] paragraphs = result.split("\\n\\n");
        for (int i = 0; i < paragraphs.length; i++) {
            logger.info("PARAGRAPH {}: {}", i+1, paragraphs[i].replaceAll("\\n", " "));
        }
        
        logger.info("==========END FULL CONTEXT==========");
        
        // Log statistics about the retrieved content
        int charCount = result.length();
        int wordCount = result.split("\\s+").length;
        int paragraphCount = paragraphs.length;
        
        logger.info("Context statistics - Characters: {}, Words: {}, Paragraphs: {}", 
                charCount, wordCount, paragraphCount);
    }
    
    /**
     * Get search results as a structured SearchResponse object
     */
    public SearchResponse getSearchResults(String query, int topK) {
        // First perform the search to update lastRawResults
        search(query, topK);
        
        // Create the response object
        SearchResponse response = new SearchResponse();
        
        // Extract just the documents from the raw results
        List<String> documents = chromaDBService.extractDocuments(lastRawResults);
        
        // Set the documents in the response
        // Using reflection since SearchResponse doesn't have a setter
        try {
            java.lang.reflect.Field documentsField = SearchResponse.class.getDeclaredField("documents");
            documentsField.setAccessible(true);
            documentsField.set(response, documents);
        } catch (Exception e) {
            logger.error("Error setting documents in SearchResponse", e);
        }
        
        return response;
    }
    
    /**
     * Async version of search using CompletableFuture for parallel processing
     */
    public CompletableFuture<String> searchParallel(String query) {
        return searchParallel(query, DEFAULT_TOP_K);
    }
    
    /**
     * Async version of search with customizable number of results
     */
    public CompletableFuture<String> searchParallel(String query, int topK) {
        // Make a final copy of the parameter for use in the lambda
        final int finalTopK = Math.min(Math.max(1, topK), MAX_TOP_K);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate parameters
                if (query == null || query.trim().isEmpty()) {
                    return "Error: Query cannot be empty.";
                }
                
                logger.info("Parallel searching knowledge base for query: {} (topK: {})", query, finalTopK);
                
                // First get the embedding
                List<Double> queryEmbedding = embeddingService.getEmbedding(query);
                if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                    logger.warn("Failed to generate embedding for query: {}", query);
                    return "Error: Failed to generate embedding.";
                }
                
                // Also store the raw results for programmatic access
                synchronized(this) {
                    this.lastRawResults = chromaDBService.getRawResults(queryEmbedding, finalTopK);
                }
                
                // Then query ChromaDB with the embedding
                String result = chromaDBService.queryDatabase(queryEmbedding, finalTopK);
                
                // Store the retrieved context
                synchronized(this) {
                    this.lastRetrievedContext = result;
                    // Log the context
                    logRetrievedContext(query, result);
                }
                
                return result;
            } catch (Exception e) {
                logger.error("Error in parallel search: {}", e.getMessage(), e);
                return "Error: " + e.getMessage();
            }
        }, executorService);
    }
    
    /**
     * Retrieves the last context that was sent to the chatbot
     * @return The last retrieved context as a string
     */
    public String getLastRetrievedContext() {
        return this.lastRetrievedContext;
    }
    
    /**
     * Get the raw results from the last search
     */
    public Map<String, Object> getLastRawResults() {
        return this.lastRawResults;
    }
    
    /**
     * REST endpoint method to get the last context info
     * Can be called by a controller to expose this data via API
     */
    public Map<String, Object> getLastContextInfo() {
        Map<String, Object> contextInfo = new HashMap<>();
        contextInfo.put("context", this.lastRetrievedContext);
        contextInfo.put("timestamp", new Date().toString());
        contextInfo.put("contextLength", this.lastRetrievedContext.length());
        
        // Add a preview of the context for easy viewing
        String preview = this.lastRetrievedContext;
        if (preview.length() > 300) {
            preview = preview.substring(0, 300) + "...";
        }
        contextInfo.put("preview", preview);
        
        // Add the document count if available
        if (this.lastRawResults.containsKey("documents")) {
            try {
                List<List<String>> documentsList = (List<List<String>>) this.lastRawResults.get("documents");
                if (!documentsList.isEmpty()) {
                    contextInfo.put("documentCount", documentsList.get(0).size());
                }
            } catch (Exception e) {
                logger.warn("Couldn't get document count from raw results", e);
            }
        }
        
        return contextInfo;
    }
}