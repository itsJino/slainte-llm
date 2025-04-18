package com.example.slainte.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ChromaDBServiceTest {

    @Mock
    private HttpClient httpClientMock;
    
    @Mock
    private HttpResponse<String> httpResponseMock;
    
    private ChromaDBService chromaDBService;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    public void setup() throws Exception {
        // Create the service instance
        chromaDBService = new ChromaDBService();
        
        // Use reflection to replace the private httpClient with our mock
        java.lang.reflect.Field httpClientField = ChromaDBService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(chromaDBService, httpClientMock);
        
        // Default setup for mock response
        when(httpResponseMock.statusCode()).thenReturn(200);
        
        // Setup httpClientMock to return our mock response
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponseMock);
    }
    
    @Test
    public void testQueryDatabase_Success() throws Exception {
        // Prepare test data
        List<Double> testEmbedding = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            testEmbedding.add(0.1);
        }
        
        // Prepare mock response data
        Map<String, Object> responseMap = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        List<String> docList = new ArrayList<>();
        docList.add("Test document content");
        documents.add(docList);
        responseMap.put("documents", documents);
        
        List<List<Map<String, Object>>> metadatas = new ArrayList<>();
        List<Map<String, Object>> metaList = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test_source");
        metaList.add(metadata);
        metadatas.add(metaList);
        responseMap.put("metadatas", metadatas);
        
        // Configure the mock response
        when(httpResponseMock.body()).thenReturn(objectMapper.writeValueAsString(responseMap));
        
        // Execute the test
        String result = chromaDBService.queryDatabase(testEmbedding, 3);
        
        // Verify results
        assertNotNull(result);
        assertTrue(result.contains("Test document content"));
        assertTrue(result.contains("[Source: test_source]"));
        
        // Verify that the HttpClient was called with appropriate parameters
        verify(httpClientMock).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    public void testQueryDatabase_EmptyResponse() throws Exception {
        // Prepare test data
        List<Double> testEmbedding = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            testEmbedding.add(0.1);
        }
        
        // Prepare empty response
        Map<String, Object> responseMap = new HashMap<>();
        
        // Configure the mock response
        when(httpResponseMock.body()).thenReturn(objectMapper.writeValueAsString(responseMap));
        
        // Execute the test
        String result = chromaDBService.queryDatabase(testEmbedding, 3);
        
        // Verify results
        assertEquals("No documents in response.", result);
    }
    
    @Test
    public void testQueryDatabase_EmptyDocuments() throws Exception {
        // Prepare test data
        List<Double> testEmbedding = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            testEmbedding.add(0.1);
        }
        
        // Prepare response with empty documents
        Map<String, Object> responseMap = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        documents.add(new ArrayList<>()); // Empty document list
        responseMap.put("documents", documents);
        
        // Configure the mock response
        when(httpResponseMock.body()).thenReturn(objectMapper.writeValueAsString(responseMap));
        
        // Execute the test
        String result = chromaDBService.queryDatabase(testEmbedding, 3);
        
        // Verify results
        assertEquals("No documents found.", result);
    }
    
    @Test
    public void testQueryDatabase_Exception() throws Exception {
        // Prepare test data
        List<Double> testEmbedding = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            testEmbedding.add(0.1);
        }
        
        // Configure the mock to throw an exception
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Connection refused"));
        
        // Execute the test
        String result = chromaDBService.queryDatabase(testEmbedding, 3);
        
        // Verify results
        assertTrue(result.contains("Error querying ChromaDB"));
        assertTrue(result.contains("Connection refused"));
    }
    
    @Test
    public void testQueryDatabaseAsync_Success() throws Exception {
        // Prepare test data
        List<Double> testEmbedding = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            testEmbedding.add(0.1);
        }
        
        // Prepare mock response data
        Map<String, Object> responseMap = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        List<String> docList = new ArrayList<>();
        docList.add("Test document content");
        documents.add(docList);
        responseMap.put("documents", documents);
        
        // Configure the mock response
        when(httpResponseMock.body()).thenReturn(objectMapper.writeValueAsString(responseMap));
        
        // Execute the test
        CompletableFuture<String> futureResult = chromaDBService.queryDatabaseAsync(testEmbedding, 3);
        String result = futureResult.get(); // Blocks until complete
        
        // Verify results
        assertNotNull(result);
        assertTrue(result.contains("Test document content"));
    }
    
    @Test
    public void testProcessChromaDBResponse_MalformedResponse() {
        // Create a response body with an invalid format
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("documents", "This should be a list, not a string"); // Wrong type
        
        // Use reflection to access the private method
        try {
            java.lang.reflect.Method processMethod = ChromaDBService.class.getDeclaredMethod(
                "processChromaDBResponse", Map.class);
            processMethod.setAccessible(true);
            
            // Execute the method
            String result = (String) processMethod.invoke(chromaDBService, responseBody);
            
            // Verify result
            assertTrue(result.contains("Error processing search results"));
        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testQueryDatabase_ErrorResponse() throws Exception {
        // Prepare test data
        List<Double> testEmbedding = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            testEmbedding.add(0.1);
        }
        
        // Configure error response
        when(httpResponseMock.statusCode()).thenReturn(500);
        when(httpResponseMock.body()).thenReturn("{\"error\":\"Internal server error\"}");
        
        // Execute the test
        String result = chromaDBService.queryDatabase(testEmbedding, 3);
        
        // Verify results
        assertTrue(result.contains("Error from ChromaDB"));
    }
    
    @Test
    public void testQueryDatabase_SmallEmbedding() throws Exception {
        // Prepare test data with smaller than required embedding size
        List<Double> smallEmbedding = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            smallEmbedding.add(0.1);
        }
        
        // Prepare mock response data
        Map<String, Object> responseMap = new HashMap<>();
        List<List<String>> documents = new ArrayList<>();
        List<String> docList = new ArrayList<>();
        docList.add("Test document content");
        documents.add(docList);
        responseMap.put("documents", documents);
        
        // Configure the mock response
        when(httpResponseMock.body()).thenReturn(objectMapper.writeValueAsString(responseMap));
        
        // Execute the test - should pad the embedding
        String result = chromaDBService.queryDatabase(smallEmbedding, 3);
        
        // Verify results
        assertNotNull(result);
        assertTrue(result.contains("Test document content"));
        
        // Verify HTTP call
        verify(httpClientMock).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}