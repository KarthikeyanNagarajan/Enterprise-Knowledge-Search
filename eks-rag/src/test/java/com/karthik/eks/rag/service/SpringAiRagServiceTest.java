package com.karthik.eks.rag.service;

import com.karthik.eks.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringAiRagServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private SpringAiRagService springAiRagService;

    @Test
    void ingestShouldRejectBlankContent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> springAiRagService.ingest("  ", 1L, 7L));

        assertEquals("Cannot ingest empty content", ex.getMessage());
        verify(vectorStore, never()).add(any());
    }

    @Test
    void ingestShouldStoreDocumentWithTenantAndChunkMetadata() {
        String documentId = springAiRagService.ingest("some chunk text", 10L, 7L);

        assertNotNull(documentId);
        assertEquals(36, documentId.length());
        verify(vectorStore).add(any());

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        List<?> docs = captor.getValue();
        assertEquals(1, docs.size());
        Document doc = (Document) docs.get(0);
        assertEquals("some chunk text", doc.getText());
        assertEquals("10", doc.getMetadata().get("chunkId"));
        assertEquals("7", doc.getMetadata().get("tenantId"));
    }

    @Test
    void similaritySearchShouldDelegateToVectorStore() {
        Document document = new Document("doc-1", "relevant content", Map.of("tenantId", "7"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));

        List<Document> results = springAiRagService.similaritySearch("hello", 7L);

        assertEquals(1, results.size());
        assertEquals("relevant content", results.get(0).getText());
    }

    @Test
    void askShouldReturnFallbackWhenNoRelevantDocumentsFound() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String result = springAiRagService.ask("missing context", 9L);

        assertEquals("I don't have any relevant information to answer your question.", result);
        verifyNoInteractions(chatClient);
    }

    @Test
    void embedShouldDelegateToEmbeddingModel() {
        float[] expected = new float[] {0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed("hello")).thenReturn(expected);

        float[] actual = springAiRagService.embed("hello");

        assertArrayEquals(expected, actual);
    }
}
