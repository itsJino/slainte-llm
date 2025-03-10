package com.example.slainte.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KnowledgeBaseServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ChromaDBService chromaDBService;

    @InjectMocks
    private KnowledgeBaseService knowledgeBaseService;

    private List<Double> mockEmbedding;
    private String mockChromaResult;

    @BeforeEach
    public void setup() {
        mockEmbedding = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mockEmbedding.add(0.1 * i);
        }
        
        mockChromaResult = "Test document content from ChromaDB";
    }

    @Test
    public void testSearch_Success() {
        // Setup
        String query = "test query";
        when(embeddingService.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBService.queryDatabase(mockEmbedding, 8)).thenReturn(mockChromaResult);

        // Execute
        String result = knowledgeBaseService.search(query);

        // Verify
        assertEquals(mockChromaResult, result);
        assertEquals(mockChromaResult, knowledgeBaseService.getLastRetrievedContext());
        
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBService).queryDatabase(mockEmbedding, 8);
    }

    @Test
    public void testSearch_EmbeddingFailure() {
        // Setup
        String query = "test query";
        when(embeddingService.getEmbedding(query)).thenReturn(new ArrayList<>()); // Empty embedding
        
        // Execute
        String result = knowledgeBaseService.search(query);

        // Verify
        assertTrue(result.startsWith("Error:"));
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBService, never()).queryDatabase(any(), anyInt());
    }

    @Test
    public void testSearch_ChromaDBException() {
        // Setup
        String query = "test query";
        String errorMessage = "ChromaDB connection failed";
        
        when(embeddingService.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBService.queryDatabase(mockEmbedding, 8))
            .thenThrow(new RuntimeException(errorMessage));

        // Execute
        String result = knowledgeBaseService.search(query);

        // Verify
        assertEquals("Error: " + errorMessage, result);
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBService).queryDatabase(mockEmbedding, 8);
    }

    @Test
    public void testSearchParallel_Success() throws ExecutionException, InterruptedException {
        // Setup
        String query = "test query";
        when(embeddingService.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBService.queryDatabase(mockEmbedding, 5)).thenReturn(mockChromaResult);

        // Execute
        CompletableFuture<String> futureResult = knowledgeBaseService.searchParallel(query);
        String result = futureResult.get(); // Blocks until complete

        // Verify
        assertEquals(mockChromaResult, result);
        assertEquals(mockChromaResult, knowledgeBaseService.getLastRetrievedContext());
        
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBService).queryDatabase(mockEmbedding, 5);
    }

    @Test
    public void testSearchParallel_EmbeddingFailure() throws ExecutionException, InterruptedException {
        // Setup
        String query = "test query";
        when(embeddingService.getEmbedding(query)).thenReturn(new ArrayList<>()); // Empty embedding
        
        // Execute
        CompletableFuture<String> futureResult = knowledgeBaseService.searchParallel(query);
        String result = futureResult.get(); // Blocks until complete

        // Verify
        assertEquals("Error: Failed to generate embedding.", result);
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBService, never()).queryDatabase(any(), anyInt());
    }

    @Test
    public void testGetLastContextInfo() {
        // Setup - set a known context
        String context = "Test context information";
        when(embeddingService.getEmbedding("test")).thenReturn(mockEmbedding);
        when(chromaDBService.queryDatabase(mockEmbedding, 8)).thenReturn(context);
        knowledgeBaseService.search("test"); // This will set the last context
        
        // Execute
        Map<String, Object> contextInfo = knowledgeBaseService.getLastContextInfo();
        
        // Verify
        assertNotNull(contextInfo);
        assertEquals(context, contextInfo.get("context"));
        assertEquals(context.length(), contextInfo.get("contextLength"));
        assertNotNull(contextInfo.get("timestamp"));
    }
}