package com.karthik.eks.rag.service;

import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.rag.domain.SearchFeedback;
import com.karthik.eks.rag.repository.SearchFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * FeedbackService - Manages user feedback for search results.
 * 
 * Purpose:
 * - Records user ratings and feedback on search results
 * - Calculates quality metrics and trends
 * - Powers feedback-driven ranking improvements
 * - Supports analysis and A/B testing
 * 
 * Workflow:
 * 1. User performs search → gets results
 * 2. User rates/comments on result
 * 3. FeedbackService records the feedback
 * 4. Analytics services aggregate feedback
 * 5. Reranking service uses feedback scores
 * 6. System continuously improves
 * 
 * Benefits:
 * - Closes the feedback loop for ML improvement
 * - Provides early warning of ranking degradation
 * - Enables data-driven optimization decisions
 * - Supports user satisfaction tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final SearchFeedbackRepository feedbackRepository;

    /**
     * Record feedback from a user on a search result.
     * 
     * Steps:
     * 1. Validate input
     * 2. Hash the query for grouping
     * 3. Create feedback record
     * 4. Save to repository
     * 5. Log for analytics
     * 
     * @param chunkId the result chunk ID
     * @param query the original search query
     * @param rankPosition the position in results
     * @param feedbackType the feedback type (RELEVANT, IRRELEVANT, etc.)
     * @param rating optional numeric rating (1-5)
     * @param comment optional user comment
     * @param tenantId the tenant ID
     * @return the saved feedback record
     */
    @Transactional
    public SearchFeedback recordFeedback(
            Long chunkId, String query, Integer rankPosition,
            SearchFeedback.FeedbackType feedbackType,
            Integer rating, String comment, Long tenantId) {

        log.info("Recording feedback for chunk={}, query='{}', feedbackType={}, tenantId={}",
                chunkId, query, feedbackType, tenantId);

        // Validate inputs
        if (chunkId == null || chunkId <= 0) {
            throw new BusinessException("Invalid chunk ID", HttpStatus.BAD_REQUEST);
        }
        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException("Query cannot be empty", HttpStatus.BAD_REQUEST);
        }
        if (rankPosition == null || rankPosition <= 0) {
            throw new BusinessException("Invalid rank position", HttpStatus.BAD_REQUEST);
        }
        if (feedbackType == null) {
            throw new BusinessException("Feedback type is required", HttpStatus.BAD_REQUEST);
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BusinessException("Rating must be between 1 and 5", HttpStatus.BAD_REQUEST);
        }

        try {
            // Create feedback record
            SearchFeedback feedback = SearchFeedback.builder()
                    .chunkId(chunkId)
                    .query(query.trim())
                    .queryHash(hashQuery(query))
                    .rankPosition(rankPosition)
                    .feedbackType(feedbackType)
                    .rating(rating)
                    .comment(comment)
                    .tenantId(tenantId)
                    .status(SearchFeedback.FeedbackStatus.PENDING)
                    .build();

            // Save feedback
            SearchFeedback saved = feedbackRepository.save(feedback);
            log.info("Feedback recorded successfully. ID={}, score={}", 
                    saved.getId(), String.format("%.2f", saved.computeFeedbackScore()));

            return saved;

        } catch (Exception e) {
            log.error("Failed to record feedback", e);
            throw new BusinessException("Failed to record feedback: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Record feedback with additional signals (click, dwell time, selection).
     * 
     * @param chunkId the result chunk ID
     * @param query the search query
     * @param rankPosition result rank position
     * @param feedbackType the feedback type
     * @param rating optional rating
     * @param clicked whether user clicked the result
     * @param dwellTimeMs time (ms) user spent on result
     * @param selected whether user selected this as final choice
     * @param tenantId the tenant ID
     * @return the saved feedback record
     */
    @Transactional
    public SearchFeedback recordFeedbackWithSignals(
            Long chunkId, String query, Integer rankPosition,
            SearchFeedback.FeedbackType feedbackType,
            Integer rating, boolean clicked, Long dwellTimeMs,
            boolean selected, Long tenantId) {

        SearchFeedback feedback = recordFeedback(
                chunkId, query, rankPosition, feedbackType, rating, null, tenantId);

        feedback.setClicked(clicked);
        feedback.setDwellTimeMs(dwellTimeMs);
        feedback.setSelected(selected);

        return feedbackRepository.save(feedback);
    }

    /**
     * Get average quality score for a chunk across all feedback.
     * Score combines explicit ratings and implicit signals.
     * 
     * @param chunkId the chunk ID
     * @param tenantId the tenant ID
     * @return average score 0.0-1.0, or null if no feedback
     */
    @Transactional(readOnly = true)
    public Double getChunkQualityScore(Long chunkId, Long tenantId) {
        log.debug("Calculating quality score for chunk={}", chunkId);

        Double avgScore = feedbackRepository.getAverageChunkScore(chunkId, tenantId);
        log.debug("Chunk quality score: {}", avgScore);
        return avgScore;
    }

    /**
     * Calculate search quality metrics for analysis.
     * Aggregates feedback to assess overall search quality.
     * 
     * @param queryHash hash of the query
     * @param tenantId the tenant ID
     * @return quality metrics object
     */
    @Transactional(readOnly = true)
    public SearchQualityMetrics getSearchQualityMetrics(String queryHash, Long tenantId) {
        log.debug("Calculating search quality metrics for queryHash={}", queryHash);

        // Get all feedback for this query
        Page<SearchFeedback> feedbackPage = feedbackRepository.findByQueryHashAndTenantId(
                queryHash, tenantId, PageRequest.of(0, 1000));

        List<SearchFeedback> allFeedback = feedbackPage.getContent();

        if (allFeedback.isEmpty()) {
            return SearchQualityMetrics.builder()
                    .queryHash(queryHash)
                    .totalFeedback(0)
                    .averageRating(null)
                    .relevanceRate(0.0)
                    .clickThroughRate(0.0)
                    .build();
        }

        // Calculate metrics
        long relevantCount = allFeedback.stream()
                .filter(f -> f.getFeedbackType() == SearchFeedback.FeedbackType.RELEVANT)
                .count();

        long clickedCount = allFeedback.stream()
                .filter(f -> f.getClicked() != null && f.getClicked())
                .count();

        Double avgRating = feedbackRepository.getAverageQueryRating(queryHash, tenantId);

        SearchQualityMetrics metrics = SearchQualityMetrics.builder()
                .queryHash(queryHash)
                .totalFeedback(allFeedback.size())
                .averageRating(avgRating)
                .relevanceRate((double) relevantCount / allFeedback.size())
                .clickThroughRate((double) clickedCount / allFeedback.size())
                .averageDwellTimeMs(calculateAverageDwellTime(allFeedback))
                .selectionRate(calculateSelectionRate(allFeedback))
                .build();

        log.info("Search quality metrics: relevance={}, ctr={}, rating={}",
                String.format("%.2f", metrics.getRelevanceRate()),
                String.format("%.2f", metrics.getClickThroughRate()),
                avgRating != null ? String.format("%.2f", avgRating) : "N/A");

        return metrics;
    }

    /**
     * Identify problematic searches that need ranking improvement.
     * Returns queries with low relevance scores or high irrelevant clicks.
     * 
     * @param tenantId the tenant ID
     * @param maxRankPosition max rank position to check
     * @param limit maximum number of results
     * @return list of problematic feedback records
     */
    @Transactional(readOnly = true)
    public List<SearchFeedback> getProblematicSearches(
            Long tenantId, Integer maxRankPosition, int limit) {

        log.debug("Finding problematic searches for tenant={}", tenantId);

        Page<SearchFeedback> problematic = feedbackRepository.findProblematicFeedback(
                tenantId, maxRankPosition, PageRequest.of(0, limit));

        log.info("Found {} problematic searches", problematic.getContent().size());
        return problematic.getContent();
    }

    /**
     * Process pending feedback for model training.
     * Called by batch jobs to update chunk quality scores and train ranking models.
     * 
     * @param tenantId the tenant ID
     * @param batchSize number of feedback records to process
     * @return number of records processed
     */
    @Transactional
    public int processPendingFeedback(Long tenantId, int batchSize) {
        log.info("Processing pending feedback for tenant={}, batchSize={}", tenantId, batchSize);

        Page<SearchFeedback> pendingFeedback = feedbackRepository.findByStatusAndTenantId(
                SearchFeedback.FeedbackStatus.PENDING, tenantId,
                PageRequest.of(0, batchSize));

        List<SearchFeedback> toProcess = pendingFeedback.getContent();

        for (SearchFeedback feedback : toProcess) {
            // Mark as processed
            feedback.setStatus(SearchFeedback.FeedbackStatus.PROCESSED);
            feedbackRepository.save(feedback);

            log.debug("Processed feedback: chunk={}, score={}", 
                    feedback.getChunkId(),
                    String.format("%.2f", feedback.computeFeedbackScore()));
        }

        log.info("Processed {} feedback records", toProcess.size());
        return toProcess.size();
    }

    /**
     * Hash a query for efficient grouping and deduplication.
     * Uses SHA-256 for consistency.
     * 
     * @param query the query text
     * @return hex string hash
     */
    private String hashQuery(String query) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(query.toLowerCase().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 64);  // Limit to 64 chars
        } catch (Exception e) {
            log.warn("Failed to hash query, using default", e);
            return String.valueOf(query.hashCode());
        }
    }

    /**
     * Calculate average dwell time from feedback records.
     * @param feedbacks list of feedback records
     * @return average dwell time in milliseconds
     */
    private Long calculateAverageDwellTime(List<SearchFeedback> feedbacks) {
        OptionalDouble average = feedbacks.stream()
                .filter(f -> f.getDwellTimeMs() != null && f.getDwellTimeMs() > 0)
                .mapToLong(SearchFeedback::getDwellTimeMs)
                .average();
        
        return average.isPresent() ? Math.round(average.getAsDouble()) : 0L;
    }

    /**
     * Calculate selection rate (% of results selected by users).
     * @param feedbacks list of feedback records
     * @return selection rate 0.0-1.0
     */
    private Double calculateSelectionRate(List<SearchFeedback> feedbacks) {
        long selectedCount = feedbacks.stream()
                .filter(f -> f.getSelected() != null && f.getSelected())
                .count();
        return (double) selectedCount / Math.max(feedbacks.size(), 1);
    }

    /**
     * SearchQualityMetrics DTO - Aggregated metrics for a search query.
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    public static class SearchQualityMetrics {
        private String queryHash;
        private Integer totalFeedback;
        private Double averageRating;
        private Double relevanceRate;      // % of results marked as RELEVANT
        private Double clickThroughRate;   // % of results clicked
        private Long averageDwellTimeMs;   // Average time spent on result
        private Double selectionRate;      // % of results selected as final choice
    }
}
