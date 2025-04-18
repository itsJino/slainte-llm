package com.example.slainte.controller;

import com.example.slainte.model.SearchRequest;
import com.example.slainte.model.SearchResponse;
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
public class KnowledgeBaseControllerTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseServiceMock;
    
    private KnowledgeBaseController knowledgeBaseController;
    
    @BeforeEach
    public void setup() {
        knowledgeBaseController = new KnowledgeBaseController(knowledgeBaseServiceMock);
    }
    
    @Test
    public void testBasicSearch() {
        // Prepare test data
        String query = "test query";
        Map<String, String> request = new HashMap<>();
        request.put("query", query);
        
        String expectedResult = "Test search results";
        
        // Configure mock
        when(knowledgeBaseServiceMock.search(query)).thenReturn(expectedResult);
        
        // Execute test
        String result = knowledgeBaseController.search(request);
        
        // Verify result
        assertEquals(expectedResult, result);
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).search(query);
    }
    
    @Test
    public void testAdvancedSearch() {
        // Prepare test data
        SearchRequest request = new SearchRequest();
        request.setQuery("advanced query");
        request.setTopK(5);
        
        String expectedResult = "Advanced search results";
        
        // Configure mock
        when(knowledgeBaseServiceMock.search("advanced query", 5)).thenReturn(expectedResult);
        
        // Execute test
        String result = knowledgeBaseController.advancedSearch(request);
        
        // Verify result
        assertEquals(expectedResult, result);
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).search("advanced query", 5);
    }
    
    @Test
    public void testStructuredSearch() {
        // Prepare test data
        SearchRequest request = new SearchRequest();
        request.setQuery("structured query");
        request.setTopK(3);
        
        SearchResponse mockResponse = new SearchResponse();
        // In a real test, we would set fields in the response, but since SearchResponse
        // doesn't have setters in the provided code, we're just testing the mechanics
        
        // Configure mock
        when(knowledgeBaseServiceMock.getSearchResults("structured query", 3)).thenReturn(mockResponse);
        
        // Execute test
        ResponseEntity<SearchResponse> responseEntity = knowledgeBaseController.structuredSearch(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(mockResponse, responseEntity.getBody());
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).getSearchResults("structured query", 3);
    }
    
    @Test
    public void testStructuredSearchWithDefaultTopK() {
        // Prepare test data with zero topK (should use default)
        SearchRequest request = new SearchRequest();
        request.setQuery("structured query");
        request.setTopK(0);
        
        SearchResponse mockResponse = new SearchResponse();
        
        // Configure mock - should use default topK (3)
        when(knowledgeBaseServiceMock.getSearchResults("structured query", 3)).thenReturn(mockResponse);
        
        // Execute test
        ResponseEntity<SearchResponse> responseEntity = knowledgeBaseController.structuredSearch(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(mockResponse, responseEntity.getBody());
        
        // Verify mock interactions uses default topK
        verify(knowledgeBaseServiceMock).getSearchResults("structured query", 3);
    }
    
    @Test
    public void testGetLastContextInfo() {
        // Prepare mock data
        Map<String, Object> contextInfo = new HashMap<>();
        contextInfo.put("context", "Last retrieved context");
        contextInfo.put("timestamp", "2023-04-09T12:00:00");
        
        // Configure mock
        when(knowledgeBaseServiceMock.getLastContextInfo()).thenReturn(contextInfo);
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = knowledgeBaseController.getLastContextInfo();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(contextInfo, responseEntity.getBody());
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).getLastContextInfo();
    }
    
    @Test
    public void testGetRawResults() {
        // Prepare mock data
        Map<String, Object> rawResults = new HashMap<>();
        rawResults.put("documents", Arrays.asList("doc1", "doc2"));
        rawResults.put("metadatas", Arrays.asList(Map.of("source", "source1"), Map.of("source", "source2")));
        
        // Configure mock
        when(knowledgeBaseServiceMock.getLastRawResults()).thenReturn(rawResults);
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = knowledgeBaseController.getRawResults();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(rawResults, responseEntity.getBody());
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).getLastRawResults();
    }
}