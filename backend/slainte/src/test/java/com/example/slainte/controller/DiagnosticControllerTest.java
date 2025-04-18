package com.example.slainte.controller;

import com.example.slainte.service.ChromaDBDirectClient;
import com.example.slainte.service.ChromaTestService;
import com.example.slainte.service.KnowledgeBaseService;
import com.example.slainte.service.RawChromaService;
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
public class DiagnosticControllerTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseServiceMock;
    
    @Mock
    private ChromaDBDirectClient chromaDBDirectClientMock;
    
    @Mock
    private RawChromaService rawChromaServiceMock;
    
    @Mock
    private ChromaTestService chromaTestServiceMock;
    
    private DiagnosticController diagnosticController;
    
    @BeforeEach
    public void setup() {
        diagnosticController = new DiagnosticController(
            knowledgeBaseServiceMock,
            chromaDBDirectClientMock,
            rawChromaServiceMock,
            chromaTestServiceMock
        );
    }
    
    @Test
    public void testChromaTest() {
        // Prepare mock data
        Map<String, Object> diagnostics = new HashMap<>();
        diagnostics.put("connection", "OK");
        diagnostics.put("heartbeat", "{\"nanosecond heartbeat\":1681048335699574016}");
        diagnostics.put("collection_exists", true);
        
        // Configure mock
        when(chromaDBDirectClientMock.testConnection()).thenReturn(diagnostics);
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticController.testChromaDB();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(diagnostics, responseEntity.getBody());
        
        // Verify mock interactions
        verify(chromaDBDirectClientMock).testConnection();
    }
    
    @Test
    public void testSearchTestSuccess() {
        // Prepare test data
        Map<String, String> request = new HashMap<>();
        request.put("query", "test query");
        
        // Configure mock
        when(knowledgeBaseServiceMock.search("test query", 1)).thenReturn("Test search results");
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticController.testSearch(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals(true, responseBody.get("success"));
        assertEquals("test query", responseBody.get("query"));
        assertEquals("Test search results", responseBody.get("result"));
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).search("test query", 1);
    }
    
    @Test
    public void testSearchTestWithDefaultQuery() {
        // Prepare test data with no query specified
        Map<String, String> request = new HashMap<>();
        
        // Configure mock
        when(knowledgeBaseServiceMock.search("test query", 1)).thenReturn("Test search results");
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticController.testSearch(request);
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals(true, responseBody.get("success"));
        assertEquals("test query", responseBody.get("query"));
        assertEquals("Test search results", responseBody.get("result"));
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).search("test query", 1);
    }
    
    @Test
    public void testSearchTestWithError() {
        // Prepare test data
        Map<String, String> request = new HashMap<>();
        request.put("query", "test query");
        
        // Configure mock to throw exception
        when(knowledgeBaseServiceMock.search("test query", 1))
            .thenThrow(new RuntimeException("Search error"));
        
        // Execute test
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticController.testSearch(request);
        
        // Verify result
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertEquals(false, responseBody.get("success"));
        assertEquals("Search error", responseBody.get("error"));
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).search("test query", 1);
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
        ResponseEntity<Map<String, Object>> responseEntity = diagnosticController.getLastContextInfo();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(contextInfo, responseEntity.getBody());
        
        // Verify mock interactions
        verify(knowledgeBaseServiceMock).getLastContextInfo();
    }
    
    @Test
    public void testMinimalTest() {
        // Configure mock
        when(chromaTestServiceMock.runMinimalTest()).thenReturn("Minimal test result");
        
        // Execute test
        ResponseEntity<String> responseEntity = diagnosticController.minimalTest();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Minimal test result", responseEntity.getBody());
        
        // Verify mock interactions
        verify(chromaTestServiceMock).runMinimalTest();
    }
    
    @Test
    public void testHeartbeatTest() {
        // Configure mock
        when(chromaTestServiceMock.testHeartbeat()).thenReturn("Heartbeat test result");
        
        // Execute test
        ResponseEntity<String> responseEntity = diagnosticController.heartbeatTest();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Heartbeat test result", responseEntity.getBody());
        
        // Verify mock interactions
        verify(chromaTestServiceMock).testHeartbeat();
    }
    
    @Test
    public void testRawQueryTest() {
        // Configure mock
        when(rawChromaServiceMock.testHardcodedQuery()).thenReturn("Raw query test result");
        
        // Execute test
        ResponseEntity<String> responseEntity = diagnosticController.rawQueryTest();
        
        // Verify result
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Raw query test result", responseEntity.getBody());
        
        // Verify mock interactions
        verify(rawChromaServiceMock).testHardcodedQuery();
    }
}