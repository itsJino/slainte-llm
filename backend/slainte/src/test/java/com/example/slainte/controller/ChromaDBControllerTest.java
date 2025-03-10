package com.example.slainte.controller;

import com.example.slainte.service.ChromaDBService;
import com.example.slainte.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private ChromaDBService chromaDBService;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private ChromaDBController chromaDBTestController;

    private List<Double> testEmbedding;
    private String mockChromaResponse;

    @BeforeEach
    public void setup() {
        // Create a test embedding with 512 dimensions
        testEmbedding = new ArrayList<>();
        for (int i = 0; i < 512; i++) {
            testEmbedding.add(0.1);
        }
        
        mockChromaResponse = "Sample document from ChromaDB";
    }

    @Test
    public void testTestChromaDB_Success() {
        // Setup
        when(chromaDBService.queryDatabase(anyList(), eq(3))).thenReturn(mockChromaResponse);

        // Execute
        ResponseEntity<Map<String, Object>> response = chromaDBTestController.testChromaDB();

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(true, responseBody.get("success"));
        assertEquals("ChromaDB test completed", responseBody.get("message"));
        assertEquals(mockChromaResponse, responseBody.get("response"));
        
        verify(chromaDBService).queryDatabase(anyList(), eq(3));
    }

    @Test
    public void testTestChromaDB_Exception() {
        // Setup - simulate an exception when calling ChromaDB
        when(chromaDBService.queryDatabase(anyList(), anyInt())).thenThrow(new RuntimeException("Test exception"));

        // Execute
        ResponseEntity<Map<String, Object>> response = chromaDBTestController.testChromaDB();

        // Verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(false, responseBody.get("success"));
        assertEquals("Test exception", responseBody.get("error"));
    }

    @Test
    public void testTestQuery_Success() {
        // Setup
        String query = "test query";
        Map<String, String> requestBody = Map.of("query", query);
        
        when(embeddingService.getEmbedding(query)).thenReturn(testEmbedding);
        when(chromaDBService.queryDatabase(testEmbedding, 3)).thenReturn(mockChromaResponse);

        // Execute
        ResponseEntity<Map<String, Object>> response = chromaDBTestController.testQuery(requestBody);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(true, responseBody.get("success"));
        assertEquals(query, responseBody.get("query"));
        assertEquals(512, responseBody.get("embeddingSize"));
        assertEquals(mockChromaResponse, responseBody.get("response"));
        
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBService).queryDatabase(testEmbedding, 3);
    }

    @Test
    public void testTestQuery_MissingQuery() {
        // Setup - request body without query
        Map<String, String> requestBody = new HashMap<>();

        // Execute
        ResponseEntity<Map<String, Object>> response = chromaDBTestController.testQuery(requestBody);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(false, responseBody.get("success"));
        assertEquals("Query is required", responseBody.get("error"));
        
        verify(embeddingService, never()).getEmbedding(anyString());
        verify(chromaDBService, never()).queryDatabase(anyList(), anyInt());
    }

    @Test
    public void testTestQuery_EmbeddingServiceException() {
        // Setup
        String query = "test query";
        Map<String, String> requestBody = Map.of("query", query);
        
        when(embeddingService.getEmbedding(query)).thenThrow(new RuntimeException("Embedding service error"));

        // Execute
        ResponseEntity<Map<String, Object>> response = chromaDBTestController.testQuery(requestBody);

        // Verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(false, responseBody.get("success"));
        assertEquals("Embedding service error", responseBody.get("error"));
    }
}