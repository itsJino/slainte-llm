package com.example.slainte.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeepseekChatClientTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private WebClient.Builder webClientBuilder;
    private DeepseekChatClient deepseekChatClient;

    @BeforeEach
    public void setup() {
        // Create mock WebClient.Builder
        webClientBuilder = mock(WebClient.Builder.class);
        
        // Create mock WebClient
        WebClient webClientMock = mock(WebClient.class);
        
        // Setup builder to return the WebClient
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClientMock);
        
        // Use a more direct approach for the WebClient post chain
        // Instead of mocking the intermediate objects, we'll go straight to the response
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(webClientMock.post()).thenReturn(requestBodyUriSpec);
        
        // Use a workaround for the bodyValue() method
        // Instead of trying to return a RequestBodySpec, we'll handle the next steps directly
        doReturn(requestBodyUriSpec).when(requestBodyUriSpec).bodyValue(any());
        
        // Mock the retrieve() method to return a ResponseSpec
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        doReturn(responseSpec).when(requestBodyUriSpec).retrieve();
        
        // Setup service with mocked WebClient.Builder
        deepseekChatClient = new DeepseekChatClient(webClientBuilder);
    }

    @Test
    public void testPrompt_Success() throws Exception {
        // Setup WebClient and response
        WebClient webClientMock = webClientBuilder.build();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = webClientMock.post();
        
        // Create a successful response
        String expectedResponse = "This is a test response";
        ObjectNode mockResponseNode = objectMapper.createObjectNode();
        mockResponseNode.put("response", expectedResponse);
        String mockJsonResponse = objectMapper.writeValueAsString(mockResponseNode);
        
        // Setup the response mono
        WebClient.ResponseSpec responseSpec = requestBodyUriSpec.retrieve();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        // Execute
        String result = deepseekChatClient.prompt("test query");

        // Verify
        assertEquals(expectedResponse, result);
    }

    @Test
    public void testPrompt_ErrorInRequest() throws Exception {
        // Setup WebClient and response
        WebClient webClientMock = webClientBuilder.build();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = webClientMock.post();
        WebClient.ResponseSpec responseSpec = requestBodyUriSpec.retrieve();
        
        // Setup error response
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("API Error")));

        // Execute
        String result = deepseekChatClient.prompt("test query");

        // Verify
        assertTrue(result.startsWith("Error retrieving response:"));
    }

    @Test
    public void testPrompt_MalformedResponse() throws Exception {
        // Setup WebClient and response
        WebClient webClientMock = webClientBuilder.build();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = webClientMock.post();
        WebClient.ResponseSpec responseSpec = requestBodyUriSpec.retrieve();
        
        // Setup malformed response
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{invalid-json}"));

        // Execute
        String result = deepseekChatClient.prompt("test query");

        // Verify
        assertTrue(result.startsWith("Error parsing AI response:"));
    }

    @Test
    public void testPrompt_MissingResponseField() throws Exception {
        // Setup WebClient and response
        WebClient webClientMock = webClientBuilder.build();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = webClientMock.post();
        WebClient.ResponseSpec responseSpec = requestBodyUriSpec.retrieve();
        
        // JSON without 'response' field
        ObjectNode mockResponseNode = objectMapper.createObjectNode();
        mockResponseNode.put("other_field", "value");
        String mockJsonResponse = objectMapper.writeValueAsString(mockResponseNode);
        
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        // Execute
        String result = deepseekChatClient.prompt("test query");

        // Verify
        assertEquals("No response from AI.", result);
    }

    @Test
    public void testPromptAsync_Success() throws Exception {
        // Setup WebClient and response
        WebClient webClientMock = webClientBuilder.build();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = webClientMock.post();
        WebClient.ResponseSpec responseSpec = requestBodyUriSpec.retrieve();
        
        // Create a successful response
        String expectedResponse = "This is a test response";
        ObjectNode mockResponseNode = objectMapper.createObjectNode();
        mockResponseNode.put("response", expectedResponse);
        String mockJsonResponse = objectMapper.writeValueAsString(mockResponseNode);
        
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        // Execute
        String result = deepseekChatClient.promptAsync("test query").block();

        // Verify
        assertEquals(expectedResponse, result);
    }

    @Test
    public void testPromptAsync_Error() throws Exception {
        // Setup WebClient and response
        WebClient webClientMock = webClientBuilder.build();
        WebClient.RequestBodyUriSpec requestBodyUriSpec = webClientMock.post();
        WebClient.ResponseSpec responseSpec = requestBodyUriSpec.retrieve();
        
        // Setup error response
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("API Error")));

        // Execute
        String result = deepseekChatClient.promptAsync("test query").block();

        // Verify
        assertTrue(result.startsWith("Error retrieving response:"));
    }
}