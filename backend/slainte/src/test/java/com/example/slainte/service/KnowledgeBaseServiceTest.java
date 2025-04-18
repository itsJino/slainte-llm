package com.example.slainte.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.slainte.model.SearchResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KnowledgeBaseServiceTest {

    @Mock
    private EmbeddingService embeddingService;
    
    @Mock
    private ChromaDBLowLevelService chromaDBLowLevelService;
    
    private KnowledgeBaseService knowledgeBaseService;
    
    private List<Double> mockEmbedding;
    private String mockChromaResult;
    private Map<String, Object> mockRawResults;

    @BeforeEach
    public void setup() {
        // Manually create KnowledgeBaseService with mocked dependencies
        knowledgeBaseService = new KnowledgeBaseService(embeddingService, chromaDBLowLevelService);
        
        // Setup test data
        mockEmbedding = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mockEmbedding.add(0.1 * i);
        }
        
        mockChromaResult = "Test document content from ChromaDB";
        
        // Set up mock raw results
        mockRawResults = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        List<String> docList = new ArrayList<>();
        docList.add("Test document content");
        documents.add(docList);
        mockRawResults.put("documents", documents);
    }

    @Test
    public void testSearch_Success() {
        // Setup
        String query = "test query";
        when(embeddingService.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBLowLevelService.queryDatabase(mockEmbedding, 8)).thenReturn(mockChromaResult);
        when(chromaDBLowLevelService.getRawResults(mockEmbedding, 8)).thenReturn(mockRawResults);

        // Execute
        String result = knowledgeBaseService.search(query);

        // Verify
        assertEquals(mockChromaResult, result);
        assertEquals(mockChromaResult, knowledgeBaseService.getLastRetrievedContext());
        
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBLowLevelService).queryDatabase(mockEmbedding, 8);
        verify(chromaDBLowLevelService).getRawResults(mockEmbedding, 8);
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
        verify(chromaDBLowLevelService, never()).queryDatabase(any(), anyInt());
        verify(chromaDBLowLevelService, never()).getRawResults(any(), anyInt());
    }

    @Test
    public void testSearch_ChromaDBException() {
        // Setup
        String query = "test query";
        String errorMessage = "ChromaDB connection failed";
        
        when(embeddingService.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBLowLevelService.getRawResults(mockEmbedding, 8))
            .thenThrow(new RuntimeException(errorMessage));

        // Execute
        String result = knowledgeBaseService.search(query);

        // Verify
        assertEquals("Error: " + errorMessage, result);
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBLowLevelService).getRawResults(mockEmbedding, 8);
        verify(chromaDBLowLevelService, never()).queryDatabase(any(), anyInt());
    }

    @Test
    public void testSearchParallel_Success() throws ExecutionException, InterruptedException {
        // Setup
        String query = "test query";
        when(embeddingService.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBLowLevelService.queryDatabase(mockEmbedding, 8)).thenReturn(mockChromaResult);
        when(chromaDBLowLevelService.getRawResults(mockEmbedding, 8)).thenReturn(mockRawResults);

        // Execute
        CompletableFuture<String> futureResult = knowledgeBaseService.searchParallel(query, 8);
        String result = futureResult.get(); // Blocks until complete

        // Verify
        assertEquals(mockChromaResult, result);
        
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBLowLevelService).queryDatabase(mockEmbedding, 8);
        verify(chromaDBLowLevelService).getRawResults(mockEmbedding, 8);
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
        assertTrue(result.contains("Failed to generate embedding"));
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBLowLevelService, never()).queryDatabase(any(), anyInt());
        verify(chromaDBLowLevelService, never()).getRawResults(any(), anyInt());
    }

    @Test
    public void testGetLastContextInfo() {
        // Setup - manually set the lastRetrievedContext using reflection
        try {
            java.lang.reflect.Field lastContextField = 
                KnowledgeBaseService.class.getDeclaredField("lastRetrievedContext");
            lastContextField.setAccessible(true);
            lastContextField.set(knowledgeBaseService, "Test context information");
            
            Map<String, Object> rawResults = new HashMap<>();
            List<List<String>> documents = new ArrayList<>();
            documents.add(Arrays.asList("Document 1", "Document 2"));
            rawResults.put("documents", documents);
            
            java.lang.reflect.Field rawResultsField = 
                KnowledgeBaseService.class.getDeclaredField("lastRawResults");
            rawResultsField.setAccessible(true);
            rawResultsField.set(knowledgeBaseService, rawResults);
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
        
        // Execute
        Map<String, Object> contextInfo = knowledgeBaseService.getLastContextInfo();
        
        // Verify
        assertNotNull(contextInfo);
        assertEquals("Test context information", contextInfo.get("context"));
        assertEquals(23, contextInfo.get("contextLength"));
        assertEquals("Test context information", contextInfo.get("preview"));
        assertEquals(2, contextInfo.get("documentCount"));
    }
    
    @Test
    public void testGetSearchResults() {
        // Setup
        String query = "test query";
        int topK = 3;
        
        when(embeddingService.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBLowLevelService.getRawResults(mockEmbedding, topK)).thenReturn(mockRawResults);
        when(chromaDBLowLevelService.queryDatabase(mockEmbedding, topK)).thenReturn(mockChromaResult);
        
        List<String> documents = Arrays.asList("Document 1", "Document 2", "Document 3");
        when(chromaDBLowLevelService.extractDocuments(mockRawResults)).thenReturn(documents);
        
        // Execute
        SearchResponse response = knowledgeBaseService.getSearchResults(query, topK);
        
        // Verify
        assertNotNull(response);
        
        // Verify method calls
        verify(embeddingService).getEmbedding(query);
        verify(chromaDBLowLevelService).getRawResults(mockEmbedding, topK);
        verify(chromaDBLowLevelService).queryDatabase(mockEmbedding, topK);
        verify(chromaDBLowLevelService).extractDocuments(mockRawResults);
    }
    
    @Test
    public void testGetLastRawResults() {
        // Setup using reflection
        Map<String, Object> rawResults = new HashMap<>();
        rawResults.put("testKey", "testValue");
        
        try {
            java.lang.reflect.Field field = 
                KnowledgeBaseService.class.getDeclaredField("lastRawResults");
            field.setAccessible(true);
            field.set(knowledgeBaseService, rawResults);
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
        
        // Execute
        Map<String, Object> results = knowledgeBaseService.getLastRawResults();
        
        // Verify
        assertNotNull(results);
        assertEquals("testValue", results.get("testKey"));
    }
}