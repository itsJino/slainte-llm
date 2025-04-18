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
                String topic = extractPrimaryTopic(userQuery);
                
                logger.info("Detected primary topic: {}", topic);
                
                // Perform the initial search
                String retrievedInfo = knowledgeBaseService.search(userQuery);
                
                // Check if retrievedInfo contains an error or is too short
                if (retrievedInfo.startsWith("Error:") || retrievedInfo.length() < 50) {
                    logger.warn("Error or insufficient context retrieved: {}", retrievedInfo);
                    
                    // Try a more general search as fallback
                    String fallbackQuery = topic + " information HSE";
                    logger.info("Trying fallback query: {}", fallbackQuery);
                    retrievedInfo = knowledgeBaseService.search(fallbackQuery);
                    
                    if (retrievedInfo.startsWith("Error:") || retrievedInfo.length() < 50) {
                        logger.warn("Fallback search also failed or insufficient");
                        retrievedInfo = "No relevant information found about " + topic + " in the HSE knowledge base.";
                        logger.info("Using empty context placeholder");
                    } else {
                        logger.info("Using fallback context for topic: {}", topic);
                    }
                }
                
                // Format the prompt with retrieved context
                finalPrompt = formatPromptWithContext(retrievedInfo, userQuery, topic);
                logger.info("Using RAG context with length: {} for topic: {}", retrievedInfo.length(), topic);
                
                // Log the full formatted prompt for debugging
                logger.debug("==========FULL FORMATTED PROMPT==========");
                logger.debug(finalPrompt);
                logger.debug("==========END FORMATTED PROMPT==========");
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
                
                // Extract primary topic
                String topic = extractPrimaryTopic(userQuery);
                result.put("detectedTopic", topic);
                
                String retrievedInfo = knowledgeBaseService.search(userQuery);
                result.put("retrievedContext", retrievedInfo);
                result.put("retrievedContextLength", retrievedInfo.length());
                
                // Check for fallback if needed
                if (retrievedInfo.startsWith("Error:") || retrievedInfo.length() < 50) {
                    String fallbackQuery = topic + " information HSE";
                    result.put("fallbackQuery", fallbackQuery);
                    
                    String fallbackInfo = knowledgeBaseService.search(fallbackQuery);
                    result.put("fallbackContext", fallbackInfo);
                    result.put("fallbackContextLength", fallbackInfo.length());
                    
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
                    userQuery, 
                    topic
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
     * Extract the primary topic from a user query
     */
    private String extractPrimaryTopic(String query) {
        // This is a simple implementation that could be enhanced with NLP techniques
        String lowerQuery = query.toLowerCase();
        
        // Check for common health topics in HSE context
        if (lowerQuery.contains("gp visit card")) return "GP Visit Card";
        if (lowerQuery.contains("medical card")) return "Medical Card";
        if (lowerQuery.contains("hospital")) return "Hospitals";
        if (lowerQuery.contains("emergency") || lowerQuery.contains("urgent care")) return "Emergency Services";
        if (lowerQuery.contains("covid") || lowerQuery.contains("coronavirus")) return "COVID-19";
        if (lowerQuery.contains("vaccine") || lowerQuery.contains("vaccination")) return "Vaccines";
        if (lowerQuery.contains("mental health")) return "Mental Health";
        if (lowerQuery.contains("diabetes")) return "Diabetes";
        if (lowerQuery.contains("blood pressure") || lowerQuery.contains("hypertension")) return "Blood Pressure";
        if (lowerQuery.contains("pregnancy") || lowerQuery.contains("maternity")) return "Pregnancy Services";
        if (lowerQuery.contains("child") || lowerQuery.contains("pediatric")) return "Children's Health";
        if (lowerQuery.contains("elderly") || lowerQuery.contains("older")) return "Services for Older People";
        
        // For queries that don't match specific topics, extract important words
        // This is a very simple approach - could be improved with NLP
        String[] words = query.split("\\s+");
        StringBuilder topic = new StringBuilder();
        
        for (String word : words) {
            // Skip common words and focus on potentially meaningful terms
            if (word.length() > 3 && !isCommonWord(word)) {
                if (topic.length() > 0) topic.append(" ");
                topic.append(word);
                
                // Limit topic length
                if (topic.length() > 30) break;
            }
        }
        
        return topic.length() > 0 ? topic.toString() : "Health Information";
    }
    
    /**
     * Check if a word is a common word that's less likely to be a meaningful topic
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {"about", "with", "this", "that", "what", "when", "where", "which", 
                               "who", "whom", "whose", "why", "how", "information", "need", "would", 
                               "could", "should", "tell", "know", "find"};
        
        String lowerWord = word.toLowerCase();
        for (String commonWord : commonWords) {
            if (lowerWord.equals(commonWord)) return true;
        }
        
        return false;
    }

    /**
     * Formats the prompt by including relevant retrieved context.
     */
    private String formatPromptWithContext(String context, String userMessage, String topic) {
        if (context == null || context.isEmpty() || context.equals("No results found.") || context.startsWith("Error")) {
            return userMessage;
        } else {
            StringBuilder formattedPrompt = new StringBuilder();
            
            // Add a clear separator for the RAG context
            formattedPrompt.append("### HSE INFORMATION ON ").append(topic.toUpperCase()).append(" ###\n\n");
            formattedPrompt.append(context);
            formattedPrompt.append("\n\n### END OF HSE INFORMATION ###\n\n");
            
            // Add the user's query with clear instructions
            formattedPrompt.append("Using ONLY the HSE information provided above, ");
            formattedPrompt.append("please answer the following query: ");
            formattedPrompt.append(userMessage);
            
            return formattedPrompt.toString();
        }
    }
}