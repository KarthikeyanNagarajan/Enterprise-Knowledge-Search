package com.karthik.eks.rag.repository;

import com.karthik.eks.rag.domain.VectorEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for VectorEmbedding entity.
 * 
 * Uses pgvector similarity search queries.
 * The <-> operator performs L2 distance similarity search in pgvector.
 */
@Repository
public interface VectorEmbeddingRepository extends JpaRepository<VectorEmbedding, Long> {

    /**
     * Find embedding by chunk ID.
     */
    Optional<VectorEmbedding> findByChunkId(Long chunkId);

    /**
     * Similarity search - find top K embeddings similar to query vector.
     * 
     * SQL: SELECT * FROM vector_embeddings WHERE tenant_id = :tenantId
     *      ORDER BY embedding <-> :queryVector LIMIT :limit
     * 
     * The <-> operator calculates L2 (Euclidean) distance.
     * Smaller distance = more similar.
     * 
     * @param queryVector the query embedding as string "[0.1, 0.2, ...]"
     * @param tenantId the tenant for isolation
     * @param limit number of results to return
     * @return list of similar embeddings
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM vector_embeddings " +
                    "WHERE tenant_id = :tenantId " +
                    "ORDER BY embedding <-> CAST(:queryVector AS vector) LIMIT :limit")
    List<VectorEmbedding> findSimilarEmbeddings(
            @Param("queryVector") String queryVector,
            @Param("tenantId") Long tenantId,
            @Param("limit") int limit);

    /**
     * Find all embeddings for a chunk (usually just 1).
     */
    List<VectorEmbedding> findByTenantId(Long tenantId);

    /**
     * Delete embeddings for a chunk (when chunk is deleted).
     */
    void deleteByChunkId(Long chunkId);

    /**
     * Delete all embeddings for chunks in a document.
     */
    @Query(nativeQuery = true,
            value = "DELETE FROM vector_embeddings " +
                    "WHERE chunk_id IN (SELECT id FROM document_chunks WHERE document_id = :documentId)")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}
