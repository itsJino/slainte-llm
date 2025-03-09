package com.example.slainte.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.slainte.dto.ChatRequest;
import com.example.slainte.dto.Message;
import com.example.slainte.service.KnowledgeBaseService;
import com.example.slainte.service.DeepseekChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@RestController
@RequestMapping("/api/llm")
@CrossOrigin(origins = "http://localhost:5173")
public class DeepseekController {
    private static final Logger logger = LoggerFactory.getLogger(DeepseekController.class);

    private final DeepseekChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;

    public DeepseekController(DeepseekChatClient chatClient, KnowledgeBaseService knowledgeBaseService) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chatWithContext(@RequestBody ChatRequest chatRequest) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Extract the latest user message
            Message latestUserMessage = extractLatestUserMessage(chatRequest);
            logger.info("Processing chat request: {}", latestUserMessage.getContent());
            
            // Step 1: Retrieve relevant knowledge from ChromaDB (Vector Search)
            String retrievedInfo = knowledgeBaseService.search(latestUserMessage.getContent());
            
            // Step 2: Create an AI-friendly prompt
            String augmentedPrompt = formatPrompt(retrievedInfo, latestUserMessage.getContent());
            
            // Step 3: Send the augmented prompt to the AI model
            String response = chatClient.prompt(augmentedPrompt);
            
            long endTime = System.currentTimeMillis();
            logger.info("Request processed in {} ms", (endTime - startTime));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    
    /**
     * Extract the latest user message from the chat request
     */
    private Message extractLatestUserMessage(ChatRequest chatRequest) {
        if (chatRequest.getMessages() == null || chatRequest.getMessages().isEmpty()) {
            // Create a default message if none exists
            Message defaultMessage = new Message();
            defaultMessage.setRole("user");
            defaultMessage.setContent(chatRequest.getPrompt() != null ? chatRequest.getPrompt() : "");
            return defaultMessage;
        }
        
        // Find the last user message
        for (int i = chatRequest.getMessages().size() - 1; i >= 0; i--) {
            Message message = chatRequest.getMessages().get(i);
            if ("user".equals(message.getRole())) {
                return message;
            }
        }
        
        // Fallback if no user message found
        Message firstMessage = chatRequest.getMessages().get(0);
        return firstMessage;
    }

    /**
     * Formats the prompt by including relevant retrieved context.
     */
    private String formatPrompt(String context, String userMessage) {
        if (context == null || context.isEmpty() || context.equals("No results found.")) {
            return userMessage;
        } else {
            return "Context information:\n" + context + "\n\n" + userMessage;
        }
    }
}