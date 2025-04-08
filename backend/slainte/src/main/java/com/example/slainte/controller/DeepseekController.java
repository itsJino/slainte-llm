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
import java.util.Map;
import java.util.Optional;

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
            
            // Check if this is a symptom assessment start
            boolean isStartingAssessment = false;
            if (chatRequest.getMessages() != null) {
                Optional<Message> startAssessmentMsg = chatRequest.getMessages().stream()
                    .filter(m -> "system".equals(m.getRole()) && 
                           m.getContent() != null && 
                           m.getContent().contains("start assessment"))
                    .findFirst();
                
                if (startAssessmentMsg.isPresent()) {
                    isStartingAssessment = true;
                    logger.info("Detected start of symptom assessment flow");
                }
            }
            
            // Check if the useRag flag is present and false
            boolean useRag = true; // Default to true for backward compatibility
            if (chatRequest.getUseRag() != null) {
                useRag = chatRequest.getUseRag();
            }
            
            // Force useRag to false if this is a start assessment message
            if (isStartingAssessment) {
                useRag = false;
                logger.info("Forcing RAG off for symptom assessment start");
            }
            
            // Log whether RAG is being used for this request
            logger.info("RAG usage for this request: {}", useRag ? "ENABLED" : "DISABLED");
            
            String finalPrompt;
            
            if (useRag) {
                // Only retrieve information from ChromaDB if useRag is true
                String userQuery = latestUserMessage.getContent();
                
                // Check if the query is specifically about GP Visit Cards
                if (userQuery.toLowerCase().contains("gp visit card")) {
                    // Use a more specific query for GP Visit Cards
                    userQuery = "Information on GP Visit Card eligibility and application process";
                }
                
                String retrievedInfo = knowledgeBaseService.search(userQuery);
                
                // Check if retrievedInfo contains an error or is too short
                if (retrievedInfo.startsWith("Error:") || retrievedInfo.length() < 50) {
                    logger.warn("Error or insufficient context retrieved: {}", retrievedInfo);
                    
                    // Try a more general search as fallback
                    String fallbackQuery = "GP Visit Card information HSE";
                    logger.info("Trying fallback query: {}", fallbackQuery);
                    retrievedInfo = knowledgeBaseService.search(fallbackQuery);
                    
                    if (retrievedInfo.startsWith("Error:") || retrievedInfo.length() < 50) {
                        logger.warn("Fallback search also failed or insufficient");
                        retrievedInfo = "No relevant information found about GP Visit Cards in the HSE knowledge base.";
                    }
                }
                
                // Format the prompt with retrieved context
                finalPrompt = formatPromptWithContext(retrievedInfo, latestUserMessage.getContent());
                logger.info("Using RAG - retrieved context length: {}", retrievedInfo.length());
                
                // Log a preview of the context
                String contextPreview = retrievedInfo.length() > 200 ? 
                    retrievedInfo.substring(0, 200) + "..." : retrievedInfo;
                logger.info("Context preview: {}", contextPreview);
            } else {
                // Skip RAG retrieval entirely
                finalPrompt = latestUserMessage.getContent();
                logger.info("Skipping RAG retrieval as requested");
            }
            
            // Send the prompt to the AI model
            String response = chatClient.prompt(finalPrompt);
            
            long endTime = System.currentTimeMillis();
            logger.info("Request processed in {} ms", (endTime - startTime));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Diagnostic endpoint to see what would be sent to the LLM
     */
    @PostMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugChat(@RequestBody ChatRequest chatRequest) {
        try {
            Map<String, Object> result = new java.util.HashMap<>();
            
            // Extract the latest user message
            Message latestUserMessage = extractLatestUserMessage(chatRequest);
            result.put("userMessage", latestUserMessage.getContent());
            
            // Get RAG info
            boolean useRag = chatRequest.getUseRag() != null ? chatRequest.getUseRag() : true;
            result.put("useRag", useRag);
            
            if (useRag) {
                // Get the context that would be retrieved
                String userQuery = latestUserMessage.getContent();
                
                // Check if the query is specifically about GP Visit Cards
                if (userQuery.toLowerCase().contains("gp visit card")) {
                    // Use a more specific query for GP Visit Cards
                    userQuery = "Information on GP Visit Card eligibility and application process";
                    result.put("specializedQuery", userQuery);
                }
                
                String retrievedInfo = knowledgeBaseService.search(userQuery);
                result.put("retrievedContext", retrievedInfo);
                
                // Check for fallback if needed
                if (retrievedInfo.startsWith("Error:") || retrievedInfo.length() < 50) {
                    String fallbackQuery = "GP Visit Card information HSE";
                    result.put("fallbackQuery", fallbackQuery);
                    
                    String fallbackInfo = knowledgeBaseService.search(fallbackQuery);
                    result.put("fallbackContext", fallbackInfo);
                    
                    // Which context would be used
                    if (fallbackInfo.startsWith("Error:") || fallbackInfo.length() < 50) {
                        result.put("finalContext", "No relevant information found.");
                    } else {
                        result.put("finalContext", fallbackInfo);
                    }
                } else {
                    result.put("finalContext", retrievedInfo);
                }
                
                // Format the final prompt
                String finalPrompt = formatPromptWithContext(
                    (String)result.get("finalContext"), 
                    latestUserMessage.getContent()
                );
                result.put("finalPrompt", finalPrompt);
            } else {
                result.put("finalPrompt", latestUserMessage.getContent());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in debug chat", e);
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
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
    private String formatPromptWithContext(String context, String userMessage) {
        if (context == null || context.isEmpty() || context.equals("No results found.") || context.startsWith("Error")) {
            return userMessage;
        } else {
            StringBuilder formattedPrompt = new StringBuilder();
            
            // Add a clear separator for the RAG context
            formattedPrompt.append("### HSE INFORMATION ON GP VISIT CARDS ###\n\n");
            formattedPrompt.append(context);
            formattedPrompt.append("\n\n### END OF HSE INFORMATION ###\n\n");
            
            // Add the user's query with clear instructions
            formattedPrompt.append("Using ONLY the HSE information provided above, ");
            formattedPrompt.append("please answer the following query about GP Visit Cards: ");
            formattedPrompt.append(userMessage);
            
            return formattedPrompt.toString();
        }
    }
}