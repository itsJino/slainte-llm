package com.example.slainte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class KnowledgeBaseService {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);
    
    private final EmbeddingService embeddingService;
    private final ChromaDBService chromaDBService;
    private final ExecutorService executorService;
    
    // Add a variable to store the last retrieved context
    private String lastRetrievedContext;
    
    public KnowledgeBaseService(EmbeddingService embeddingService, ChromaDBService chromaDBService) {
        this.embeddingService = embeddingService;
        this.chromaDBService = chromaDBService;
        this.executorService = Executors.newFixedThreadPool(4); // Thread pool for parallel processing
        this.lastRetrievedContext = "";
    }
    
    /**
     * Synchronous method to search the knowledge base
     */
    public String search(String query) {
        try {
            logger.info("Searching knowledge base for query: {}", query);
            
            // Get embeddings
            List<Double> queryEmbedding = embeddingService.getEmbedding(query);
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                logger.warn("Failed to generate embedding for query: {}", query);
                return "Error: Failed to generate embedding.";
            }
            
            logger.info("Generated embedding of size: {}", queryEmbedding.size());
            
            // Query ChromaDB
            String result = chromaDBService.queryDatabase(queryEmbedding, 8);
            
            // Store the retrieved context
            this.lastRetrievedContext = result;
            
            // Log the retrieved context
            logger.info("Retrieved context for query [{}]: {}", query, result);
            
            return result;
        } catch (Exception e) {
            logger.error("Error searching knowledge base: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Async version of search using CompletableFuture for parallel processing
     */
    public CompletableFuture<String> searchParallel(String query) {
        return CompletableFuture.supplyAsync(() -> {
            // First get the embedding
            List<Double> queryEmbedding = embeddingService.getEmbedding(query);
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                return "Error: Failed to generate embedding.";
            }
            
            // Then query ChromaDB with the embedding
            String result = chromaDBService.queryDatabase(queryEmbedding, 5);
            
            // Store the retrieved context
            synchronized(this) {
                this.lastRetrievedContext = result;
            }
            
            return result;
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
     * REST endpoint method to get the last retrieved context
     * Can be called by a controller to expose this data via API
     */
    public Map<String, Object> getLastContextInfo() {
        Map<String, Object> contextInfo = new HashMap<>();
        contextInfo.put("context", this.lastRetrievedContext);
        contextInfo.put("timestamp", new Date().toString());
        contextInfo.put("contextLength", this.lastRetrievedContext.length());
        return contextInfo;
    }
}