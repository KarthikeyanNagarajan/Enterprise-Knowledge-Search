package com.karthik.eks.rag.service;

import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.document.model.DocumentChunk;
import com.karthik.eks.document.service.DocumentService;
import com.karthik.eks.rag.domain.VectorEmbedding;
import com.karthik.eks.rag.repository.VectorEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RAG (Retrieval Augmented Generation) Service - Core AI Module.
 * 
 * Responsibilities:
 * 1. Generate embeddings using Ollama LLM
 * 2. Perform vector similarity search
 * 3. Retrieve relevant context
 * 4. Construct prompts for LLM
 * 5. Call LLM with context
 * 
 * Architecture Constraints:
 * - Does NOT directly access Document repository
 * - Uses DocumentService interface to get chunks
 * - Can be extracted to microservice later
 * - Handles all AI logic in one place
 * 
 * Ollama Integration:
 * Configure Ollama endpoint in application.properties:
 * - ollama.base-url=http://localhost:11434
 * - ollama.embedding-model=nomic-embed-text:latest (or other embedding model)
 * - ollama.llm-model=llama2:latest (or other LLM)
 * 
 * Pull models using:
 * ollama pull nomic-embed-text
 * ollama pull llama2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final DocumentService documentService;
    private final VectorEmbeddingRepository vectorEmbeddingRepository;
    private final WebClient webClient;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.embedding-model:nomic-embed-text:latest}")
    private String embeddingModel;

    @Value("${ollama.llm-model:llama2:latest}")
    private String llmModel;

    private static final int SIMILARITY_SEARCH_LIMIT = 5;  // Top 5 context chunks

    /**
     * Generate embedding for text using Ollama.
     * 
     * Process:
     * 1. Send text to Ollama embedding endpoint
     * 2. Receive embedding vector (typically 384-768 dimensions)
     * 3. Store embedding in PostgreSQL pgvector
     * 4. Return float array
     * 
     * Ollama API Endpoint:
     * POST http://localhost:11434/api/embeddings
     * {
     *   "model": "nomic-embed-text:latest",
     *   "prompt": "text content here"
     * }
     * 
     * Response:
     * {
     *   "embedding": [0.1, 0.2, 0.3, ...]
     * }
     * 
     * @param text the text to embed
     * @return embedding vector as float array
     */
    public float[] generateEmbedding(String text) {
        log.info("Generating embedding for text length: {}", text.length());

        try {
            // Call Ollama embedding API
            EmbeddingRequest request = new EmbeddingRequest(embeddingModel, text);

            EmbeddingResponse response = webClient.post()
                    .uri(ollamaBaseUrl + "/api/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block();

            if (response == null || response.getEmbedding() == null) {
                throw new BusinessException("Failed to generate embedding", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.debug("Embedding generated with dimensions: {}", response.getEmbedding().length);
            return response.getEmbedding();

        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            throw new BusinessException("Failed to generate embedding: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Perform similarity search to find relevant context.
     * 
     * Process:
     * 1. Generate embedding for query
     * 2. Perform pgvector similarity search
     * 3. Retrieve associated chunks
     * 4. Sort by relevance
     * 5. Return top K results
     * 
     * @param query the user question
     * @param tenantId the current tenant
     * @return list of relevant context chunks, sorted by relevance (best first)
     */
    @Transactional(readOnly = true)
    public List<RelevantContext> similaritySearch(String query, Long tenantId) {
        log.info("Performing similarity search for query length: {}", query.length());

        try {
            // Generate query embedding
            float[] queryEmbedding = generateEmbedding(query);
            String queryVectorStr = formatVectorForPgvector(queryEmbedding);

            // Search vector database
            List<VectorEmbedding> similarEmbeddings = 
                    vectorEmbeddingRepository.findSimilarEmbeddings(queryVectorStr, tenantId, SIMILARITY_SEARCH_LIMIT);

            log.info("Found {} similar embeddings", similarEmbeddings.size());

            // Convert to RelevantContext with scores
            List<RelevantContext> results = new ArrayList<>();
            for (VectorEmbedding embedding : similarEmbeddings) {
                DocumentChunk chunk = documentService.getDocumentChunk(embedding.getChunkId())
                        .orElse(null);

                if (chunk != null) {
                    double relevanceScore = calculateCosineSimilarity(queryEmbedding, embedding.getEmbeddingArray());
                    
                    results.add(RelevantContext.builder()
                            .chunkId(embedding.getChunkId())
                            .content(chunk.getContent())
                            .relevanceScore(relevanceScore)
                            .build());
                }
            }

            return results;

        } catch (Exception e) {
            log.error("Error in similarity search: {}", e.getMessage(), e);
            throw new BusinessException("Similarity search failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate LLM response using contexts from RAG.
     * 
     * Process:
     * 1. Perform similarity search for relevant context
     * 2. Build system prompt with instructions
     * 3. Build user prompt with context and question
     * 4. Call Llama LLM
     * 5. Return response
     * 
     * Prompt Engineering:
     * Better system prompts lead to better responses!
     * 
     * @param question the user's question
     * @param tenantId the current tenant (for isolation)
     * @return AI response
     */
    @Transactional(readOnly = true)
    public String generateResponse(String question, Long tenantId) {
        log.info("Generating response for question: {}", question);

        try {
            // Step 1: Find relevant context
            List<RelevantContext> relevantContexts = similaritySearch(question, tenantId);
            
            if (relevantContexts.isEmpty()) {
                return "I don't have any relevant information to answer your question.";
            }

            // Step 2: Build context string from most relevant chunks
            StringBuilder contextBuilder = new StringBuilder();
            for (RelevantContext ctx : relevantContexts) {
                contextBuilder.append("- ").append(ctx.getContent()).append("\n\n");
            }

            // Step 3: Build prompt
            String systemPrompt = """
                    You are a helpful AI assistant that answers questions based on provided context.
                    If you cannot answer based on the context, say so.
                    Always be factual and cite the context when relevant.
                    """;

            String userPrompt = String.format("""
                    Context:
                    %s
                    
                    Question: %s
                    
                    Answer:
                    """, contextBuilder, question);

            // Step 4: Call Ollama LLM
            LlmRequest llmRequest = new LlmRequest(llmModel, systemPrompt + "\n" + userPrompt, false);

            LlmResponse llmResponse = webClient.post()
                    .uri(ollamaBaseUrl + "/api/generate")
                    .bodyValue(llmRequest)
                    .retrieve()
                    .bodyToMono(LlmResponse.class)
                    .block();

            if (llmResponse == null || llmResponse.getResponse() == null) {
                throw new BusinessException("Failed to generate response from LLM", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.info("Response generated successfully");
            return llmResponse.getResponse().trim();

        } catch (Exception e) {
            log.error("Error generating response: {}", e.getMessage(), e);
            throw new BusinessException("Failed to generate response: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Asynchronously generate a response to a user question using RAG.
     * 
     * Non-blocking LLM call useful for chat endpoints that benefit from async processing.
     * 
     * Usage:
     * <code>
     * CompletableFuture<String> future = ragService.generateResponseAsync(question, tenantId);
     * // returns immediately, LLM processing happens in thread pool
     * String response = future.get(); // wait for result
     * </code>
     * 
     * @param question the user's question
     * @param tenantId the current tenant (for isolation)
     * @return CompletableFuture containing the AI response
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<String> generateResponseAsync(String question, Long tenantId) {
        try {
            String response = generateResponse(question, tenantId);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Error generating response asynchronously", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Format vector array for pgvector database query.
     * pgvector expects format: "[0.1, 0.2, 0.3, ...]"
     * 
     * @param embedding the embedding array
     * @return formatted string for pgvector
     */
    private String formatVectorForPgvector(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Calculate cosine similarity between two vectors.
     * 
     * Cosine Similarity = (A · B) / (||A|| * ||B||)
     * Range: 0 to 1 (1 = identical, 0 = orthogonal)
     * 
     * @param vector1 first embedding
     * @param vector2 second embedding
     * @return cosine similarity score (0-1)
     */
    private double calculateCosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            normA += vector1[i] * vector1[i];
            normB += vector2[i] * vector2[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Result DTO for similarity search.
     */
    @lombok.Data
    @lombok.Builder
    public static class RelevantContext {
        private Long chunkId;
        private String content;
        private double relevanceScore;
    }

    /**
     * Ollama Embedding API Request.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EmbeddingRequest {
        private String model;
        private String prompt;
    }

    /**
     * Ollama Embedding API Response.
     */
    @lombok.Data
    public static class EmbeddingResponse {
        private float[] embedding;
    }

    /**
     * Ollama LLM API Request.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LlmRequest {
        private String model;
        private String prompt;
        private boolean stream;
    }

    /**
     * Ollama LLM API Response.
     */
    @lombok.Data
    public static class LlmResponse {
        private String response;
    }
}
