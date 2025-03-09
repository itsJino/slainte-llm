package com.example.slainte.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class EmbeddingService {

    private final String EMBEDDING_API_URL = "http://localhost:5000/embed";
    private final WebClient webClient;

    public EmbeddingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(EMBEDDING_API_URL).build();
    }

    /**
     * Generates an embedding for a given text using the external Flask embedding service.
     * Results are cached to improve performance for repeated queries.
     */
    @Cacheable("embeddings")
    public List<Double> getEmbedding(String text) {
        try {
            Map<String, String> requestBody = Map.of("text", text);
            
            Map response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // Still blocking for now, but prepared for future async implementation
                
            if (response != null && response.containsKey("embedding")) {
                return (List<Double>) response.get("embedding");
            }
        } catch (Exception e) {
            System.err.println("Error fetching embedding: " + e.getMessage());
        }
        return Collections.emptyList();
    }
    
    /**
     * Async version of getEmbedding for non-blocking operations
     */
    public Mono<List<Double>> getEmbeddingAsync(String text) {
        Map<String, String> requestBody = Map.of("text", text);
        
        return webClient.post()
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                if (response != null && response.containsKey("embedding")) {
                    return (List<Double>) response.get("embedding");
                }
                return Collections.<Double>emptyList();
            })
            .onErrorReturn(Collections.emptyList());
    }
}