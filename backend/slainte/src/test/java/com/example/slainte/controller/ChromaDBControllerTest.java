package com.example.slainte.controller;

import com.example.slainte.service.ChromaDBService;
import com.example.slainte.service.EmbeddingService;
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
public class ChromaDBControllerTest {

    @Mock
    private ChromaDBService chromaDBServiceMock;
    
    @Mock
    private EmbeddingService embeddingServiceMock;
    
    private ChromaDBController chromaDBController;
    
    @BeforeEach
    public void setup() {
        chromaDBController = new ChromaDBController(chromaDBServiceMock, embeddingServiceMock);
    }
    
    @Test
    public void testChromaDBEndpointSuccess() {
        // Configure mock
        when(chromaDBServiceMock.queryDatabase(anyList(), eq(3)))
            .thenReturn("Test ChromaDB response");
        
        // Execute test
        ResponseEntity<Map<String, Object>> response = chromaDBController.testChromaDB();
        
        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(true, responseBody.get("success"));
        assertEquals("ChromaDB test completed", responseBody.get("message"));
        assertEquals("Test ChromaDB response", responseBody.get("response"));
        
        // Verify mock interactions
        verify(chromaDBServiceMock).queryDatabase(anyList(), eq(3));
    }
    
    @Test
    public void testChromaDBEndpointError() {
        // Configure mock to throw exception
        when(chromaDBServiceMock.queryDatabase(anyList(), eq(3)))
            .thenThrow(new RuntimeException("Test error"));
        
        // Execute test
        ResponseEntity<Map<String, Object>> response = chromaDBController.testChromaDB();
        
        // Verify response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(false, responseBody.get("success"));
        assertEquals("Test error", responseBody.get("error"));
        
        // Verify mock interactions
        verify(chromaDBServiceMock).queryDatabase(anyList(), eq(3));
    }
    
    @Test
    public void testQueryEndpointSuccess() {
        // Prepare test data
        String testQuery = "test query";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", testQuery);
        
        List<Double> mockEmbedding = new ArrayList<>();
        for (int i = 0; i < 512; i++) {
            mockEmbedding.add(0.1);
        }
        
        // Configure mocks
        when(embeddingServiceMock.getEmbedding(testQuery)).thenReturn(mockEmbedding);
        when(chromaDBServiceMock.queryDatabase(mockEmbedding, 3))
            .thenReturn("Test ChromaDB response");
        
        // Execute test
        ResponseEntity<Map<String, Object>> response = chromaDBController.testQuery(requestBody);
        
        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(true, responseBody.get("success"));
        assertEquals(testQuery, responseBody.get("query"));
        assertEquals(mockEmbedding.size(), responseBody.get("embeddingSize"));
        assertEquals("Test ChromaDB response", responseBody.get("response"));
        
        // Verify mock interactions
        verify(embeddingServiceMock).getEmbedding(testQuery);
        verify(chromaDBServiceMock).queryDatabase(mockEmbedding, 3);
    }
    
    @Test
    public void testQueryEndpointWithEmptyQuery() {
        // Prepare test data with missing query
        Map<String, String> requestBody = new HashMap<>();
        
        // Execute test
        ResponseEntity<Map<String, Object>> response = chromaDBController.testQuery(requestBody);
        
        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(false, responseBody.get("success"));
        assertEquals("Query is required", responseBody.get("error"));
        
        // Verify no interactions with services
        verifyNoInteractions(embeddingServiceMock);
        verifyNoInteractions(chromaDBServiceMock);
    }
    
    @Test
    public void testQueryEndpointWithEmbeddingError() {
        // Prepare test data
        String testQuery = "test query";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", testQuery);
        
        // Configure mock to throw exception
        when(embeddingServiceMock.getEmbedding(testQuery))
            .thenThrow(new RuntimeException("Embedding service error"));
        
        // Execute test
        ResponseEntity<Map<String, Object>> response = chromaDBController.testQuery(requestBody);
        
        // Verify response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(false, responseBody.get("success"));
        assertEquals("Embedding service error", responseBody.get("error"));
        
        // Verify mock interactions
        verify(embeddingServiceMock).getEmbedding(testQuery);
        verifyNoInteractions(chromaDBServiceMock);
    }
}