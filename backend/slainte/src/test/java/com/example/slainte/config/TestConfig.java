package com.example.slainte.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.Mockito.mock;

/**
 * Test configuration that provides mock beans for external dependencies.
 * This helps isolate tests from external services.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provides a mock RestTemplate for tests
     */
    @Bean
    @Primary
    public RestTemplate testRestTemplate() {
        return mock(RestTemplate.class);
    }
    
    /**
     * Provides a mock WebClient.Builder for tests
     */
    @Bean
    @Primary
    public WebClient.Builder testWebClientBuilder() {
        WebClient.Builder mockBuilder = mock(WebClient.Builder.class);
        WebClient mockWebClient = mock(WebClient.class);
        
        // Configure the mock to return itself for method chaining
        org.mockito.Mockito.when(mockBuilder.baseUrl(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(mockBuilder);
        org.mockito.Mockito.when(mockBuilder.build())
            .thenReturn(mockWebClient);
            
        return mockBuilder;
    }
}