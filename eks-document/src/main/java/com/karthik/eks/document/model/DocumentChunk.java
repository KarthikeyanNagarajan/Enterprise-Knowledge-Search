package com.karthik.eks.document.model;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DocumentChunk Model - Represents a chunk of a document for RAG processing.
 * 
 * Chunking Strategy:
 * - Each document is split into overlapping chunks (~1500 chars, ~300-500 tokens)
 * - Overlap of 300 chars to preserve context across chunk boundaries
 * - Chunks are embedded separately for vector search
 * 
 * Metadata for Enhanced RAG:
 * - Stores metadata about chunk source (section, page, etc.)
 * - Tracks embedding status (PENDING, EMBEDDED, FAILED)
 * - Enables document-level filtering in retrieval
 * - Supports access control through tenant isolation
 * 
 * Relationships:
 * - Many chunks belong to one Document
 * - Each chunk has a corresponding VectorEmbedding for vector search
 * 
 * Data Flow:
 * 1. Document uploaded → split into chunks
 * 2. Chunk stored with metadata and PENDING embedding status
 * 3. Background job generates embedding → updates status to EMBEDDED
 * 4. RAG retrieves chunks based on vector similarity
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunk_document_id", columnList = "document_id"),
    @Index(name = "idx_chunk_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_chunk_status", columnList = "embedding_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk extends BaseEntity {

    /**
     * Reference to the parent Document.
     */
    @Column(nullable = false)
    private Long documentId;

    /**
     * Tenant ID for multi-tenancy and data isolation.
     */
    @Column(nullable = false)
    private Long tenantId;

    /**
     * Sequential index of this chunk within the document (0, 1, 2, ...).
     * Helps reconstruct document context.
     */
    @Column(nullable = false)
    private Integer chunkIndex;

    /**
     * The actual text content of this chunk (~1500 characters).
     * Optimized for embedding generation (typically 300-500 tokens).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Approximate token count of the chunk content.
     * Used for cost estimation and quota tracking.
     */
    @Column(nullable = false)
    private Integer tokens;

    /**
     * Embedding generation status: PENDING, EMBEDDED, FAILED.
     * Used to track which chunks have been embedded for vector search.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmbeddingStatus embeddingStatus = EmbeddingStatus.PENDING;

    /**
     * Source section or heading for this chunk.
     * Helps identify chunk context in search results.
     * Example: "Chapter 2", "Section 3.1", "Appendix A"
     */
    @Column(length = 255)
    private String sectionName;

    /**
     * Page number if applicable (for PDF or paged documents).
     * Useful for citation and user navigation.
     */
    private Integer pageNumber;

    /**
     * Character offset of this chunk in the original document.
     * Enables linking back to the source location.
     */
    private Integer charOffset;

    /**
     * Document source type for filtering in RAG results.
     * Inherited from parent document at chunk creation.
     */
    @Column(length = 50)
    private String documentSource;

    /**
     * Document author for access control in retrieval.
     * Inherited from parent document at chunk creation.
     */
    @Column(length = 255)
    private String documentAuthor;

    /**
     * Last error message if embedding generation failed.
     * Helpful for debugging and monitoring embedding pipeline.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Number of times this chunk appeared in search results.
     * Can be used for popularity-based ranking and feedback.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer retrievalCount = 0;

    /**
     * Average relevance score across all retrievals.
     * Used for monitoring chunk quality and search effectiveness.
     */
    private Double averageRelevanceScore;

    /**
     * EmbeddingStatus enumeration for tracking embedding generation.
     */
    public enum EmbeddingStatus {
        PENDING,   // Chunk created, awaiting embedding generation
        EMBEDDED,  // Successfully embedded and stored in vector database
        FAILED     // Embedding generation failed, see errorMessage
    }

    /**
     * Increments the retrieval count when this chunk is retrieved.
     * Called by RAG service during search result processing.
     */
    public void incrementRetrievalCount() {
        this.retrievalCount = (this.retrievalCount != null ? this.retrievalCount : 0) + 1;
    }

    /**
     * Updates the average relevance score based on a new retrieval score.
     * Implements exponential moving average for efficiency.
     * 
     * @param newScore the relevance score from the latest retrieval (0.0 to 1.0)
     */
    public void updateAverageRelevanceScore(Double newScore) {
        if (newScore == null || newScore < 0 || newScore > 1.0) {
            return;
        }

        if (this.averageRelevanceScore == null) {
            this.averageRelevanceScore = newScore;
        } else {
            // Exponential moving average with alpha=0.3 for recent scores to matter more
            double alpha = 0.3;
            this.averageRelevanceScore = (alpha * newScore) + ((1 - alpha) * this.averageRelevanceScore);
        }
    }
}
