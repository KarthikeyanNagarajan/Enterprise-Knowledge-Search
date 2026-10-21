package com.karthik.eks.document.model;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Document Model - Represents uploaded documents with metadata support.
 * 
 * Metadata Enhancements:
 * - Tracks document source, author, upload date for better filtering
 * - Supports document status (active, archived, deleted)
 * - Enables access control and auditing
 * 
 * Relationships:
 * - One Document has many DocumentChunks (for RAG processing)
 * - Belongs to a Tenant (multi-tenancy support)
 * 
 * Data Flow:
 * 1. Document uploaded → stored with metadata
 * 2. Chunked into DocumentChunks
 * 3. Chunks embedded and stored in vector database
 * 4. RAG retrieves relevant chunks for search results
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_doc_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_doc_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    /**
     * Unique identifier for the document.
     */
    @Column(nullable = false)
    private String title;
    
    /**
     * Full content of the document (before chunking).
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Document source/type for filtering (e.g., "PDF", "TXT", "WIKI", "CONFLUENCE").
     */
    @Column(length = 50)
    private String source;

    /**
     * User who uploaded this document - for access control and auditing.
     */
    @Column(length = 255)
    private String author;

    /**
     * File path or URL reference for retrieving the source document.
     */
    @Column(length = 500)
    private String filePath;

    /**
     * Document status: ACTIVE, ARCHIVED, DELETED.
     * Deleted documents are soft-deleted (not physically removed).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.ACTIVE;

    /**
     * Total number of tokens in the document (for billing/tracking).
     */
    private Integer totalTokens;

    /**
     * File MIME type (e.g., "application/pdf", "text/plain").
     */
    @Column(length = 100)
    private String mimeType;

    /**
     * Document version (for tracking updates/revisions).
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * Tenant ID for multi-tenancy support and data isolation.
     */
    @Column(nullable = false)
    private Long tenantId;

    /**
     * Document status enumeration.
     */
    public enum DocumentStatus {
        ACTIVE,        // Document is currently in use
        ARCHIVED,      // Document is archived (can be restored)
        DELETED        // Document is soft-deleted
    }
}