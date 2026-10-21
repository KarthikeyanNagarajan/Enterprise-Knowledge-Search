package com.karthik.eks.rag.service;

import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.document.model.DocumentChunk;
import com.karthik.eks.document.service.DocumentService;
import com.karthik.eks.rag.domain.VectorEmbedding;
import com.karthik.eks.rag.repository.VectorEmbeddingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private VectorEmbeddingRepository vectorEmbeddingRepository;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private RagService ragService;

    @Test
    void similaritySearchShouldReturnRelevantContextsFromStoredEmbeddings() {
        RagService service = spy(new RagService(documentService, vectorEmbeddingRepository, webClient));

        DocumentChunk chunk = DocumentChunk.builder()
                .documentId(1L)
                .tenantId(7L)
                .chunkIndex(0)
                .content("relevant context")
                .tokens(4)
                .build();
        chunk.setId(10L);

        VectorEmbedding embedding = VectorEmbedding.builder()
                .chunkId(10L)
                .tenantId(7L)
                .embedding("[1.0, 0.0]")
                .modelName("test-model")
                .build();

        doReturn(new float[]{1.0f, 0.0f}).when(service).generateEmbedding("hello");
        when(vectorEmbeddingRepository.findSimilarEmbeddings(anyString(), eq(7L), eq(5)))
                .thenReturn(List.of(embedding));
        when(documentService.getDocumentChunk(10L)).thenReturn(Optional.of(chunk));

        List<RagService.RelevantContext> result = service.similaritySearch("hello", 7L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getChunkId());
    }

    @Test
    void generateResponseShouldReturnHelpfulFallbackWhenNoContextIsFound() {
        RagService service = spy(new RagService(documentService, vectorEmbeddingRepository, webClient));

        doReturn(List.of()).when(service).similaritySearch("missing context", 9L);

        String response = service.generateResponse("missing context", 9L);

        assertEquals("I don't have any relevant information to answer your question.", response);
    }

    @Test
    void generateEmbeddingShouldRejectMissingResponse() {
        RagService service = spy(new RagService(documentService, vectorEmbeddingRepository, webClient));

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RagService.EmbeddingResponse.class)).thenReturn(Mono.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.generateEmbedding("hello"));

        assertEquals("Failed to generate embedding: Failed to generate embedding", ex.getMessage());
    }
}
