package com.karthik.eks.rag.repository;

import com.karthik.eks.rag.domain.SearchFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SearchFeedbackRepository - Data access for user search feedback.
 * 
 * Responsibilities:
 * - Store user ratings and feedback on search results
 * - Query feedback for analysis and quality monitoring
 * - Support ML training pipelines
 * - Enable A/B testing analysis
 */
@Repository
public interface SearchFeedbackRepository extends JpaRepository<SearchFeedback, Long> {

    /**
     * Find all feedback for a specific chunk.
     * Used to calculate chunk quality score and relevance.
     * 
     * @param chunkId the chunk ID
     * @param tenantId the tenant ID
     * @return list of feedback records for the chunk
     */
    List<SearchFeedback> findByChunkIdAndTenantId(Long chunkId, Long tenantId);

    /**
     * Find all feedback for a specific query (by hash).
     * Used to analyze search quality for particular queries.
     * 
     * @param queryHash hash of the query
     * @param tenantId the tenant ID
     * @param pageable pagination
     * @return page of feedback for the query
     */
    Page<SearchFeedback> findByQueryHashAndTenantId(
            String queryHash, Long tenantId, Pageable pageable);

    /**
     * Find feedback with specific feedback type (RELEVANT, IRRELEVANT, etc.).
     * Used for quality analysis and filtering.
     * 
     * @param feedbackType the feedback type
     * @param tenantId the tenant ID
     * @return list of feedback with the specified type
     */
    List<SearchFeedback> findByFeedbackTypeAndTenantId(
            SearchFeedback.FeedbackType feedbackType, Long tenantId);

    /**
     * Find feedback within a date range.
     * Used for time-series analysis and trend monitoring.
     * 
     * @param startDate start of date range (inclusive)
     * @param endDate end of date range (inclusive)
     * @param tenantId the tenant ID
     * @param pageable pagination
     * @return page of feedback within the date range
     */
    @Query("SELECT f FROM SearchFeedback f " +
           "WHERE f.tenantId = :tenantId " +
           "AND f.createdAt >= :startDate " +
           "AND f.createdAt <= :endDate")
    Page<SearchFeedback> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("tenantId") Long tenantId,
            Pageable pageable);

    /**
     * Calculate average feedback score for a chunk.
     * Used for chunk quality scoring in reranking.
     * 
     * Formula: Score = (sum of feedback scores) / (count of feedback)
     * Feedback score: RELEVANT=1.0, PARTIALLY=0.5, IRRELEVANT=0.0
     * 
     * @param chunkId the chunk ID
     * @param tenantId the tenant ID
     * @return average score 0.0-1.0, or null if no feedback
     */
    @Query(value = "SELECT AVG(CASE " +
                   "WHEN feedback_type = 'RELEVANT' THEN 1.0 " +
                   "WHEN feedback_type = 'PARTIALLY_RELEVANT' THEN 0.5 " +
                   "WHEN feedback_type = 'IRRELEVANT' THEN 0.0 " +
                   "ELSE 0.5 " +
                   "END) FROM search_feedback " +
                   "WHERE chunk_id = :chunkId AND tenant_id = :tenantId",
            nativeQuery = true)
    Double getAverageChunkScore(@Param("chunkId") Long chunkId, @Param("tenantId") Long tenantId);

    /**
     * Count positive feedback for a chunk.
     * Used to measure chunk usefulness.
     * 
     * @param chunkId the chunk ID
     * @param tenantId the tenant ID
     * @return count of positive feedback
     */
    @Query("SELECT COUNT(f) FROM SearchFeedback f " +
           "WHERE f.chunkId = :chunkId AND f.tenantId = :tenantId " +
           "AND (f.feedbackType = 'RELEVANT' OR f.rating >= 4)")
    long countPositiveFeedback(@Param("chunkId") Long chunkId, @Param("tenantId") Long tenantId);

    /**
     * Calculate average rating for query results.
     * Used to measure overall search quality.
     * 
     * @param queryHash hash of the query
     * @param tenantId the tenant ID
     * @return average rating 1-5, or null if no ratings
     */
    @Query("SELECT AVG(f.rating) FROM SearchFeedback f " +
           "WHERE f.queryHash = :queryHash AND f.tenantId = :tenantId")
    Double getAverageQueryRating(@Param("queryHash") String queryHash, @Param("tenantId") Long tenantId);

    /**
     * Find low-quality feedback cases for manual review.
     * Identifies problematic queries/results requiring intervention.
     * 
     * Low quality = negative feedback on top-ranked results or highly clicked but marked irrelevant
     * 
     * @param tenantId the tenant ID
     * @param maxRankPosition rank position threshold (e.g., top 3 results)
     * @param pageable pagination
     * @return page of problematic feedback
     */
    @Query("SELECT f FROM SearchFeedback f " +
           "WHERE f.tenantId = :tenantId " +
           "AND f.rankPosition <= :maxRankPosition " +
           "AND f.feedbackType = 'IRRELEVANT' " +
           "AND f.clicked = true " +
           "ORDER BY f.createdAt DESC")
    Page<SearchFeedback> findProblematicFeedback(
            @Param("tenantId") Long tenantId,
            @Param("maxRankPosition") Integer maxRankPosition,
            Pageable pageable);

    /**
     * Find feedback for A/B testing analysis.
     * Compares ranking variants by feedback scores.
     * 
     * @param experimentVariant A/B test variant identifier
     * @param tenantId the tenant ID
     * @return list of feedback for the experiment
     */
    List<SearchFeedback> findByExperimentVariantAndTenantId(
            String experimentVariant, Long tenantId);

    /**
     * Find pending feedback awaiting processing.
     * Used by batch processing jobs to train models.
     * 
     * @param status the feedback status
     * @param tenantId the tenant ID
     * @param pageable pagination
     * @return page of pending feedback
     */
    Page<SearchFeedback> findByStatusAndTenantId(
            SearchFeedback.FeedbackStatus status, Long tenantId, Pageable pageable);
}
