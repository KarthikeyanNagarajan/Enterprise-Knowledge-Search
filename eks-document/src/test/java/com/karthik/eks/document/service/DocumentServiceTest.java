package com.karthik.eks.document.service;

import com.karthik.eks.document.model.Document;
import com.karthik.eks.document.model.DocumentChunk;
import com.karthik.eks.document.repository.DocumentChunkRepository;
import com.karthik.eks.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void getAllDocumentsShouldReturnRepositoryResults() {
        Document one = Document.builder().title("Alpha").tenantId(1L).status(Document.DocumentStatus.ACTIVE).build();
        Document two = Document.builder().title("Beta").tenantId(1L).status(Document.DocumentStatus.ACTIVE).build();

        when(documentRepository.findAll()).thenReturn(List.of(one, two));

        List<Document> results = documentService.getAllDocuments();

        assertEquals(2, results.size());
        assertEquals("Alpha", results.get(0).getTitle());
        verify(documentRepository).findAll();
    }

    @Test
    void addDocumentShouldSaveAndReturnDocument() {
        Document document = Document.builder()
                .title("New Document")
                .content("content")
                .source("PDF")
                .author("user")
                .tenantId(7L)
                .status(Document.DocumentStatus.ACTIVE)
                .build();

        when(documentRepository.save(document)).thenReturn(document);

        Document saved = documentService.addDocument(document);

        assertNotNull(saved);
        assertEquals("New Document", saved.getTitle());
        verify(documentRepository).save(document);
    }

    @Test
    void getDocumentChunkShouldReturnChunkById() {
        DocumentChunk chunk = DocumentChunk.builder()
                .documentId(10L)
                .tenantId(5L)
                .chunkIndex(0)
                .content("chunk content")
                .tokens(8)
                .build();

        when(documentChunkRepository.findById(42L)).thenReturn(Optional.of(chunk));

        Optional<DocumentChunk> result = documentService.getDocumentChunk(42L);

        assertTrue(result.isPresent());
        assertEquals("chunk content", result.get().getContent());
        verify(documentChunkRepository).findById(42L);
    }

    @Test
    void getDocumentChunksShouldUseTenantScopedRepositoryQuery() {
        DocumentChunk first = DocumentChunk.builder()
                .documentId(10L)
                .tenantId(5L)
                .chunkIndex(0)
                .content("first")
                .tokens(4)
                .build();

        DocumentChunk second = DocumentChunk.builder()
                .documentId(10L)
                .tenantId(5L)
                .chunkIndex(1)
                .content("second")
                .tokens(5)
                .build();

        when(documentChunkRepository.findByDocumentIdAndTenantId(10L, 5L)).thenReturn(List.of(first, second));

        List<DocumentChunk> results = documentService.getDocumentChunks(10L, 5L);

        assertEquals(2, results.size());
        assertEquals("first", results.get(0).getContent());
        verify(documentChunkRepository).findByDocumentIdAndTenantId(10L, 5L);
    }
}
