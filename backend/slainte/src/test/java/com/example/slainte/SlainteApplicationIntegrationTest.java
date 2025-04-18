package com.example.slainte;

import com.example.slainte.controller.DeepseekController;
import com.example.slainte.controller.KnowledgeBaseController;
import com.example.slainte.controller.ChromaDBController;
import com.example.slainte.service.ChromaDBService;
import com.example.slainte.service.ChromaDBLowLevelService;
import com.example.slainte.service.DeepseekChatClient;
import com.example.slainte.service.EmbeddingService;
import com.example.slainte.service.KnowledgeBaseService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.main.allow-circular-references=true"
})
@ActiveProfiles("test")
public class SlainteApplicationIntegrationTest {

    @Autowired
    private ApplicationContext context;
    
    // Mock all external HTTP clients to prevent real API calls during tests
    @MockBean
    private RestTemplate restTemplate;
    
    @MockBean
    private WebClient.Builder webClientBuilder;
    
    // Mock external service dependencies
    @MockBean
    private EmbeddingService embeddingService;
    
    @MockBean
    private ChromaDBLowLevelService chromaDBLowLevelService;
    
    @MockBean
    private ChromaDBService chromaDBService;
    
    @MockBean
    private DeepseekChatClient deepseekChatClient;

    @Test
    void contextLoads() {
        // Verify application context loads successfully
        assertNotNull(context);
    }

    @Test
    void servicesAreAvailable() {
        // Verify the mocked services are properly registered
        assertNotNull(context.getBean(EmbeddingService.class));
        assertNotNull(context.getBean(ChromaDBService.class));
        assertNotNull(context.getBean(ChromaDBLowLevelService.class));
        assertNotNull(context.getBean(KnowledgeBaseService.class));
        assertNotNull(context.getBean(DeepseekChatClient.class));
    }

    @Test
    void controllersAreAvailable() {
        // Verify all controllers are properly initialized
        assertNotNull(context.getBean(DeepseekController.class));
        assertNotNull(context.getBean(KnowledgeBaseController.class));
        assertNotNull(context.getBean(ChromaDBController.class));
    }
}