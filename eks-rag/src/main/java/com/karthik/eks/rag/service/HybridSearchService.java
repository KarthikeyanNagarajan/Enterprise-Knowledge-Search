package com.karthik.eks.rag.service;

import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.document.model.DocumentChunk;
import com.karthik.eks.document.repository.DocumentChunkRepository;
import com.karthik.eks.rag.dto.SearchResultDto;
import com.karthik.eks.rag.domain.VectorEmbedding;
import com.karthik.eks.rag.repository.VectorEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HybridSearchService - Combines vector and keyword search for better retrieval quality.
 * 
 * Purpose:
 * - Improves recall by combining semantic (vector) and keyword-based search
 * - Implements a weighted scoring system to balance both approaches
 * - Provides detailed score breakdown for transparency and debugging
 * 
 * Architecture:
 * - Performs vector search using pgvector embeddings
 * - Performs keyword search using full-text or LIKE queries
 * - Merges results and deduplicates by chunk ID
 * - Applies configurable weights to combine scores
 * - Sorts by final score
 * 
 * Benefits:
 * - Vector search: catches semantically similar content even with different keywords
 * - Keyword search: ensures exact keyword matches appear high in results
 * - Hybrid: provides robustness against both synonyms and exact phrases
 * 
 * Configuration:
 * - rag.search.vector-weight=0.7 (importance of semantic similarity)
 * - rag.search.keyword-weight=0.3 (importance of keyword match)
 * - rag.search.vector-result-limit=10 (top K results from vector search)
 * - rag.search.keyword-result-limit=10 (top K results from keyword search)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final VectorEmbeddingRepository vectorEmbeddingRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;

    @Value("${rag.search.vector-weight:0.7}")
    private Double vectorWeight;

    @Value("${rag.search.keyword-weight:0.3}")
    private Double keywordWeight;

    @Value("${rag.search.vector-result-limit:10}")
    private Integer vectorResultLimit;

    @Value("${rag.search.keyword-result-limit:10}")
    private Integer keywordResultLimit;

    /**
     * Perform hybrid search combining vector and keyword search.
     * 
     * Steps:
     * 1. Generate embedding for the query
     * 2. Perform vector similarity search
     * 3. Perform keyword search on chunk content
     * 4. Merge results, deduplicating by chunk ID
     * 5. Calculate combined scores
     * 6. Sort by final score descending
     * 
     * @param query the search query text
     * @param tenantId the tenant ID for multi-tenancy
     * @return list of search results sorted by relevance (hybrid score)
     * @throws BusinessException if the query is invalid or search fails
     */
    @Transactional(readOnly = true)
    public List<SearchResultDto> hybridSearch(String query, Long tenantId) {
        log.info("Starting hybrid search for query='{}' tenantId={}", query, tenantId);

        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException("Search query cannot be empty", HttpStatus.BAD_REQUEST);
        }

        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("Invalid tenant ID", HttpStatus.BAD_REQUEST);
        }

        try {
            // Step 1: Vector search
            log.debug("Performing vector search");
            List<SearchResultDto> vectorResults = vectorSearch(query, tenantId);
            log.debug("Vector search returned {} results", vectorResults.size());

            // Step 2: Keyword search
            log.debug("Performing keyword search");
            List<SearchResultDto> keywordResults = keywordSearch(query, tenantId);
            log.debug("Keyword search returned {} results", keywordResults.size());

            // Step 3: Merge and deduplicate results
            Map<Long, SearchResultDto> mergedResults = mergeResults(vectorResults, keywordResults);
            log.debug("Merged results contain {} unique chunks", mergedResults.size());

            // Step 4: Sort by final score
            List<SearchResultDto> finalResults = mergedResults.values().stream()
                    .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
                    .collect(Collectors.toList());

            log.info("Hybrid search completed: returned {} results for query='{}'", 
                    finalResults.size(), query);
            return finalResults;

        } catch (Exception e) {
            log.error("Hybrid search failed for query='{}' tenantId={}", query, tenantId, e);
            throw new BusinessException("Search operation failed: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Perform vector similarity search using embeddings.
     * 
     * Steps:
     * 1. Generate embedding for the query
     * 2. Search pgvector for similar embeddings
     * 3. Retrieve associated chunks and documents
     * 4. Calculate scores and create result DTOs
     * 
     * @param query the search query text
     * @param tenantId the tenant ID
     * @return list of vector search results with vector scores
     */
    private List<SearchResultDto> vectorSearch(String query, Long tenantId) {
        log.debug("Generating query embedding for: {}", query);

        try {
            // Generate embedding for the query
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String queryVectorStr = formatVectorForPgvector(queryEmbedding);

            // Search for similar embeddings
            List<VectorEmbedding> similarEmbeddings = vectorEmbeddingRepository
                    .findSimilarEmbeddings(queryVectorStr, tenantId, vectorResultLimit);

            log.debug("Found {} similar embeddings", similarEmbeddings.size());

            // Convert to search results
            List<SearchResultDto> results = new ArrayList<>();
            for (int i = 0; i < similarEmbeddings.size(); i++) {
                VectorEmbedding embedding = similarEmbeddings.get(i);
                float[] embeddingArray = parseEmbedding(embedding.getEmbedding());

                // Calculate cosine similarity (between 0 and 1)
                double vectorScore = calculateCosineSimilarity(queryEmbedding, embeddingArray);

                // Retrieve chunk and document for context
                DocumentChunk chunk = documentChunkRepository.findById(embedding.getChunkId())
                        .orElse(null);

                if (chunk != null) {
                    SearchResultDto result = convertChunkToSearchResult(chunk, vectorScore, 0.0);
                    result.setVectorScore(vectorScore);
                    results.add(result);
                }
            }

            return results;

        } catch (Exception e) {
            log.error("Vector search failed", e);
            throw new BusinessException("Vector search failed: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Perform keyword search on chunk content.
     * 
     * Steps:
     * 1. Search chunks for content matching the query (case-insensitive LIKE)
     * 2. Calculate keyword match score based on matches found
     * 3. Create result DTOs with keyword scores
     * 
     * Scoring:
     * - Keyword score = (number of query terms found) / (total query terms)
     * - Higher score if more query words appear in the content
     * 
     * @param query the search query text
     * @param tenantId the tenant ID
     * @return list of keyword search results with keyword scores
     */
    private List<SearchResultDto> keywordSearch(String query, Long tenantId) {
        log.debug("Starting keyword search for query: {}", query);

        try {
            // Split query into keywords
            String[] keywords = query.toLowerCase().split("\\s+");
            log.debug("Query keywords: {}", Arrays.toString(keywords));

            // Get all embedded chunks for the tenant (for keyword search)
            // In production, this should use a full-text search index
            // For now, we retrieve all chunks and filter in memory
            List<DocumentChunk> allChunks = documentChunkRepository.findByEmbeddingStatusAndTenantId(
                    DocumentChunk.EmbeddingStatus.EMBEDDED, tenantId, 
                    org.springframework.data.domain.PageRequest.of(0, keywordResultLimit * 5)
            ).getContent();

            log.debug("Searching {} chunks for keywords", allChunks.size());

            List<SearchResultDto> results = new ArrayList<>();

            for (DocumentChunk chunk : allChunks) {
                String chunkContent = chunk.getContent().toLowerCase();

                // Count keyword matches
                int matchCount = 0;
                for (String keyword : keywords) {
                    if (chunkContent.contains(keyword)) {
                        matchCount++;
                    }
                }

                // Calculate keyword score
                if (matchCount > 0) {
                    double keywordScore = (double) matchCount / keywords.length;
                    SearchResultDto result = convertChunkToSearchResult(chunk, 0.0, keywordScore);
                    result.setKeywordScore(keywordScore);
                    results.add(result);
                }
            }

            // Sort by keyword score and limit results
            results.sort((a, b) -> Double.compare(b.getKeywordScore(), a.getKeywordScore()));
            return results.stream().limit(keywordResultLimit).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Keyword search failed", e);
            throw new BusinessException("Keyword search failed: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Merge vector and keyword search results, deduplicating by chunk ID.
     * When a chunk appears in both searches, combine the scores.
     * 
     * @param vectorResults results from vector search
     * @param keywordResults results from keyword search
     * @return map of merged results keyed by chunk ID
     */
    private Map<Long, SearchResultDto> mergeResults(
            List<SearchResultDto> vectorResults,
            List<SearchResultDto> keywordResults) {

        Map<Long, SearchResultDto> merged = new LinkedHashMap<>();

        // Add vector search results
        for (SearchResultDto result : vectorResults) {
            merged.put(result.getChunkId(), result);
        }

        // Merge keyword results
        for (SearchResultDto result : keywordResults) {
            if (merged.containsKey(result.getChunkId())) {
                // Chunk found in both searches - combine scores
                SearchResultDto existing = merged.get(result.getChunkId());
                existing.setKeywordScore(result.getKeywordScore());
                existing.setFinalScore(calculateFinalScore(existing));
                existing.setScoreBreakdown(buildScoreBreakdown(existing));
                log.debug("Merged result for chunk {}: vector={}, keyword={}, final={}",
                        result.getChunkId(),
                        String.format("%.3f", existing.getVectorScore()),
                        String.format("%.3f", result.getKeywordScore()),
                        String.format("%.3f", existing.getFinalScore()));
            } else {
                // Chunk only in keyword search
                result.setFinalScore(calculateFinalScore(result));
                result.setScoreBreakdown(buildScoreBreakdown(result));
                merged.put(result.getChunkId(), result);
            }
        }

        return merged;
    }

    /**
     * Calculate the final combined score from vector and keyword scores.
     * Applies configurable weights to balance both approaches.
     * 
     * Formula: finalScore = (vectorWeight * vectorScore) + (keywordWeight * keywordScore)
     * Default: (0.7 * vectorScore) + (0.3 * keywordScore)
     * 
     * @param result the search result to score
     * @return the combined final score (0.0 to 1.0)
     */
    private Double calculateFinalScore(SearchResultDto result) {
        double vectorScore = result.getVectorScore() != null ? result.getVectorScore() : 0.0;
        double keywordScore = result.getKeywordScore() != null ? result.getKeywordScore() : 0.0;
        
        return (vectorWeight * vectorScore) + (keywordWeight * keywordScore);
    }

    /**
     * Build a detailed explanation of how the final score was calculated.
     * Useful for debugging and transparency in search results.
     * 
     * Format: "Vector: 0.95 * 0.7 = 0.665, Keyword: 0.60 * 0.3 = 0.180, Total: 0.845"
     * 
     * @param result the search result
     * @return formatted score breakdown string
     */
    private String buildScoreBreakdown(SearchResultDto result) {
        double vectorScore = result.getVectorScore() != null ? result.getVectorScore() : 0.0;
        double keywordScore = result.getKeywordScore() != null ? result.getKeywordScore() : 0.0;
        double vectorComponent = vectorWeight * vectorScore;
        double keywordComponent = keywordWeight * keywordScore;

        return String.format(
                "Vector: %.2f * %.2f = %.3f, Keyword: %.2f * %.2f = %.3f, Total: %.3f",
                vectorScore, vectorWeight, vectorComponent,
                keywordScore, keywordWeight, keywordComponent,
                result.getFinalScore()
        );
    }

    /**
     * Convert a DocumentChunk to a SearchResultDto.
     * Initializes common fields from the chunk and document.
     * 
     * @param chunk the document chunk
     * @param vectorScore the vector similarity score
     * @param keywordScore the keyword match score
     * @return search result DTO
     */
    private SearchResultDto convertChunkToSearchResult(
            DocumentChunk chunk, Double vectorScore, Double keywordScore) {

        SearchResultDto result = SearchResultDto.builder()
                .chunkId(chunk.getId())
                .documentId(chunk.getDocumentId())
                .content(chunk.getContent())
                .sectionName(chunk.getSectionName())
                .pageNumber(chunk.getPageNumber())
                .documentSource(chunk.getDocumentSource())
                .documentAuthor(chunk.getDocumentAuthor())
                .vectorScore(vectorScore)
                .keywordScore(keywordScore)
                .timestamp(LocalDateTime.now())
                .chunkQualityScore(chunk.getAverageRelevanceScore())
                .build();

        result.setFinalScore(calculateFinalScore(result));
        result.setScoreBreakdown(buildScoreBreakdown(result));

        return result;
    }

    /**
     * Calculate cosine similarity between two embedding vectors.
     * Returns a value between 0 and 1, where 1 is identical vectors.
     * 
     * Formula:
     * cosine_similarity = (A · B) / (||A|| * ||B||)
     * where · is dot product and || || is L2 norm
     * 
     * @param vectorA first embedding vector
     * @param vectorB second embedding vector
     * @return cosine similarity between 0 and 1
     */
    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            log.warn("Invalid vectors for cosine similarity calculation");
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    /**
     * Format a float array as a pgvector format string.
     * pgvector expects: "[0.1, 0.2, 0.3, ...]"
     * 
     * @param embedding the embedding vector
     * @return pgvector format string
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
     * Parse a pgvector format embedding string to float array.
     * Handles format: "[0.1, 0.2, 0.3, ...]"
     * 
     * @param embeddingStr the pgvector format string
     * @return float array of embedding values
     */
    private float[] parseEmbedding(String embeddingStr) {
        try {
            // Remove brackets and split by comma
            String cleaned = embeddingStr.replaceAll("[\\[\\]]", "").trim();
            String[] parts = cleaned.split(",");
            float[] embedding = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                embedding[i] = Float.parseFloat(parts[i].trim());
            }
            return embedding;
        } catch (Exception e) {
            log.error("Failed to parse embedding: {}", embeddingStr, e);
            throw new BusinessException("Invalid embedding format", e, HttpStatus.BAD_REQUEST);
        }
    }
}
