package com.karthik.eks.rag.controller;

import com.karthik.eks.rag.service.SpringAiRagService;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Spring AI-backed RAG controller.
 *
 * <p>This controller runs in parallel with the existing manual RAG endpoints and is kept
 * separate so the legacy implementation can remain untouched for comparison or fallback.
 */
@RestController
@RequestMapping("/rag/spring-ai")
public class SpringAiRagController {

    private final SpringAiRagService springAiRagService;

    public SpringAiRagController(SpringAiRagService springAiRagService) {
        this.springAiRagService = springAiRagService;
    }

    /**
     * Ingest a new content unit into the Spring AI vector store.
     *
     * @param request the ingestion payload
     * @return the generated document identifier
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingest(@RequestBody IngestRequest request) {
        String documentId = springAiRagService.ingest(request.content(), request.chunkId(), request.tenantId());
        return ResponseEntity.ok(Map.of("documentId", documentId));
    }

    /**
     * Search for documents semantically matching the supplied query and tenant.
     *
     * @param query the search intent
     * @param tenantId tenant-scoped search limit
     * @return matching documents
     */
    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestParam String query, @RequestParam Long tenantId) {
        return ResponseEntity.ok(springAiRagService.similaritySearch(query, tenantId));
    }

    /**
     * Ask a question using the Spring AI retrieval pipeline.
     *
     * @param question the input question
     * @param tenantId tenant-scoped retrieval context
     * @return answer payload
     */
    @GetMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestParam String question, @RequestParam Long tenantId) {
        String answer = springAiRagService.ask(question, tenantId);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    /**
     * Ingestion request payload.
     *
     * @param content the content to index
     * @param chunkId the originating chunk identifier
     * @param tenantId the owning tenant
     */
    public record IngestRequest(String content, Long chunkId, Long tenantId) {
    }
}
