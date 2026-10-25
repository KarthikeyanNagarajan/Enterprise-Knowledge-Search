package com.karthik.eks.rag.service;

import com.karthik.eks.common.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring AI equivalent of the existing manual RAG implementation.
 *
 * <p>This service intentionally lives side by side with {@link RagService},
 * {@link EmbeddingService}, and the other manual Ollama-based classes as a parallel
 * implementation for comparison and migration testing. It uses Spring AI's auto-configured
 * {@link ChatClient}, {@link EmbeddingModel}, and {@link VectorStore} instead of direct
 * WebClient calls to Ollama.
 */
@Service
public class SpringAiRagService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public SpringAiRagService(ChatClient chatClient, EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    /**
     * Ingest a text fragment into the configured vector store.
     *
     * <p>This is the Spring AI equivalent of the manual document ingestion flow used by the
     * existing RagService pipeline. The stored metadata keeps the document tied to its chunk
     * and tenant for future filtered retrieval.
     *
     * @param content the text content to index
     * @param chunkId the source document chunk identifier
     * @param tenantId the tenant that owns the content
     * @return a generated UUID for the stored document
     */
    public String ingest(String content, Long chunkId, Long tenantId) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("Cannot ingest empty content", HttpStatus.BAD_REQUEST);
        }

        try {
            String documentId = UUID.randomUUID().toString();
            Document document = Document.builder()
                    .id(documentId)
                    .text(content)
                    .metadata("chunkId", String.valueOf(chunkId))
                    .metadata("tenantId", String.valueOf(tenantId))
                    .build();

            vectorStore.add(List.of(document));
            return documentId;
        } catch (Exception e) {
            throw new BusinessException("Failed to ingest document into Spring AI vector store: " + e.getMessage(),
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Search for relevant documents using a tenant-scoped similarity search.
     *
     * <p>This method mirrors the manual similarity search logic in the older RAG service,
     * but delegates retrieval to the Spring AI {@link VectorStore} and applies a tenant filter.
     *
     * @param query the user query to search for
     * @param tenantId the tenant to isolate results for
     * @return a list of relevant documents sorted by relevance
     */
    public List<Document> similaritySearch(String query, Long tenantId) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(5)
                    .filterExpression(new FilterExpressionBuilder().eq("tenantId", tenantId).build())
                    .build();

            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            throw new BusinessException("Spring AI similarity search failed: " + e.getMessage(),
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Ask a question using the Spring AI RAG pipeline.
     *
     * <p>This is the parallel equivalent of the existing manual RagService.generateResponse call.
     * It retrieves relevant context, combines it, and asks the configured ChatClient to answer
     * based strictly on the supplied context.
     *
     * @param question the user question
     * @param tenantId the current tenant for scoped retrieval
     * @return the generated answer, or a fallback if no relevant context is available
     */
    public String ask(String question, Long tenantId) {
        try {
            List<Document> relevantDocuments = similaritySearch(question, tenantId);
            if (relevantDocuments == null || relevantDocuments.isEmpty()) {
                return "I don't have any relevant information to answer your question.";
            }

            String context = relevantDocuments.stream()
                    .map(Document::getText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n\n---\n\n"));

            if (context.isBlank()) {
                return "I don't have any relevant information to answer your question.";
            }

            return chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text("""
                                    Use the context below to answer the user's question.

                                    Context:
                                    {context}

                                    Question:
                                    {question}
                                    """)
                            .param("context", context)
                            .param("question", question))
                    .call()
                    .content();
        } catch (Exception e) {
            throw new BusinessException("Spring AI RAG ask operation failed: " + e.getMessage(),
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate an embedding for a text value using the Spring AI embedding model.
     *
     * <p>This mirrors the behavior of the older manual EmbeddingService but delegates to the
     * configured Spring AI embedding model rather than a direct Ollama WebClient request.
     *
     * @param text the text to embed
     * @return a float array containing the embedding vector
     */
    public float[] embed(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            throw new BusinessException("Spring AI embedding generation failed: " + e.getMessage(),
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
