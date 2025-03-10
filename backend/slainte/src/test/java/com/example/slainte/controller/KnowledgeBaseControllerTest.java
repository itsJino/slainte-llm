package com.example.slainte.controller;

import com.example.slainte.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KnowledgeBaseControllerTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private KnowledgeBaseController knowledgeBaseController;

    private Map<String, String> requestBody;
    private static final String TEST_QUERY = "test query";
    private static final String EXPECTED_RESULT = "Test search result";

    @BeforeEach
    public void setup() {
        requestBody = new HashMap<>();
        requestBody.put("query", TEST_QUERY);
    }

    @Test
    public void testSearch_Success() {
        // Setup
        when(knowledgeBaseService.search(TEST_QUERY)).thenReturn(EXPECTED_RESULT);

        // Execute
        String result = knowledgeBaseController.search(requestBody);

        // Verify
        assertEquals(EXPECTED_RESULT, result);
        verify(knowledgeBaseService).search(TEST_QUERY);
    }

    @Test
    public void testSearch_EmptyResult() {
        // Setup
        when(knowledgeBaseService.search(TEST_QUERY)).thenReturn("");

        // Execute
        String result = knowledgeBaseController.search(requestBody);

        // Verify
        assertEquals("", result);
        verify(knowledgeBaseService).search(TEST_QUERY);
    }

    @Test
    public void testSearch_ErrorResult() {
        // Setup
        String errorMessage = "Error: Database connection failed";
        when(knowledgeBaseService.search(TEST_QUERY)).thenReturn(errorMessage);

        // Execute
        String result = knowledgeBaseController.search(requestBody);

        // Verify
        assertEquals(errorMessage, result);
        verify(knowledgeBaseService).search(TEST_QUERY);
    }

    @Test
    public void testSearch_NullQuery() {
        // Setup - create request with null query
        Map<String, String> emptyRequest = new HashMap<>();
        
        // Execute - this should not throw an exception but pass null to service
        knowledgeBaseController.search(emptyRequest);

        // Verify
        verify(knowledgeBaseService).search(null);
    }
}