package com.karthik.eks.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI configuration for the parallel RAG implementation.
 *
 * <p>This configuration exists alongside the existing manual Ollama WebClient pipeline.
 * It gives the project a Spring AI-backed chat client that can be used by the side-by-side
 * RAG service while preserving the original integration path for comparison and rollback.
 */
@Configuration
public class SpringAiConfig {

    /**
     * Build a ChatClient configured with a strict system prompt.
     *
     * @param chatClientBuilder the auto-configured ChatClient builder supplied by Spring AI
     * @return a ready-to-use ChatClient that answers only from context
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .defaultSystem("""
                        You are a helpful retrieval assistant.
                        Answer only from the provided context.
                        If the provided context is insufficient to answer the question, say so explicitly and do not guess.
                        """)
                .build();
    }
}
