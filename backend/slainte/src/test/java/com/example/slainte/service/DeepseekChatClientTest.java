package com.example.slainte.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DeepseekChatClientTest {

    @Mock
    private WebClient webClientMock;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock;
    
    @Mock
    private WebClient.ResponseSpec responseSpecMock;
    
    private WebClient.Builder webClientBuilderMock;
    
    private DeepseekChatClient deepseekChatClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    public void setup() {
        // Create a manual mock for WebClient.Builder
        webClientBuilderMock = mock(WebClient.Builder.class);
        
        // Setup WebClient mock chain
        doReturn(webClientBuilderMock).when(webClientBuilderMock).baseUrl(anyString());
        doReturn(webClientMock).when(webClientBuilderMock).build();
        doReturn(requestBodyUriSpecMock).when(webClientMock).post();
        doReturn(requestBodySpecMock).when(requestBodyUriSpecMock).bodyValue(any());
        doReturn(responseSpecMock).when(requestBodySpecMock).retrieve();
        
        deepseekChatClient = new DeepseekChatClient(webClientBuilderMock);
    }
    
    @Test
    public void testPromptSuccess() {
        // Prepare test data
        String inputText = "Tell me about health services";
        
        // Prepare mock response
        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("response", "Here's information about health services");
        String responseJson = responseNode.toString();
        
        when(responseSpecMock.bodyToMono(String.class)).thenReturn(Mono.just(responseJson));
        
        // Execute test
        String result = deepseekChatClient.prompt(inputText);
        
        // Verify result
        assertEquals("Here's information about health services", result);
        
        // Verify the request was made with correct parameters
        verify(requestBodyUriSpecMock).bodyValue(argThat(req -> {
            if (req instanceof Map) {
                Map<String, Object> requestMap = (Map<String, Object>) req;
                return "deepseek-r1:1.5b".equals(requestMap.get("model")) &&
                       requestMap.get("prompt").toString().contains(inputText);
            }
            return false;
        }));
    }
    
    @Test
    public void testPromptWithInvalidResponse() {
        // Prepare test data
        String inputText = "Tell me about health services";
        
        // Prepare invalid mock response (missing 'response' field)
        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("wrongField", "This won't be extracted");
        String responseJson = responseNode.toString();
        
        when(responseSpecMock.bodyToMono(String.class)).thenReturn(Mono.just(responseJson));
        
        // Execute test
        String result = deepseekChatClient.prompt(inputText);
        
        // Verify result
        assertEquals("No response from AI.", result);
    }
    
    @Test
    public void testPromptWithError() {
        // Prepare test data
        String inputText = "Tell me about health services";
        
        // Configure mock to throw an exception
        when(responseSpecMock.bodyToMono(String.class))
            .thenReturn(Mono.error(new RuntimeException("API connection error")));
        
        // Execute test
        String result = deepseekChatClient.prompt(inputText);
        
        // Verify result
        assertTrue(result.contains("Error retrieving response"));
        assertTrue(result.contains("API connection error"));
    }
    
    @Test
    public void testPromptAsync() {
        // Prepare test data
        String inputText = "Tell me about health services";
        
        // Prepare mock response
        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("response", "Here's information about health services");
        String responseJson = responseNode.toString();
        
        when(responseSpecMock.bodyToMono(String.class)).thenReturn(Mono.just(responseJson));
        
        // Execute test
        String result = deepseekChatClient.promptAsync(inputText).block();
        
        // Verify result
        assertEquals("Here's information about health services", result);
        
        // Verify the request was made with correct parameters
        verify(requestBodyUriSpecMock).bodyValue(argThat(req -> {
            if (req instanceof Map) {
                Map<String, Object> requestMap = (Map<String, Object>) req;
                return "deepseek-r1:7b".equals(requestMap.get("model")) &&
                       requestMap.get("prompt").toString().contains(inputText);
            }
            return false;
        }));
    }
    
    @Test
    public void testPromptAsyncWithError() {
        // Prepare test data
        String inputText = "Tell me about health services";
        
        // Configure mock to throw an exception
        when(responseSpecMock.bodyToMono(String.class))
            .thenReturn(Mono.error(new RuntimeException("API connection error")));
        
        // Execute test
        String result = deepseekChatClient.promptAsync(inputText).block();
        
        // Verify result
        assertTrue(result.contains("Error retrieving response"));
        assertTrue(result.contains("API connection error"));
    }
    
    // Testing the private extractResponse method separately
    // without using the WebClient mocks (which caused the UnnecessaryStubbingException)
    @Test
    public void testExtractResponse() throws Exception {
        // We use a new instance that doesn't have the WebClient mocks
        DeepseekChatClient client = new DeepseekChatClient(WebClient.builder());
        
        // Access the private method using reflection
        java.lang.reflect.Method extractResponseMethod = 
            DeepseekChatClient.class.getDeclaredMethod("extractResponse", String.class);
        extractResponseMethod.setAccessible(true);
        
        // Prepare test data
        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("response", "Extracted response text");
        String responseJson = responseNode.toString();
        
        // Execute test by invoking the private method
        String result = (String) extractResponseMethod.invoke(client, responseJson);
        
        // Verify result
        assertEquals("Extracted response text", result);
    }
    
    @Test
    public void testExtractResponseWithError() throws Exception {
        // We use a new instance that doesn't have the WebClient mocks
        DeepseekChatClient client = new DeepseekChatClient(WebClient.builder());
        
        // Access the private method using reflection
        java.lang.reflect.Method extractResponseMethod = 
            DeepseekChatClient.class.getDeclaredMethod("extractResponse", String.class);
        extractResponseMethod.setAccessible(true);
        
        // Prepare invalid JSON
        String invalidJson = "{not valid json}";
        
        // Execute test by invoking the private method
        String result = (String) extractResponseMethod.invoke(client, invalidJson);
        
        // Verify result
        assertTrue(result.contains("Error parsing AI response"));
    }
}