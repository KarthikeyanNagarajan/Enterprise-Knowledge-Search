package com.karthik.eks.rag.domain;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * SearchFeedback Model - Tracks user feedback on search results.
 * 
 * Purpose:
 * - Captures user ratings and feedback on search results
 * - Enables continuous improvement of search quality
 * - Powers reranking algorithms with real user signals
 * - Supports A/B testing and experiment evaluation
 * 
 * Feedback Signals:
 * - Rating: User satisfaction (1-5 stars, or thumbs up/down)
 * - Feedback Type: RELEVANT, IRRELEVANT, PARTIALLY_RELEVANT
 * - Comments: Optional user comments for deeper understanding
 * - Click-through: Whether user clicked the result
 * - Dwell Time: How long user interacted with the result
 * 
 * Use Cases:
 * 1. Quality Monitoring: Track average satisfaction per query
 * 2. Machine Learning: Train ranking models on feedback data
 * 3. Anomaly Detection: Identify problematic queries/results
 * 4. Personalization: Understand user preferences
 * 5. A/B Testing: Compare two ranking strategies
 * 
 * Privacy:
 * - Anonymous by default (no user ID required)
 * - Can optionally track user for personalization
 * - Subject to data retention policies
 */
@Entity
@Table(name = "search_feedback", indexes = {
    @Index(name = "idx_feedback_chunk", columnList = "chunk_id"),
    @Index(name = "idx_feedback_query", columnList = "query_hash"),
    @Index(name = "idx_feedback_tenant", columnList = "tenant_id"),
    @Index(name = "idx_feedback_timestamp", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchFeedback extends BaseEntity {

    /**
     * The search query that generated this result.
     * Stored for analysis and debugging.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    /**
     * Hash of the query for efficient deduplication and analysis.
     * Useful for grouping feedback by query type.
     */
    @Column(nullable = false, length = 64)
    private String queryHash;

    /**
     * ID of the chunk that was rated/rated by user.
     * Links feedback to specific results.
     */
    @Column(nullable = false)
    private Long chunkId;

    /**
     * Rank position of this result in the search results.
     * Example: 1 (first result), 2 (second result), etc.
     * Used to track position bias and ranking quality.
     */
    @Column(nullable = false)
    private Integer rankPosition;

    /**
     * Tenant ID for multi-tenancy and isolation.
     */
    @Column(nullable = false)
    private Long tenantId;

    /**
     * User feedback type: RELEVANT, IRRELEVANT, PARTIALLY_RELEVANT, NOT_SURE.
     * Provides categorical feedback on result quality.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType feedbackType;

    /**
     * Numeric rating from user (1-5 stars or similar).
     * 1 = very poor, 5 = excellent.
     * Optional but preferred for ML training.
     */
    private Integer rating;

    /**
     * User comment explaining their feedback.
     * Provides qualitative feedback for understanding issues.
     * Example: "This result doesn't address my question"
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * Whether the user clicked on this result.
     * Click = engagement signal (but not necessarily relevant).
     */
    @Builder.Default
    private Boolean clicked = false;

    /**
     * Time (milliseconds) user spent viewing this result.
     * Long dwell time → likely relevant. Short → likely irrelevant.
     * Used by advanced ranking algorithms.
     */
    private Long dwellTimeMs;

    /**
     * Whether the user selected this result as their final choice.
     * Strong signal of usefulness and relevance.
     */
    @Builder.Default
    private Boolean selected = false;

    /**
     * A/B test variant ID (if this search is part of an experiment).
     * Used to compare ranking strategies.
     */
    @Column(length = 64)
    private String experimentVariant;

    /**
     * Session ID for grouping related searches.
     * Used to track user session patterns and conversation flow.
     */
    @Column(length = 64)
    private String sessionId;

    /**
     * Source system where this feedback originated.
     * Example: "web", "mobile", "api", "internal".
     */
    @Column(length = 50)
    private String source;

    /**
     * Feedback status: PENDING, PROCESSED, ARCHIVED.
     * PENDING = awaiting quality check or processing
     * PROCESSED = used for analysis
     * ARCHIVED = old feedback no longer in active training
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FeedbackStatus status = FeedbackStatus.PENDING;

    /**
     * FeedbackType enumeration.
     */
    public enum FeedbackType {
        RELEVANT,             // Result directly answers the query
        PARTIALLY_RELEVANT,   // Result partially answers the query
        IRRELEVANT,          // Result has nothing to do with the query
        NOT_SURE             // User unsure about relevance
    }

    /**
     * FeedbackStatus enumeration.
     */
    public enum FeedbackStatus {
        PENDING,   // Awaiting quality checks
        PROCESSED, // Used in analysis/training
        ARCHIVED   // Old feedback no longer in active use
    }

    /**
     * Convenience method to compute feedback score (0.0-1.0).
     * Used for aggregation and analysis.
     * 
     * Scoring:
     * - RELEVANT: 1.0
     * - PARTIALLY_RELEVANT: 0.5
     * - IRRELEVANT: 0.0
     * - NOT_SURE: 0.5 (neutral)
     * 
     * Or use numeric rating if provided:
     * - Rating 5: 1.0
     * - Rating 1: 0.0
     * - Linear scale for 2-4
     * 
     * @return normalized feedback score 0.0-1.0
     */
    public Double computeFeedbackScore() {
        // Prefer numeric rating if available
        if (rating != null) {
            return (rating - 1) / 4.0;  // Convert 1-5 scale to 0.0-1.0
        }

        // Fall back to categorical feedback
        return switch (feedbackType) {
            case RELEVANT -> 1.0;
            case PARTIALLY_RELEVANT -> 0.5;
            case IRRELEVANT -> 0.0;
            case NOT_SURE -> 0.5;
        };
    }

    /**
     * Check if this feedback is positive (useful to the user).
     * @return true if relevant or highly rated
     */
    public boolean isPositive() {
        return feedbackType == FeedbackType.RELEVANT || 
               (rating != null && rating >= 4);
    }

    /**
     * Check if this feedback is negative (not useful).
     * @return true if irrelevant or poorly rated
     */
    public boolean isNegative() {
        return feedbackType == FeedbackType.IRRELEVANT || 
               (rating != null && rating <= 2);
    }
}
