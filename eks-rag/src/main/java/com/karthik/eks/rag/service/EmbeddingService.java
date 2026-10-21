package com.karthik.eks.rag.service;

import com.karthik.eks.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * EmbeddingService - Generates text embeddings using Ollama LLM.
 * 
 * Purpose:
 * - Converts text queries and document chunks into vector embeddings
 * - Integrates with Ollama embedding API
 * - Caches embeddings for performance (optional)
 * - Handles embedding generation errors gracefully
 * 
 * Configuration:
 * Set in application.properties:
 * - rag.ollama.base-url=http://localhost:11434
 * - rag.ollama.embedding-model=nomic-embed-text:latest
 * 
 * Model Details:
 * - nomic-embed-text: 768-dimensional embeddings, optimized for general text
 * - Other options: all-MiniLM-L6-v2, sentence-transformers, etc.
 * 
 * API Endpoint:
 * POST /api/embeddings
 * Request: { "model": "nomic-embed-text:latest", "prompt": "Your text here" }
 * Response: { "embedding": [0.1, 0.2, ...] }
 * 
 * Performance:
 * - Embedding generation is relatively fast (~100ms-500ms per text)
 * - Results should be cached to avoid regenerating for same text
 * - Batch embedding API could be used for bulk operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final WebClient webClient;

    @Value("${rag.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${rag.ollama.embedding-model:nomic-embed-text:latest}")
    private String embeddingModel;

    /**
     * Generate an embedding for the given text using Ollama.
     * 
     * Steps:
     * 1. Validate input text
     * 2. Create embedding request
     * 3. Call Ollama API
     * 4. Parse and return embedding vector
     * 
     * @param text the text to embed
     * @return float array representing the embedding vector (typically 768-dimensional)
     * @throws BusinessException if embedding generation fails
     */
    public float[] generateEmbedding(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());

        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException("Cannot generate embedding for empty text", HttpStatus.BAD_REQUEST);
        }

        try {
            // Create request
            Map<String, String> request = Map.of(
                    "model", embeddingModel,
                    "prompt", text.trim()
            );

            log.debug("Calling Ollama API at {} with model {}", ollamaBaseUrl, embeddingModel);

            // Call Ollama embedding API
            EmbeddingResponse response = webClient.post()
                    .uri(ollamaBaseUrl + "/api/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block();

            if (response == null || response.getEmbedding() == null) {
                throw new BusinessException("No embedding returned from Ollama API", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.debug("Successfully generated embedding with dimension: {}", response.getEmbedding().length);
            return response.getEmbedding();

        } catch (Exception e) {
            log.error("Failed to generate embedding for text of length: {}", text.length(), e);
            throw new BusinessException("Embedding generation failed: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Asynchronously generate embeddings for multiple texts.
     * 
     * Non-blocking batch embedding generation using @Async.
     * Useful when processing document chunks in the background.
     * 
     * Usage:
     * <code>
     * CompletableFuture<List<float[]>> future = embeddingService.generateEmbeddingsBatch(texts);
     * // returns immediately, processing happens in thread pool
     * List<float[]> embeddings = future.get(); // wait for result
     * </code>
     * 
     * @param texts list of texts to embed
     * @return CompletableFuture containing list of embeddings
     */
    @Async
    public CompletableFuture<List<float[]>> generateEmbeddingsBatch(List<String> texts) {
        log.info("Starting batch embedding generation for {} texts", texts.size());
        
        try {
            List<float[]> embeddings = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);
                try {
                    float[] embedding = generateEmbedding(text);
                    embeddings.add(embedding);
                    if ((i + 1) % 10 == 0) {
                        log.debug("Progress: {}/{} embeddings generated", i + 1, texts.size());
                    }
                } catch (Exception e) {
                    log.warn("Failed to generate embedding for text {}: {}", i, e.getMessage());
                    // Continue with other texts instead of failing entire batch
                }
            }
            
            log.info("Batch embedding generation completed: {}/{} successful", embeddings.size(), texts.size());
            return CompletableFuture.completedFuture(embeddings);
        } catch (Exception e) {
            log.error("Batch embedding generation failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * EmbeddingResponse DTO - Response from Ollama embedding API.
     */
    public static class EmbeddingResponse {
        private float[] embedding;
        private String model;
        private Long totalDuration;
        private Long loadDuration;
        private Long promptEvalCount;

        public float[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Long getTotalDuration() {
            return totalDuration;
        }

        public void setTotalDuration(Long totalDuration) {
            this.totalDuration = totalDuration;
        }

        public Long getLoadDuration() {
            return loadDuration;
        }

        public void setLoadDuration(Long loadDuration) {
            this.loadDuration = loadDuration;
        }

        public Long getPromptEvalCount() {
            return promptEvalCount;
        }

        public void setPromptEvalCount(Long promptEvalCount) {
            this.promptEvalCount = promptEvalCount;
        }
    }
}
