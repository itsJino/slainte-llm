package com.example.slainte.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChromaDBServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ChromaDBService chromaDBService;

    private List<Double> testEmbedding;

    @BeforeEach
    public void setup() {
        testEmbedding = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            testEmbedding.add(0.1 * i);
        }
    }

    @Test
    public void testQueryDatabase_Success() {
        // Setup mock response from ChromaDB
        Map<String, Object> responseBody = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        List<String> docList = Arrays.asList("Document 1", "Document 2");
        documents.add(docList);
        responseBody.put("documents", documents);
        
        List<List<Double>> distances = new ArrayList<>();
        List<Double> distList = Arrays.asList(0.1, 0.2);
        distances.add(distList);
        responseBody.put("distances", distances);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Execute
        String result = chromaDBService.queryDatabase(testEmbedding, 2);

        // Verify
        assertNotNull(result);
        assertTrue(result.contains("Document 1"));
        assertTrue(result.contains("Document 2"));
        
        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    public void testQueryDatabase_EmptyResponse() {
        // Setup empty response
        Map<String, Object> responseBody = new HashMap<>();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Execute
        String result = chromaDBService.queryDatabase(testEmbedding, 2);

        // Verify
        assertEquals("No documents in response.", result);
    }

    @Test
    public void testQueryDatabase_EmptyDocuments() {
        // Setup response with empty documents
        Map<String, Object> responseBody = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        documents.add(new ArrayList<>()); // Empty document list
        responseBody.put("documents", documents);
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Execute
        String result = chromaDBService.queryDatabase(testEmbedding, 2);

        // Verify
        assertEquals("No documents found.", result);
    }

    @Test
    public void testQueryDatabase_Exception() {
        // Setup exception being thrown
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Execute
        String result = chromaDBService.queryDatabase(testEmbedding, 2);

        // Verify
        assertTrue(result.startsWith("Error querying ChromaDB:"));
    }

    @Test
    public void testQueryDatabaseAsync_Success() throws ExecutionException, InterruptedException {
        // Setup mock response from ChromaDB
        Map<String, Object> responseBody = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        List<String> docList = Arrays.asList("Document 1", "Document 2");
        documents.add(docList);
        responseBody.put("documents", documents);
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Execute
        CompletableFuture<String> futureResult = chromaDBService.queryDatabaseAsync(testEmbedding, 2);
        String result = futureResult.get(); // Blocks until complete

        // Verify
        assertNotNull(result);
        assertTrue(result.contains("Document 1"));
        assertTrue(result.contains("Document 2"));
    }

    @Test
    public void testProcessChromaDBResponse_MalformedResponse() {
        // Setup malformed response that will cause exception during processing
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("documents", "not a list"); // Wrong type to cause exception
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Execute
        String result = chromaDBService.queryDatabase(testEmbedding, 2);

        // Verify
        assertTrue(result.startsWith("Error processing search results:"));
    }
}