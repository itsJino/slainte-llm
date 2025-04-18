package com.example.slainte.controller;

import com.example.slainte.dto.ChatRequest;
import com.example.slainte.dto.Message;
import com.example.slainte.service.DeepseekChatClient;
import com.example.slainte.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeepseekControllerTest {

    @Mock
    private DeepseekChatClient chatClientMock;
    
    @Mock
    private KnowledgeBaseService knowledgeBaseServiceMock;
    
    private DeepseekController deepseekController;
    
    @BeforeEach
    public void setup() {
        deepseekController = new DeepseekController(chatClientMock, knowledgeBaseServiceMock);
    }
    
    @Test
    public void testChatWithContextUsingRAG() {
        // Prepare test data
        ChatRequest request = new ChatRequest();
        request.setUseRag(true);
        
        List<Message> messages = new ArrayList<>();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("Tell me about GP Visit Cards");
        messages.add(userMessage);
        request.setMessages(messages);
        
        // Configure mocks
        when(knowledgeBaseServiceMock.search("Tell me about GP Visit Cards"))
            .thenReturn("GP Visit Card information from knowledge base");
        
        // The formatted prompt would contain both the context and the user query
        String formattedPrompt = "### HSE INFORMATION ON GP VISIT CARDS ###\n\n" +
                               "GP Visit Card information from knowledge base\n\n" +
                               "### END OF HSE INFORMATION ###\n\n" +
                               "Using ONLY the HSE information provided above, " +
                               "please answer the following query about GP Visit Cards: " +
                               "Tell me about GP Visit Cards";
        
        when(chatClientMock.prompt(formattedPrompt))
            .thenReturn("AI response about GP Visit Cards");
        
        // Execute test
        ResponseEntity<String> responseEntity = deepseekController.chatWithContext(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("AI response about GP Visit Cards", responseEntity.getBody());
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).search("Tell me about GP Visit Cards");
        verify(chatClientMock).prompt(formattedPrompt);
    }
    
    @Test
    public void testChatWithContextWithoutRAG() {
        // Prepare test data
        ChatRequest request = new ChatRequest();
        request.setUseRag(false);
        
        List<Message> messages = new ArrayList<>();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("Tell me about GP Visit Cards");
        messages.add(userMessage);
        request.setMessages(messages);
        
        // Configure mocks
        when(chatClientMock.prompt("Tell me about GP Visit Cards"))
            .thenReturn("AI response about GP Visit Cards without context");
        
        // Execute test
        ResponseEntity<String> responseEntity = deepseekController.chatWithContext(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("AI response about GP Visit Cards without context", responseEntity.getBody());
        
        // Verify mock interactions - should not call knowledge base service
        verifyNoInteractions(knowledgeBaseServiceMock);
        verify(chatClientMock).prompt("Tell me about GP Visit Cards");
    }
    
    @Test
    public void testStartingAssessmentFlow() {
        // Prepare test data for assessment flow
        ChatRequest request = new ChatRequest();
        // Default is useRag=true, but for assessment it should force useRag=false
        request.setUseRag(true);
        
        List<Message> messages = new ArrayList<>();
        
        // System message to trigger assessment mode
        Message systemMessage = new Message();
        systemMessage.setRole("system");
        systemMessage.setContent("start assessment symptom checker");
        messages.add(systemMessage);
        
        // User message
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("I have a headache");
        messages.add(userMessage);
        
        request.setMessages(messages);
        
        // Configure mocks
        when(chatClientMock.prompt("I have a headache"))
            .thenReturn("AI assessment response for headache");
        
        // Execute test
        ResponseEntity<String> responseEntity = deepseekController.chatWithContext(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("AI assessment response for headache", responseEntity.getBody());
        
        // Verify mock interactions - should not call knowledge base service
        verifyNoInteractions(knowledgeBaseServiceMock);
        verify(chatClientMock).prompt("I have a headache");
    }
    
    @Test
    public void testSpecificGPVisitCardQuery() {
        // Prepare test data
        ChatRequest request = new ChatRequest();
        request.setUseRag(true);
        
        List<Message> messages = new ArrayList<>();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("How do I get a GP visit card?");
        messages.add(userMessage);
        request.setMessages(messages);
        
        // Configure mocks - should use specialized query
        when(knowledgeBaseServiceMock.search("Information on GP Visit Card eligibility and application process"))
            .thenReturn("Specialized GP Visit Card information");
        
        String formattedPrompt = "### HSE INFORMATION ON GP VISIT CARDS ###\n\n" +
                               "Specialized GP Visit Card information\n\n" +
                               "### END OF HSE INFORMATION ###\n\n" +
                               "Using ONLY the HSE information provided above, " +
                               "please answer the following query about GP Visit Cards: " +
                               "How do I get a GP visit card?";
        
        when(chatClientMock.prompt(formattedPrompt))
            .thenReturn("AI response about getting a GP Visit Card");
        
        // Execute test
        ResponseEntity<String> responseEntity = deepseekController.chatWithContext(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("AI response about getting a GP Visit Card", responseEntity.getBody());
        
        // Verify mock interactions - should use specialized query
        verify(knowledgeBaseServiceMock).search("Information on GP Visit Card eligibility and application process");
        verify(chatClientMock).prompt(formattedPrompt);
    }
    
    @Test
    public void testFallbackSearchOnInsufficientContext() {
        // Prepare test data
        ChatRequest request = new ChatRequest();
        request.setUseRag(true);
        
        List<Message> messages = new ArrayList<>();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("Tell me about GP Visit Cards");
        messages.add(userMessage);
        request.setMessages(messages);
        
        // Configure mocks - first search returns insufficient context
        when(knowledgeBaseServiceMock.search("Tell me about GP Visit Cards"))
            .thenReturn("Short"); // Too short, should trigger fallback
        
        when(knowledgeBaseServiceMock.search("GP Visit Card information HSE"))
            .thenReturn("Fallback GP Visit Card information from HSE knowledge base");
        
        String formattedPrompt = "### HSE INFORMATION ON GP VISIT CARDS ###\n\n" +
                               "Fallback GP Visit Card information from HSE knowledge base\n\n" +
                               "### END OF HSE INFORMATION ###\n\n" +
                               "Using ONLY the HSE information provided above, " +
                               "please answer the following query about GP Visit Cards: " +
                               "Tell me about GP Visit Cards";
        
        when(chatClientMock.prompt(formattedPrompt))
            .thenReturn("AI response with fallback information");
        
        // Execute test
        ResponseEntity<String> responseEntity = deepseekController.chatWithContext(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("AI response with fallback information", responseEntity.getBody());
        
        // Verify mock interactions - should try original query then fallback
        verify(knowledgeBaseServiceMock).search("Tell me about GP Visit Cards");
        verify(knowledgeBaseServiceMock).search("GP Visit Card information HSE");
        verify(chatClientMock).prompt(formattedPrompt);
    }
    
    @Test
    public void testHandlingException() {
        // Prepare test data
        ChatRequest request = new ChatRequest();
        request.setUseRag(true);
        
        List<Message> messages = new ArrayList<>();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("Tell me about GP Visit Cards");
        messages.add(userMessage);
        request.setMessages(messages);
        
        // Configure mocks to throw exception
        when(knowledgeBaseServiceMock.search("Tell me about GP Visit Cards"))
            .thenThrow(new RuntimeException("Knowledge base error"));
        
        // Execute test
        ResponseEntity<String> responseEntity = deepseekController.chatWithContext(request);
        
        // Verify result
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Error: Knowledge base error"));
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).search("Tell me about GP Visit Cards");
        verifyNoInteractions(chatClientMock);
    }
    
    @Test
    public void testDebugEndpoint() {
        // Prepare test data
        ChatRequest request = new ChatRequest();
        request.setUseRag(true);
        
        List<Message> messages = new ArrayList<>();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("Tell me about GP Visit Cards");
        messages.add(userMessage);
        request.setMessages(messages);
        
        // Configure mocks
        when(knowledgeBaseServiceMock.search("Tell me about GP Visit Cards"))
            .thenReturn("GP Visit Card information from knowledge base");
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = deepseekController.debugChat(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals("Tell me about GP Visit Cards", responseBody.get("userMessage"));
        assertEquals(true, responseBody.get("useRag"));
        assertEquals("GP Visit Card information from knowledge base", responseBody.get("retrievedContext"));
        assertEquals("GP Visit Card information from knowledge base", responseBody.get("finalContext"));
        
        // The finalPrompt should contain both context and user message
        String finalPrompt = (String) responseBody.get("finalPrompt");
        assertTrue(finalPrompt.contains("GP Visit Card information from knowledge base"));
        assertTrue(finalPrompt.contains("Tell me about GP Visit Cards"));
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).search("Tell me about GP Visit Cards");
    }
}