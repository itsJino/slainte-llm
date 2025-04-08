package com.example.slainte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    // Updated to use Ollama embedding service (alternatively can use the Flask proxy service)
    private final String EMBEDDING_API_URL = "http://localhost:5000/embed";
    // Direct Ollama API (uncomment to use directly instead of through Flask)
    // private final String EMBEDDING_API_URL = "http://localhost:11434/api/embeddings";
    
    private final WebClient webClient;
    private final boolean useDirectOllamaApi = false; // Set to true to use Ollama API directly

    public EmbeddingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(EMBEDDING_API_URL).build();
    }

    /**
     * Generates an embedding for a given text using the external embedding service.
     * Results are cached to improve performance for repeated queries.
     */
    @Cacheable("embeddings")
    public List<Double> getEmbedding(String text) {
        try {
            logger.info("Generating embedding for text of length: {}", text.length());
            
            Map<String, Object> requestBody;
            if (useDirectOllamaApi) {
                // Format for direct Ollama API
                requestBody = new HashMap<>();
                requestBody.put("model", "nomic-embed-text");
                requestBody.put("prompt", text);
            } else {
                // Format for Flask service
                requestBody = new HashMap<>();
                requestBody.put("text", text);
            }
            
            Map response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // Still blocking for now, but prepared for future async implementation
                
            if (response != null) {
                if (useDirectOllamaApi && response.containsKey("embedding")) {
                    // Ollama API returns embedding directly
                    return (List<Double>) response.get("embedding");
                } else if (!useDirectOllamaApi && response.containsKey("embedding")) {
                    // Flask service format
                    return (List<Double>) response.get("embedding");
                }
            }
            
            logger.warn("Invalid response format from embedding service");
        } catch (Exception e) {
            logger.error("Error fetching embedding: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }
    
    /**
     * Async version of getEmbedding for non-blocking operations
     */
    public Mono<List<Double>> getEmbeddingAsync(String text) {
        Map<String, Object> requestBody;
        if (useDirectOllamaApi) {
            // Format for direct Ollama API
            requestBody = new HashMap<>();
            requestBody.put("model", "nomic-embed-text");
            requestBody.put("prompt", text);
        } else {
            // Format for Flask service
            requestBody = new HashMap<>();
            requestBody.put("text", text);
        }
        
        return webClient.post()
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                if (response != null) {
                    if (useDirectOllamaApi && response.containsKey("embedding")) {
                        // Ollama API returns embedding directly
                        return (List<Double>) response.get("embedding");
                    } else if (!useDirectOllamaApi && response.containsKey("embedding")) {
                        // Flask service format
                        return (List<Double>) response.get("embedding");
                    }
                }
                return Collections.<Double>emptyList();
            })
            .onErrorResume(e -> {
                logger.error("Error fetching embedding async: {}", e.getMessage(), e);
                return Mono.just(Collections.emptyList());
            });
    }
}