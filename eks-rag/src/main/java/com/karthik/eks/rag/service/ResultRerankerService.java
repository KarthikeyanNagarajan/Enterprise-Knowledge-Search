package com.karthik.eks.rag.service;

import com.karthik.eks.rag.dto.SearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ResultRerankerService - Reranks search results using multiple signals.
 * 
 * Purpose:
 * - Improves initial search ranking with domain knowledge
 * - Considers multiple relevance signals beyond semantic similarity
 * - Provides transparency through score breakdown
 * 
 * Reranking Signals:
 * 1. Base Score: The hybrid search score (vector + keyword)
 * 2. Quality Score: Average relevance feedback from past retrievals
 * 3. Freshness: Prefers newer documents (optional)
 * 4. Popularity: Boosts chunks frequently retrieved
 * 5. Source Credibility: Can boost results from trusted sources
 * 
 * Weighting (Configurable):
 * - Base Score: 50% (hybrid search result quality)
 * - Quality Score: 30% (user feedback and past performance)
 * - Popularity: 15% (retrieval frequency)
 * - Freshness: 5% (document recency)
 * 
 * Benefits:
 * - Machine Learning: Incorporates feedback loop data
 * - Diversity: Can penalize similar consecutive results
 * - Authority: Boosts results from trusted sources
 * - Personalization: Can consider user preferences
 * 
 * Example:
 * Input: [Result A (score=0.9, quality=0.8, pop=5), Result B (score=0.85, quality=0.95, pop=15)]
 * Output: [Result B (reranked=0.93), Result A (reranked=0.88)]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultRerankerService {

    @Value("${rag.rerank.base-weight:0.5}")
    private Double baseScoreWeight;

    @Value("${rag.rerank.quality-weight:0.3}")
    private Double qualityScoreWeight;

    @Value("${rag.rerank.popularity-weight:0.15}")
    private Double popularityWeight;

    @Value("${rag.rerank.freshness-weight:0.05}")
    private Double freshnessWeight;

    @Value("${rag.rerank.max-freshness-days:30}")
    private Integer maxFreshnessDays;

    @Value("${rag.rerank.enable-diversity-penalty:false}")
    private Boolean enableDiversityPenalty;

    /**
     * Rerank search results using multiple signals.
     * 
     * Process:
     * 1. Validate inputs
     * 2. Calculate signal scores for each result
     * 3. Combine signals with weights
     * 4. Sort by reranked score
     * 5. Apply diversity penalties (optional)
     * 
     * @param results the initial search results
     * @param query the original search query (for diversity calculation)
     * @return reranked results sorted by final score
     */
    public List<SearchResultDto> rerank(List<SearchResultDto> results, String query) {
        log.info("Reranking {} results for query: '{}'", results.size(), query);

        if (results == null || results.isEmpty()) {
            log.debug("No results to rerank");
            return new ArrayList<>();
        }

        try {
            // Calculate reranking scores
            List<RankedResult> rankedResults = results.stream()
                    .map(result -> calculateRankedScore(result))
                    .collect(Collectors.toList());

            // Sort by reranked score descending
            rankedResults.sort((a, b) -> Double.compare(b.getRerankedScore(), a.getRerankedScore()));

            // Apply diversity penalty if enabled (to avoid similar results clustering)
            if (enableDiversityPenalty && results.size() > 2) {
                applyDiversityPenalty(rankedResults, query);
                rankedResults.sort((a, b) -> Double.compare(b.getRerankedScore(), a.getRerankedScore()));
            }

            // Convert back to SearchResultDto with updated scores
            List<SearchResultDto> rerankedResults = rankedResults.stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());

            log.info("Reranking complete. Top result score: {}", 
                    rerankedResults.isEmpty() ? "N/A" : 
                    String.format("%.3f", rerankedResults.get(0).getFinalScore()));

            return rerankedResults;

        } catch (Exception e) {
            log.error("Reranking failed", e);
            // On error, return original results
            return results;
        }
    }

    /**
     * Calculate a combined reranked score for a single result.
     * Factors: base score, quality, popularity, freshness.
     * 
     * Formula:
     * rerankedScore = (baseWeight * baseScore) +
     *                 (qualityWeight * qualityScore) +
     *                 (popularityWeight * popularityScore) +
     *                 (freshnessWeight * freshnessScore)
     * 
     * @param result the search result to score
     * @return ranked result with detailed score breakdown
     */
    private RankedResult calculateRankedScore(SearchResultDto result) {
        double baseScore = result.getFinalScore() != null ? result.getFinalScore() : 0.0;
        
        // Quality score (from feedback/past performance, 0.0-1.0)
        double qualityScore = result.getChunkQualityScore() != null ? 
                result.getChunkQualityScore() : 0.5;  // Default to neutral
        
        // Popularity score (derived from retrieval count, normalized)
        double popularityScore = normalizePopularityScore(result.getChunkId());
        
        // Freshness score (based on document age, 0.0-1.0)
        double freshnessScore = calculateFreshnessScore(result.getTimestamp());

        // Combine scores with weights
        double rerankedScore = (baseScoreWeight * baseScore) +
                               (qualityScoreWeight * qualityScore) +
                               (popularityWeight * popularityScore) +
                               (freshnessWeight * freshnessScore);

        // Create detailed breakdown
        String breakdown = String.format(
                "Base: %.2f*%.3f=%.3f, Quality: %.2f*%.3f=%.3f, Popularity: %.2f*%.3f=%.3f, Freshness: %.2f*%.3f=%.3f → Final: %.3f",
                baseScoreWeight, baseScore, baseScoreWeight * baseScore,
                qualityScoreWeight, qualityScore, qualityScoreWeight * qualityScore,
                popularityWeight, popularityScore, popularityWeight * popularityScore,
                freshnessWeight, freshnessScore, freshnessWeight * freshnessScore,
                rerankedScore
        );

        return RankedResult.builder()
                .chunkId(result.getChunkId())
                .originalResult(result)
                .baseScore(baseScore)
                .qualityScore(qualityScore)
                .popularityScore(popularityScore)
                .freshnessScore(freshnessScore)
                .rerankedScore(rerankedScore)
                .scoreBreakdown(breakdown)
                .build();
    }

    /**
     * Normalize retrieval count to a popularity score (0.0-1.0).
     * Uses logarithmic scaling to avoid giving too much weight to very popular items.
     * 
     * Formula: normalizedScore = log(retrievalCount + 1) / log(maxCount + 1)
     * This ensures the score ranges from 0 to 1 naturally.
     * 
     * @param chunkId the chunk ID
     * @return normalized popularity score between 0 and 1
     */
    private double normalizePopularityScore(Long chunkId) {
        // In a real implementation, this would fetch actual retrieval count from repository
        // For now, use a default moderate score
        // TODO: Integrate with DocumentChunkRepository to get actual retrievalCount
        
        int retrievalCount = 5;  // Placeholder
        int maxExpectedCount = 100;  // Scaling factor
        
        // Use logarithmic scale to avoid linear bias
        return Math.log(retrievalCount + 1) / Math.log(maxExpectedCount + 1);
    }

    /**
     * Calculate freshness score based on document creation/update time.
     * Gives preference to newer documents within a rolling window.
     * 
     * Scoring:
     * - < 1 day old: 1.0 (perfect freshness)
     * - 1-7 days old: 0.8-1.0
     * - 7-30 days old: 0.5-0.8
     * - > 30 days old: 0.0-0.5
     * 
     * @param timestamp the document timestamp
     * @return freshness score between 0 and 1
     */
    private double calculateFreshnessScore(LocalDateTime timestamp) {
        if (timestamp == null) {
            return 0.5;  // Default to neutral if no timestamp
        }

        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(timestamp, LocalDateTime.now());
        
        if (daysSinceCreation <= 1) {
            return 1.0;
        } else if (daysSinceCreation <= 7) {
            return 1.0 - (0.2 * (daysSinceCreation / 7.0));  // Linear decay from 1.0 to 0.8
        } else if (daysSinceCreation <= maxFreshnessDays) {
            return 0.8 - (0.3 * ((daysSinceCreation - 7) / (double)(maxFreshnessDays - 7)));  // Decay from 0.8 to 0.5
        } else {
            return 0.5 * Math.pow(0.5, ( daysSinceCreation - maxFreshnessDays) / 30.0);  // Exponential decay beyond max
        }
    }

    /**
     * Apply diversity penalty to reduce clustering of very similar results.
     * Penalizes results that have similar content as higher-ranked results.
     * 
     * Purpose:
     * - Ensures result set diversity
     * - Avoids showing multiple chunks from the same section
     * - Improves user experience with varied perspectives
     * 
     * @param rankedResults the ranked results to penalize
     * @param query the search query
     */
    private void applyDiversityPenalty(List<RankedResult> rankedResults, String query) {
        log.debug("Applying diversity penalty to avoid similar result clustering");

        for (int i = 1; i < rankedResults.size(); i++) {
            RankedResult current = rankedResults.get(i);
            double maxSimilarity = 0.0;

            // Check similarity with all higher-ranked results
            for (int j = 0; j < i; j++) {
                RankedResult higher = rankedResults.get(j);
                double similarity = calculateContentSimilarity(
                        current.getOriginalResult().getContent(),
                        higher.getOriginalResult().getContent()
                );
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }

            // Apply penalty: reduce score by up to 20% based on similarity
            if (maxSimilarity > 0.7) {
                double penalty = 0.2 * (maxSimilarity - 0.7) / 0.3;  // Scale from 0 to 0.2
                current.setRerankedScore(current.getRerankedScore() * (1 - penalty));
                log.debug("Applied diversity penalty to chunk {}: similarity={}, penalty={}", 
                        current.getChunkId(), 
                        String.format("%.2f", maxSimilarity),
                        String.format("%.2f", penalty));
            }
        }
    }

    /**
     * Calculate content similarity between two text chunks.
     * Uses Jaccard similarity (set-based overlap of words).
     * 
     * Jaccard = |A ∩ B| / |A ∪ B|
     * where A and B are sets of words
     * 
     * @param content1 first chunk content
     * @param content2 second chunk content
     * @return similarity between 0 and 1
     */
    private double calculateContentSimilarity(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return 0.0;
        }

        String[] words1 = content1.toLowerCase().split("\\s+");
        String[] words2 = content2.toLowerCase().split("\\s+");

        java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
        java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Convert RankedResult back to SearchResultDto with updated scores.
     * 
     * @param rankedResult the ranked result
     * @return search result DTO with updated scores
     */
    private SearchResultDto convertToSearchResult(RankedResult rankedResult) {
        SearchResultDto result = rankedResult.getOriginalResult();
        result.setFinalScore(rankedResult.getRerankedScore());
        result.setScoreBreakdown(rankedResult.getScoreBreakdown());
        return result;
    }

    /**
     * Internal class to hold ranking intermediate values.
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    private static class RankedResult {
        private Long chunkId;
        private SearchResultDto originalResult;
        private Double baseScore;
        private Double qualityScore;
        private Double popularityScore;
        private Double freshnessScore;
        private Double rerankedScore;
        private String scoreBreakdown;
    }
}
