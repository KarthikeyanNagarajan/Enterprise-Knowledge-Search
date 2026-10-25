package com.karthik.eks.chat.controller;

import com.karthik.eks.chat.domain.Conversation;
import com.karthik.eks.chat.domain.Message;
import com.karthik.eks.chat.service.ChatService;
import com.karthik.eks.common.response.ApiResponse;
import com.karthik.eks.identity.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V2 chat API that mirrors {@link ChatController} but routes message generation through
 * the Spring AI-backed RAG implementation instead of the legacy manual Ollama pipeline.
 *
 * <p>Both v1 and v2 share the same conversation/message persistence logic; only the
 * retrieval engine changes between implementations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/chat")
@RequiredArgsConstructor
public class ChatControllerV2 {

    private final ChatService chatService;
    private final TenantService tenantService;

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<Conversation>> startConversation(
            @RequestBody ChatController.StartConversationRequest request) {

        Long tenantId = tenantService.getCurrentTenant().getId();
        Long userId = 1L;

        log.info("Starting conversation for user: {} in tenant: {}", userId, tenantId);

        Conversation conversation = chatService.startConversation(tenantId, userId, request.getTitle());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation started", conversation));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ChatService.ChatResponse>> askQuestion(
            @PathVariable Long conversationId,
            @RequestBody ChatController.AskQuestionRequest request) {

        Long tenantId = tenantService.getCurrentTenant().getId();
        Long userId = 1L;

        log.info("Processing question for conversation: {} in tenant: {}", conversationId, tenantId);

        ChatService.ChatResponse response = chatService.askQuestionWithSpringAi(
                conversationId,
                tenantId,
                userId,
                request.getMessage()
        );

        return ResponseEntity.ok(ApiResponse.success("Response generated", response));
    }

    @GetMapping("/conversations/{conversationId}/history")
    public ResponseEntity<ApiResponse<List<Message>>> getConversationHistory(
            @PathVariable Long conversationId) {

        Long tenantId = tenantService.getCurrentTenant().getId();
        List<Message> messages = chatService.getConversationHistory(conversationId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Conversation history retrieved", messages));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Conversation>>> listConversations() {
        Long tenantId = tenantService.getCurrentTenant().getId();
        Long userId = 1L;

        List<Conversation> conversations = chatService.getUserConversations(tenantId, userId);

        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved", conversations));
    }

    @PutMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @PathVariable Long conversationId) {

        Long tenantId = tenantService.getCurrentTenant().getId();
        chatService.archiveConversation(conversationId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Conversation archived", null));
    }
}
