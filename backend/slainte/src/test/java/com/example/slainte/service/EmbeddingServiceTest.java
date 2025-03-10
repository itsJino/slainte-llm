package com.example.slainte.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmbeddingServiceTest {

    private WebClient.Builder webClientBuilder;
    private WebClient webClientMock;
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private WebClient.ResponseSpec responseSpec;
    private EmbeddingService embeddingService;

    @BeforeEach
    public void setup() {
        // Create mock objects
        webClientBuilder = mock(WebClient.Builder.class);
        webClientMock = mock(WebClient.class);
        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);
        
        // Setup WebClient builder
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClientMock);
        
        // Setup WebClient post method
        when(webClientMock.post()).thenReturn(requestBodyUriSpec);
        
        // Use doReturn for the bodyValue method to avoid the generic type issues
        doReturn(requestBodyUriSpec).when(requestBodyUriSpec).bodyValue(any());
        
        // Setup retrieve method
        doReturn(responseSpec).when(requestBodyUriSpec).retrieve();
        
        // Create service with mocked dependencies
        embeddingService = new EmbeddingService(webClientBuilder);
    }

    @Test
    public void testGetEmbedding_Success() {
        // Setup mock response data
        List<Double> expectedEmbedding = Arrays.asList(0.1, 0.2, 0.3);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("embedding", expectedEmbedding);
        
        // Setup response
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(responseMap));

        // Execute
        List<Double> result = embeddingService.getEmbedding("test text");

        // Verify
        assertNotNull(result);
        assertEquals(expectedEmbedding, result);
        
        // Verify interactions
        verify(webClientMock).post();
        verify(requestBodyUriSpec).bodyValue(any());
    }

    @Test
    public void testGetEmbedding_EmptyResponse() {
        // Setup empty response
        Map<String, Object> emptyResponse = new HashMap<>();
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(emptyResponse));

        // Execute
        List<Double> result = embeddingService.getEmbedding("test text");

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetEmbedding_Exception() {
        // Setup error response
        when(responseSpec.bodyToMono(Map.class))
            .thenReturn(Mono.error(new RuntimeException("API error")));

        // Execute
        List<Double> result = embeddingService.getEmbedding("test text");

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetEmbeddingAsync_Success() {
        // Setup mock response data
        List<Double> expectedEmbedding = Arrays.asList(0.1, 0.2, 0.3);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("embedding", expectedEmbedding);
        
        // Setup response
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(responseMap));

        // Execute
        List<Double> result = embeddingService.getEmbeddingAsync("test text").block();

        // Verify
        assertNotNull(result);
        assertEquals(expectedEmbedding, result);
    }

    @Test
    public void testGetEmbeddingAsync_Error() {
        // Setup error response
        when(responseSpec.bodyToMono(Map.class))
            .thenReturn(Mono.error(new RuntimeException("API error")));

        // Execute
        List<Double> result = embeddingService.getEmbeddingAsync("test text").block();

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}