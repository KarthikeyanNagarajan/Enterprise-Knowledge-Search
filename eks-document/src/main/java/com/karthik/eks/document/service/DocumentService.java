package com.karthik.eks.document.service;

import com.karthik.eks.document.model.Document;
import com.karthik.eks.document.model.DocumentChunk;
import com.karthik.eks.document.repository.DocumentRepository;
import com.karthik.eks.document.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    public DocumentService(DocumentRepository documentRepository, DocumentChunkRepository documentChunkRepository) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Document addDocument(Document document) {
        return documentRepository.save(document);
    }

    /**
     * Get a document chunk by its ID.
     * Used by RAG service to fetch chunk details after vector search.
     * 
     * @param chunkId the chunk ID
     * @return Optional containing the chunk if found
     */
    public Optional<DocumentChunk> getDocumentChunk(Long chunkId) {
        return documentChunkRepository.findById(chunkId);
    }

    /**
     * Get all chunks for a document (with tenant isolation).
     * 
     * @param documentId the document ID
     * @param tenantId the tenant ID for security isolation
     * @return list of chunks
     */
    public List<DocumentChunk> getDocumentChunks(Long documentId, Long tenantId) {
        return documentChunkRepository.findByDocumentIdAndTenantId(documentId, tenantId);
    }
}