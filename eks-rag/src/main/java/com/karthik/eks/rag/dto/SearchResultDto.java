package com.karthik.eks.rag.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * SearchResultDto - Represents a single search result with metadata and scores.
 * 
 * Purpose:
 * - Combines vector similarity score and keyword match score
 * - Tracks result quality and source information
 * - Enables client-side result rendering with full context
 * 
 * Fields:
 * - chunkId: unique identifier for the found chunk
 * - content: the actual chunk text
 * - documentTitle: parent document name (for user reference)
 * - sectionName: where in the document this chunk came from
 * - vectorScore: semantic similarity score (0.0-1.0, higher is better)
 * - keywordScore: keyword match score (0.0-1.0, higher is better)
 * - finalScore: combined score used for ranking (0.0-1.0)
 * - scoreBreakdown: explains how final score was calculated
 * 
 * Score Calculation (Hybrid):
 * finalScore = (weightVector * vectorScore) + (weightKeyword * keywordScore)
 * Default weights: vector=0.7, keyword=0.3 (configurable)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultDto {

    /**
     * Unique identifier of the document chunk.
     */
    private Long chunkId;

    /**
     * Document ID for document tracking.
     */
    private Long documentId;

    /**
     * Parent document title for user context.
     */
    private String documentTitle;

    /**
     * The actual text content of this chunk.
     */
    private String content;

    /**
     * Section name where this chunk originated (e.g., "Chapter 2", "Section 3.1").
     */
    private String sectionName;

    /**
     * Page number if applicable (helpful for PDF navigation).
     */
    private Integer pageNumber;

    /**
     * Vector (semantic) similarity score from embedding search (0.0-1.0).
     * Higher is better. Measures meaning similarity.
     */
    private Double vectorScore;

    /**
     * Keyword match score from text search (0.0-1.0).
     * Higher is better. Measures keyword presence/frequency.
     */
    private Double keywordScore;

    /**
     * Final combined score used for ranking results (0.0-1.0).
     * Calculated as: (0.7 * vectorScore) + (0.3 * keywordScore)
     */
    private Double finalScore;

    /**
     * Explanation of how the final score was calculated.
     * Useful for debugging and transparency.
     * Example: "Vector: 0.95 * 0.7 = 0.665, Keyword: 0.60 * 0.3 = 0.180, Total: 0.845"
     */
    private String scoreBreakdown;

    /**
     * Document source type (e.g., "PDF", "WIKI", "CONFLUENCE").
     * Useful for filtering and transparency.
     */
    private String documentSource;

    /**
     * Document author for access control and transparency.
     */
    private String documentAuthor;

    /**
     * Retrieval timestamp for sorting by recency.
     */
    private LocalDateTime timestamp;

    /**
     * Average relevance score of this chunk across all retrievals.
     * Indicates chunk quality and usefulness.
     */
    private Double chunkQualityScore;
}
