package com.karthik.eks.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Main Spring Boot Configuration.
 * 
 * Sets up:
 * - WebClient for async HTTP calls to Ollama
 * - BCrypt password encoder for security
 * - Async task execution for long-running operations
 * - In-memory caching for embeddings and search results
 */
@Configuration
@EnableAsync
@EnableCaching
public class AppConfig {

    /**
     * WebClient bean for calling Ollama API.
     * 
     * Non-blocking HTTP client for better async performance.
     * Used by RAG service to call Ollama embeddings and LLM.
     * 
     * @return WebClient instance
     */
    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

    /**
     * Password encoder bean using BCrypt.
     * 
     * BCrypt automatically handles:
     * - Salt generation
     * - Hash iteration (cost factor)
     * - Password verification
     * 
     * Never store plain text passwords!
     * 
     * @return PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
