package com.example.slainte.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChromaDBTestControllerTest {

    @Mock
    private RestTemplate restTemplateMock;
    
    private ChromaDBTestController chromaDBTestController;
    
    @BeforeEach
    public void setup() {
        chromaDBTestController = new ChromaDBTestController(restTemplateMock);
    }
    
    @Test
    public void testListCollections() {
        // Prepare mock data
        Map<String, Object> collectionsData = new HashMap<>();
        List<Map<String, Object>> collections = new ArrayList<>();
        Map<String, Object> collection = new HashMap<>();
        collection.put("name", "health_assistant");
        collection.put("id", "test-uuid");
        collections.add(collection);
        collectionsData.put("collections", collections);
        
        // Configure mock
        ResponseEntity<Object> mockResponse = new ResponseEntity<>(collectionsData, HttpStatus.OK);
        when(restTemplateMock.exchange(
            eq("http://localhost:8000/api/v1/collections"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenReturn(mockResponse);
        
        // Execute test
        ResponseEntity<Object> responseEntity = chromaDBTestController.listCollections();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(collectionsData, responseEntity.getBody());
        
        // Verify mock interactions
        verify(restTemplateMock).exchange(
            eq("http://localhost:8000/api/v1/collections"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Object.class)
        );
    }
    
    @Test
    public void testListCollectionsWithError() {
        // Configure mock to throw exception
        when(restTemplateMock.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenThrow(new RuntimeException("Connection error"));
        
        // Execute test
        ResponseEntity<Object> responseEntity = chromaDBTestController.listCollections();
        
        // Verify result
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, String> errorResponse = (Map<String, String>) responseEntity.getBody();
        assertEquals("Connection error", errorResponse.get("error"));
    }
    
    @Test
    public void testTestQuery() {
        // Prepare mock data
        Map<String, Object> queryResponse = new HashMap<>();
        queryResponse.put("documents", Arrays.asList(Arrays.asList("Document 1", "Document 2")));
        
        // Configure mock
        ResponseEntity<Object> mockResponse = new ResponseEntity<>(queryResponse, HttpStatus.OK);
        when(restTemplateMock.exchange(
            contains("/query"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenReturn(mockResponse);
        
        // Execute test
        ResponseEntity<Object> responseEntity = chromaDBTestController.testQuery();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(queryResponse, responseEntity.getBody());
        
        // Verify mock interactions
        verify(restTemplateMock).exchange(
            contains("/api/v1/collections/health_assistant/query"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        );
    }
    
    @Test
    public void testTestQueryWithError() {
        // Configure mock to throw exception
        when(restTemplateMock.exchange(
            contains("/query"),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenThrow(new RuntimeException("Query error"));
        
        // Execute test
        ResponseEntity<Object> responseEntity = chromaDBTestController.testQuery();
        
        // Verify result
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, String> errorResponse = (Map<String, String>) responseEntity.getBody();
        assertEquals("Query error", errorResponse.get("error"));
    }
    
    @Test
    public void testRawQuery() {
        // Prepare test data
        String jsonBody = "{\"query_embeddings\":[[0.1,0.2,0.3]],\"n_results\":1}";
        
        // Prepare mock data
        Map<String, Object> queryResponse = new HashMap<>();
        queryResponse.put("documents", Arrays.asList(Arrays.asList("Document 1")));
        
        // Configure mock
        ResponseEntity<Object> mockResponse = new ResponseEntity<>(queryResponse, HttpStatus.OK);
        when(restTemplateMock.exchange(
            contains("/query"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenReturn(mockResponse);
        
        // Execute test
        ResponseEntity<Object> responseEntity = chromaDBTestController.rawQuery(jsonBody);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(queryResponse, responseEntity.getBody());
        
        // Verify mock interactions - make sure the HTTP entity contains the raw JSON body
        verify(restTemplateMock).exchange(
            contains("/api/v1/collections/health_assistant/query"),
            eq(HttpMethod.POST),
            argThat(entity -> {
                String body = (String) entity.getBody();
                return body != null && body.equals(jsonBody);
            }),
            eq(Object.class)
        );
    }
    
    @Test
    public void testRawQueryWithError() {
        // Prepare test data
        String jsonBody = "{\"query_embeddings\":[[0.1,0.2,0.3]],\"n_results\":1}";
        
        // Configure mock to throw exception
        when(restTemplateMock.exchange(
            contains("/query"),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Object.class)
        )).thenThrow(new RuntimeException("Raw query error"));
        
        // Execute test
        ResponseEntity<Object> responseEntity = chromaDBTestController.rawQuery(jsonBody);
        
        // Verify result
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, String> errorResponse = (Map<String, String>) responseEntity.getBody();
        assertEquals("Raw query error", errorResponse.get("error"));
    }
}