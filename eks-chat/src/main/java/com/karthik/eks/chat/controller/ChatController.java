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
 * Chat Controller - Conversation API.
 * 
 * Endpoints:
 * - POST /api/v1/chat/conversations: Start new conversation
 * - POST /api/v1/chat/conversations/{id}/messages: Send message and get response
 * - GET /api/v1/chat/conversations: List conversations
 * - GET /api/v1/chat/conversations/{id}/history: Get conversation history
 * - PUT /api/v1/chat/conversations/{id}/archive: Archive conversation
 * 
 * Authentication: Required (JWT token)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final TenantService tenantService;

    /**
     * Start a new conversation.
     * 
     * @param request start conversation request
     * @return created Conversation
     */
    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<Conversation>> startConversation(
            @RequestBody StartConversationRequest request) {

        Long tenantId = tenantService.getCurrentTenant().getId();
        // Note: In real app, get userId from authenticated user
        Long userId = 1L;

        log.info("Starting conversation for user: {} in tenant: {}", userId, tenantId);

        Conversation conversation = chatService.startConversation(tenantId, userId, request.getTitle());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation started", conversation));
    }

    /**
     * Send a message and get AI response.
     * 
     * Process:
     * 1. Save user message
     * 2. Generate AI response using RAG
     * 3. Save AI response
     * 4. Return both messages
     * 
     * @param conversationId the conversation ID
     * @param request the message request
     * @return Chat response with user message and AI response
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ChatService.ChatResponse>> askQuestion(
            @PathVariable Long conversationId,
            @RequestBody AskQuestionRequest request) {

        Long tenantId = tenantService.getCurrentTenant().getId();
        // Note: In real app, get userId from authenticated user
        Long userId = 1L;

        log.info("Processing question for conversation: {} in tenant: {}", conversationId, tenantId);

        ChatService.ChatResponse response = chatService.askQuestion(
                conversationId,
                tenantId,
                userId,
                request.getMessage()
        );

        return ResponseEntity.ok(ApiResponse.success("Response generated", response));
    }

    /**
     * Get conversation history.
     * 
     * @param conversationId the conversation ID
     * @return Recent messages in conversation
     */
    @GetMapping("/conversations/{conversationId}/history")
    public ResponseEntity<ApiResponse<List<Message>>> getConversationHistory(
            @PathVariable Long conversationId) {

        Long tenantId = tenantService.getCurrentTenant().getId();

        List<Message> messages = chatService.getConversationHistory(conversationId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Conversation history retrieved", messages));
    }

    /**
     * List user's conversations.
     * 
     * @return List of conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Conversation>>> listConversations() {
        Long tenantId = tenantService.getCurrentTenant().getId();
        // Note: In real app, get userId from authenticated user
        Long userId = 1L;

        List<Conversation> conversations = chatService.getUserConversations(tenantId, userId);

        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved", conversations));
    }

    /**
     * Archive a conversation.
     * 
     * @param conversationId the conversation ID
     */
    @PutMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @PathVariable Long conversationId) {

        Long tenantId = tenantService.getCurrentTenant().getId();

        chatService.archiveConversation(conversationId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Conversation archived", null));
    }

    /**
     * Start conversation request DTO.
     */
    @lombok.Data
    public static class StartConversationRequest {
        private String title;
    }

    /**
     * Ask question request DTO.
     */
    @lombok.Data
    public static class AskQuestionRequest {
        private String message;
    }
}
