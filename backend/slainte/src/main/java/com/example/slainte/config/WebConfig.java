package com.example.slainte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // ✅ Applies CORS to all API endpoints
                        .allowedOrigins(getAllowedOrigins())  // ✅ Fetch from properties or use defaults
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Content-Type", "Authorization") // ✅ Restrict to necessary headers
                        .allowCredentials(true); // ✅ Enable credentials (cookies, auth headers)
            }
        };
    }

    /**
     * ✅ Retrieves allowed origins from environment variables or uses a default value.
     */
    private String[] getAllowedOrigins() {
        String frontendUrl = System.getenv("FRONTEND_URL"); // ✅ Read from environment
        return frontendUrl != null ? new String[]{frontendUrl} : new String[]{"http://localhost:5173"};
    }
}
