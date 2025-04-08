package com.example.slainte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;

@Configuration
@EnableCaching
public class AppConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))  // Connection timeout
            .setReadTimeout(Duration.ofSeconds(30))     // Read timeout
            .build();
    }
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public CacheManager cacheManager() {
        // Add chromaDirectResults to the cache names
        return new ConcurrentMapCacheManager(
            "embeddings", 
            "chromaResults", 
            "chromaDirectResults"
        );
    }
}