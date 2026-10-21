package com.karthik.eks.rag.domain;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

/**
 * Vector Embedding Store for similarity search.
 * 
 * This entity stores the generated embeddings using pgvector.
 * pgvector is a PostgreSQL extension for storing and searching vector embeddings.
 * 
 * Fields:
 * - chunkId: Reference to original document chunk
 * - tenantId: For multi-tenant isolation
 * - embedding: The vector embedding (1536 dims for OpenAI/Ollama embeddings) stored as pgvector
 * - modelName: Which embedding model was used (for versioning)
 * 
 * Database Requirement:
 * Your PostgreSQL 17 instance must have pgvector extension installed:
 * CREATE EXTENSION vector;
 * 
 * Indexes:
 * - pgvector creates automatic HNSW index for efficient vector search (L2 distance)
 */
@Entity
@Table(name = "vector_embeddings", indexes = {
        @Index(name = "idx_chunk_id", columnList = "chunk_id"),
        @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VectorEmbedding extends BaseEntity {

    @Column(nullable = false)
    private Long chunkId;

    @Column(nullable = false)
    private Long tenantId;

    /**
     * Vector embedding stored as pgvector type.
     * 
     * SQL definition:
     * embedding vector NOT NULL
     * 
     * Stored as string representation in Java, mapped to pgvector column in DB.
     * Format: "[0.1, 0.2, 0.3, ...]"
     * 
     * Ollama embedding default: 384 or 768 dimensions
     * (Can be configured based on your Ollama model)
     */
    @JdbcTypeCode(Types.OTHER)
    @Column(nullable = false, columnDefinition = "vector")
    private String embedding;

    @Column(nullable = false)
    private String modelName;  // e.g., "nomic-embed-text:latest"

    /**
     * Get embedding vector as float array.
     * Used for similarity calculations in memory.
     */
    public float[] getEmbeddingArray() {
        if (embedding == null || embedding.isEmpty()) {
            return new float[0];
        }
        
        String vectorStr = embedding;
        // Parse pgvector format: "[0.1, 0.2, 0.3, ...]"
        vectorStr = vectorStr.replace("[", "").replace("]", "");
        String[] parts = vectorStr.split(",");
        float[] result = new float[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        
        return result;
    }
}
