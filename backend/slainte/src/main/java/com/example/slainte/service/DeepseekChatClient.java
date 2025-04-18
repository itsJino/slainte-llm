package com.example.slainte.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeepseekChatClient {
    private static final Logger logger = LoggerFactory.getLogger(DeepseekChatClient.class);

    private final String AI_API_URL = "http://localhost:11434/api/generate";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DeepseekChatClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(AI_API_URL).build();
        this.objectMapper = new ObjectMapper();
    }

    // System prompt defined here 
    public final String MAIN_SYSTEM_PROMPT = """
            You are Slainte, a friendly and knowledgeable health assistant.
            // ... existing prompt content ...
            """;

    public String prompt(String inputText) {
        try {
            // Create optimized prompt with reduced size
            String fullPrompt = createOptimizedPrompt(inputText);
            
            logger.debug("Full prompt to LLM: {}", fullPrompt);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-r1:1.5b");
            requestBody.put("prompt", fullPrompt);
            requestBody.put("temperature", 0.0);
            requestBody.put("stream", false);

            
            String response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractResponse)
                .block(); // Still blocking for now, but prepared for future async implementation
                
            return response;
        } catch (Exception e) {
            logger.error("Error retrieving response from LLM", e);
            return "Error retrieving response: " + e.getMessage();
        }
    }
    
    /**
     * Async version of prompt for non-blocking operations
     */
    public Mono<String> promptAsync(String inputText) {
        String fullPrompt = createOptimizedPrompt(inputText);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-r1:7b");
        requestBody.put("prompt", fullPrompt);
        requestBody.put("temperature", 0.0);
        requestBody.put("stream", false);
        
        return webClient.post()
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::extractResponse)
            .onErrorResume(e -> Mono.just("Error retrieving response: " + e.getMessage()));
    }
    
    /**
     * Creates an optimized prompt to reduce token usage
     */
    private String createOptimizedPrompt(String inputText) {
        // Only include essential parts of the system prompt
        String essentialPrompt = MAIN_SYSTEM_PROMPT;
        return essentialPrompt + "\n\nUser: " + inputText + "\nAI:";
    }

    private String extractResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            return root.has("response") ? root.get("response").asText() : "No response from AI.";
        } catch (Exception e) {
            logger.error("Error parsing AI response", e);
            return "Error parsing AI response: " + e.getMessage();
        }
    }
}