package com.karthik.eks.document.repository;

import com.karthik.eks.document.model.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DocumentChunkRepository - Data access layer for DocumentChunk entities.
 * 
 * Responsibilities:
 * - Retrieve chunks by document ID for embedding generation
 * - Find chunks by embedding status (PENDING, EMBEDDED, FAILED)
 * - Support filtering by tenant for multi-tenancy
 * - Provide pagination for large document result sets
 * 
 * Used by:
 * - DocumentService: for chunk management
 * - EmbeddingService: to find chunks needing embedding
 * - RagService: to retrieve chunks based on metadata filtering
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * Retrieve all chunks for a specific document.
     * Used when processing document embeddings or retrieving full context.
     * 
     * @param documentId the document ID
     * @param tenantId the tenant ID for security
     * @return list of all chunks in order
     */
    List<DocumentChunk> findByDocumentIdAndTenantId(Long documentId, Long tenantId);

    /**
     * Retrieve a specific chunk by document and chunk index.
     * Used to find sequential chunks for context window reconstruction.
     * 
     * @param documentId the document ID
     * @param chunkIndex the sequential chunk index
     * @param tenantId the tenant ID
     * @return optional containing the chunk if found
     */
    Optional<DocumentChunk> findByDocumentIdAndChunkIndexAndTenantId(
            Long documentId, Integer chunkIndex, Long tenantId);

    /**
     * Find all chunks with a specific embedding status.
     * Used to identify chunks pending embedding or those that failed.
     * 
     * @param status the embedding status (PENDING, EMBEDDED, FAILED)
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of chunks with the specified status
     */
    Page<DocumentChunk> findByEmbeddingStatusAndTenantId(
            DocumentChunk.EmbeddingStatus status, Long tenantId, Pageable pageable);

    /**
     * Find chunks by document source type.
     * Enables filtering search results by document source (e.g., "PDF", "WIKI").
     * 
     * @param source the document source type
     * @param tenantId the tenant ID
     * @return list of chunks from the specified source
     */
    List<DocumentChunk> findByDocumentSourceAndTenantId(String source, Long tenantId);

    /**
     * Find chunks by document author.
     * Enables access control and user-specific result filtering.
     * 
     * @param author the document author
     * @param tenantId the tenant ID
     * @return list of chunks authored by the specified user
     */
    List<DocumentChunk> findByDocumentAuthorAndTenantId(String author, Long tenantId);

    /**
     * Count chunks for a document that have been successfully embedded.
     * Used to track embedding progress and determine if RAG retrieval is possible.
     * 
     * @param documentId the document ID
     * @param tenantId the tenant ID
     * @return count of embedded chunks
     */
    @Query("SELECT COUNT(c) FROM DocumentChunk c " +
           "WHERE c.documentId = :documentId " +
           "AND c.tenantId = :tenantId " +
           "AND c.embeddingStatus = 'EMBEDDED'")
    long countEmbeddedChunks(@Param("documentId") Long documentId, @Param("tenantId") Long tenantId);

    /**
     * Find chunks with failed embeddings for retry or investigation.
     * Used for monitoring and troubleshooting embedding pipeline.
     * 
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of chunks with FAILED embedding status
     */
    @Query("SELECT c FROM DocumentChunk c " +
           "WHERE c.tenantId = :tenantId " +
           "AND c.embeddingStatus = 'FAILED'")
    Page<DocumentChunk> findFailedChunks(@Param("tenantId") Long tenantId, Pageable pageable);

    /**
     * Get top chunks by retrieval count for a specific document.
     * Identifies the most frequently retrieved chunks (candidates for popularity ranking).
     * 
     * @param documentId the document ID
     * @param tenantId the tenant ID
     * @param limit maximum number of chunks to return
     * @return list of most retrieved chunks
     */
    @Query(value = "SELECT * FROM document_chunks " +
                   "WHERE document_id = :documentId " +
                   "AND tenant_id = :tenantId " +
                   "ORDER BY retrieval_count DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<DocumentChunk> findTopRetrievedChunks(
            @Param("documentId") Long documentId,
            @Param("tenantId") Long tenantId,
            @Param("limit") int limit);

    /**
     * Find chunks with above-average relevance scores.
     * Used for quality monitoring and relevance-based ranking.
     * 
     * @param minScore minimum average relevance score threshold
     * @param tenantId the tenant ID
     * @return list of high-quality chunks
     */
    @Query("SELECT c FROM DocumentChunk c " +
           "WHERE c.tenantId = :tenantId " +
           "AND c.averageRelevanceScore >= :minScore " +
           "AND c.embeddingStatus = 'EMBEDDED'")
    List<DocumentChunk> findHighQualityChunks(
            @Param("minScore") Double minScore,
            @Param("tenantId") Long tenantId);
}
