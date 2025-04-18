package com.example.slainte.controller;

import com.example.slainte.service.EmbeddingService;
import com.example.slainte.service.ChromaDBLowLevelService;
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
public class DiagnosticSearchControllerTest {

    @Mock
    private EmbeddingService embeddingServiceMock;
    
    @Mock
    private ChromaDBLowLevelService chromaDBServiceMock;
    
    private DiagnosticSearchController diagnosticSearchController;
    
    @BeforeEach
    public void setup() {
        diagnosticSearchController = new DiagnosticSearchController(
            embeddingServiceMock,
            chromaDBServiceMock
        );
    }
    
    @Test
    public void testDebugSearchSuccess() {
        // Prepare test data
        String query = "test query";
        Map<String, String> request = new HashMap<>();
        request.put("query", query);
        request.put("topK", "3");
        
        List<Double> mockEmbedding = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        Map<String, Object> rawResults = new HashMap<>();
        rawResults.put("documents", Arrays.asList(Arrays.asList("Document 1", "Document 2")));
        
        // Configure mocks
        when(embeddingServiceMock.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBServiceMock.getRawResults(mockEmbedding, 3)).thenReturn(rawResults);
        when(chromaDBServiceMock.queryDatabase(mockEmbedding, 3)).thenReturn("Text results");
        when(chromaDBServiceMock.extractDocuments(rawResults)).thenReturn(Arrays.asList("Document 1", "Document 2"));
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticSearchController.debugSearch(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals(query, responseBody.get("query"));
        assertEquals(3, responseBody.get("topK"));
        assertEquals(mockEmbedding.size(), responseBody.get("embeddingSize"));
        assertEquals("Text results", responseBody.get("textResults"));
        assertEquals(rawResults, responseBody.get("rawResults"));
        
        // Verify mock interactions
        verify(embeddingServiceMock).getEmbedding(query);
        verify(chromaDBServiceMock).getRawResults(mockEmbedding, 3);
        verify(chromaDBServiceMock).queryDatabase(mockEmbedding, 3);
        verify(chromaDBServiceMock).extractDocuments(rawResults);
    }
    
    @Test
    public void testDebugSearchWithDefaultTopK() {
        // Prepare test data with no topK specified
        String query = "test query";
        Map<String, String> request = new HashMap<>();
        request.put("query", query);
        
        List<Double> mockEmbedding = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        Map<String, Object> rawResults = new HashMap<>();
        rawResults.put("documents", Arrays.asList(Arrays.asList("Document 1", "Document 2")));
        
        // Configure mocks - should use default topK (5)
        when(embeddingServiceMock.getEmbedding(query)).thenReturn(mockEmbedding);
        when(chromaDBServiceMock.getRawResults(mockEmbedding, 5)).thenReturn(rawResults);
        when(chromaDBServiceMock.queryDatabase(mockEmbedding, 5)).thenReturn("Text results");
        when(chromaDBServiceMock.extractDocuments(rawResults)).thenReturn(Arrays.asList("Document 1", "Document 2"));
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticSearchController.debugSearch(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals(query, responseBody.get("query"));
        assertEquals(5, responseBody.get("topK")); // Default value
        
        // Verify mock interactions uses default topK
        verify(embeddingServiceMock).getEmbedding(query);
        verify(chromaDBServiceMock).getRawResults(mockEmbedding, 5);
        verify(chromaDBServiceMock).queryDatabase(mockEmbedding, 5);
    }
    
    @Test
    public void testDebugSearchWithError() {
        // Prepare test data
        String query = "test query";
        Map<String, String> request = new HashMap<>();
        request.put("query", query);
        
        // Configure mock to throw exception
        when(embeddingServiceMock.getEmbedding(query))
            .thenThrow(new RuntimeException("Embedding service error"));
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticSearchController.debugSearch(request);
        
        // Verify result
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals("Embedding service error", responseBody.get("error"));
        
        // Verify mock interactions
        verify(embeddingServiceMock).getEmbedding(query);
        verifyNoInteractions(chromaDBServiceMock);
    }
    
    @Test
    public void testListDocuments() {
        // Prepare mock data
        List<Double> genericEmbedding = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            genericEmbedding.add(0.1);
        }
        
        Map<String, Object> rawResults = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        documents.add(Arrays.asList("Document 1", "Document 2", "Document 3"));
        rawResults.put("documents", documents);
        
        List<List<Map<String, Object>>> metadatas = new ArrayList<>();
        List<Map<String, Object>> metaList = new ArrayList<>();
        metaList.add(Map.of("source", "source1"));
        metaList.add(Map.of("source", "source2"));
        metaList.add(Map.of("source", "source3"));
        metadatas.add(metaList);
        rawResults.put("metadatas", metadatas);
        
        // Configure mock
        when(chromaDBServiceMock.getRawResults(any(List.class), eq(20))).thenReturn(rawResults);
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticSearchController.listDocuments();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals(documents, responseBody.get("documents"));
        assertEquals(metadatas, responseBody.get("metadatas"));
        
        // Verify mock interactions
        verify(chromaDBServiceMock).getRawResults(any(List.class), eq(20));
    }
    
    @Test
    public void testSearchByKeyword() {
        // Prepare test data
        String keyword = "health";
        List<Double> mockEmbedding = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        
        Map<String, Object> rawResults = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        documents.add(Arrays.asList("Health document 1", "Health document 2"));
        rawResults.put("documents", documents);
        
        List<List<Map<String, Object>>> metadatas = new ArrayList<>();
        List<Map<String, Object>> metaList = new ArrayList<>();
        metaList.add(Map.of("source", "health_source1"));
        metaList.add(Map.of("source", "health_source2"));
        metadatas.add(metaList);
        rawResults.put("metadatas", metadatas);
        
        // Configure mocks
        when(embeddingServiceMock.getEmbedding(keyword)).thenReturn(mockEmbedding);
        when(chromaDBServiceMock.getRawResults(mockEmbedding, 10)).thenReturn(rawResults);
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticSearchController.searchByKeyword(keyword);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals(keyword, responseBody.get("keyword"));
        
        List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Health document 1", results.get(0).get("document"));
        assertEquals(Map.of("source", "health_source1"), results.get(0).get("metadata"));
        
        // Verify mock interactions
        verify(embeddingServiceMock).getEmbedding(keyword);
        verify(chromaDBServiceMock).getRawResults(mockEmbedding, 10);
    }
    
    @Test
    public void testSearchByKeywordWithError() {
        // Prepare test data
        String keyword = "health";
        
        // Configure mock to throw exception
        when(embeddingServiceMock.getEmbedding(keyword))
            .thenThrow(new RuntimeException("Embedding service error"));
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticSearchController.searchByKeyword(keyword);
        
        // Verify result
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals("Embedding service error", responseBody.get("error"));
        
        // Verify mock interactions
        verify(embeddingServiceMock).getEmbedding(keyword);
        verifyNoInteractions(chromaDBServiceMock);
    }
}