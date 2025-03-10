package com.example.slainte.controller;

import com.example.slainte.dto.ChatRequest;
import com.example.slainte.dto.Message;
import com.example.slainte.service.DeepseekChatClient;
import com.example.slainte.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeepseekControllerTest {

    private static final String USER_MESSAGE = "Hello, how are you?";
    private static final String AI_RESPONSE = "I'm doing well, thank you!";
    private static final String RETRIEVED_CONTEXT = "Important context information";

    @Mock
    private DeepseekChatClient chatClient;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private DeepseekController deepseekController;

    private ChatRequest chatRequest;

    @BeforeEach
    public void setup() {
        // Create a fresh list of messages for each test
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", USER_MESSAGE));
        
        // Create a fresh chat request for each test
        chatRequest = new ChatRequest();
        chatRequest.setMessages(messages);
        chatRequest.setUseRag(true);
    }

    @Test
    public void testChatWithContext_WithRAG() {
        // Setup
        String expectedPrompt = "Context information:\n" + RETRIEVED_CONTEXT + "\n\n" + USER_MESSAGE;
        
        when(knowledgeBaseService.search(USER_MESSAGE)).thenReturn(RETRIEVED_CONTEXT);
        when(chatClient.prompt(expectedPrompt)).thenReturn(AI_RESPONSE);

        // Execute
        ResponseEntity<String> response = deepseekController.chatWithContext(chatRequest);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(AI_RESPONSE, response.getBody());
        
        verify(knowledgeBaseService).search(USER_MESSAGE);
        verify(chatClient).prompt(expectedPrompt);
    }

    @Test
    public void testChatWithContext_WithoutRAG() {
        // Setup - explicitly disable RAG
        chatRequest.setUseRag(false);
        
        when(chatClient.prompt(USER_MESSAGE)).thenReturn(AI_RESPONSE);

        // Execute
        ResponseEntity<String> response = deepseekController.chatWithContext(chatRequest);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(AI_RESPONSE, response.getBody());
        
        // Should not call search when RAG is disabled
        verify(knowledgeBaseService, never()).search(anyString());
        verify(chatClient).prompt(USER_MESSAGE);
    }

    @Test
    public void testChatWithContext_SymptomAssessment() {
        // Setup - create a new request with symptom assessment system message
        List<Message> assessmentMessages = new ArrayList<>();
        assessmentMessages.add(new Message("system", "start assessment for the user"));
        assessmentMessages.add(new Message("user", "I have a headache"));
        
        ChatRequest assessmentRequest = new ChatRequest();
        assessmentRequest.setMessages(assessmentMessages);
        assessmentRequest.setUseRag(true); // Even though RAG is enabled, system message should override
        
        when(chatClient.prompt("I have a headache")).thenReturn("Let me help with your headache");

        // Execute
        ResponseEntity<String> response = deepseekController.chatWithContext(assessmentRequest);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Let me help with your headache", response.getBody());
        
        // Should not call search due to symptom assessment detection
        verify(knowledgeBaseService, never()).search(anyString());
        verify(chatClient).prompt("I have a headache");
    }

    @Test
    public void testChatWithContext_EmptyMessages_FallbackToPrompt() {
        // Setup - request with no messages but with a prompt
        ChatRequest requestWithPrompt = new ChatRequest();
        requestWithPrompt.setPrompt(USER_MESSAGE);
        requestWithPrompt.setMessages(null); // No messages
        requestWithPrompt.setUseRag(true);
        
        when(knowledgeBaseService.search(USER_MESSAGE)).thenReturn(RETRIEVED_CONTEXT);
        String expectedPrompt = "Context information:\n" + RETRIEVED_CONTEXT + "\n\n" + USER_MESSAGE;
        when(chatClient.prompt(expectedPrompt)).thenReturn(AI_RESPONSE);

        // Execute
        ResponseEntity<String> response = deepseekController.chatWithContext(requestWithPrompt);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(AI_RESPONSE, response.getBody());
        verify(knowledgeBaseService).search(USER_MESSAGE);
        verify(chatClient).prompt(expectedPrompt);
    }
    
    @Test
    public void testChatWithContext_EmptyUserMessages() {
        // Setup - request with only system messages (no user messages)
        List<Message> systemMessages = new ArrayList<>();
        systemMessages.add(new Message("system", "System instruction"));
        
        ChatRequest systemOnlyRequest = new ChatRequest();
        systemOnlyRequest.setMessages(systemMessages);
        systemOnlyRequest.setUseRag(true);
        systemOnlyRequest.setPrompt(USER_MESSAGE); // Fallback prompt
        
        when(knowledgeBaseService.search(USER_MESSAGE)).thenReturn(RETRIEVED_CONTEXT);
        String expectedPrompt = "Context information:\n" + RETRIEVED_CONTEXT + "\n\n" + USER_MESSAGE;
        when(chatClient.prompt(expectedPrompt)).thenReturn(AI_RESPONSE);

        // Execute
        ResponseEntity<String> response = deepseekController.chatWithContext(systemOnlyRequest);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(AI_RESPONSE, response.getBody());
    }

    @Test
    public void testChatWithContext_ErrorCase() {
        // Setup - simulate an exception during processing
        when(knowledgeBaseService.search(USER_MESSAGE))
            .thenThrow(new RuntimeException("Test exception"));

        // Execute
        ResponseEntity<String> response = deepseekController.chatWithContext(chatRequest);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("Error:"));
    }
    
    @Test
    public void testChatWithContext_EmptyContext() {
        // Setup - knowledge base returns empty context
        when(knowledgeBaseService.search(USER_MESSAGE)).thenReturn("");
        when(chatClient.prompt(USER_MESSAGE)).thenReturn(AI_RESPONSE);

        // Execute
        ResponseEntity<String> response = deepseekController.chatWithContext(chatRequest);

        // Verify - should still work without context
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(AI_RESPONSE, response.getBody());
        verify(chatClient).prompt(USER_MESSAGE); // Should not include context in prompt
    }
    
    @Test
    public void testChatWithContext_NoRelevantResults() {
        // Setup - knowledge base returns "No results found"
        when(knowledgeBaseService.search(USER_MESSAGE)).thenReturn("No results found.");
        when(chatClient.prompt(USER_MESSAGE)).thenReturn(AI_RESPONSE);

        // Execute
        ResponseEntity<String> response = deepseekController.chatWithContext(chatRequest);

        // Verify - should still work without results
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(AI_RESPONSE, response.getBody());
        verify(chatClient).prompt(USER_MESSAGE); // Should not include the "No results found" in prompt
    }
}