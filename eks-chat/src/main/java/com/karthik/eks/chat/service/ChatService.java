package com.karthik.eks.chat.service;

import com.karthik.eks.chat.domain.Conversation;
import com.karthik.eks.chat.domain.Message;
import com.karthik.eks.chat.repository.ConversationRepository;
import com.karthik.eks.chat.repository.MessageRepository;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import com.karthik.eks.rag.service.RagService;
import com.karthik.eks.rag.service.SpringAiRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Chat Service - Chat Orchestration Layer.
 * 
 * Responsibilities:
 * 1. Manage conversations and message history
 * 2. Orchestrate RAG system for question answering
 * 3. Maintain conversation memory (recent messages)
 * 4. Provide conversation history and export
 * 
 * Architecture:
 * - Does NOT contain AI logic (that's RAG's job)
 * - Does NOT call LLM directly
 * - Calls RAG service to get AI responses
 * - Focuses on conversation management
 * 
 * Request Flow:
 * ChatController (v1) or ChatController (v2)
 *   ↓
 * ChatService (manages conversation)
 *   ↓
 * RagService or SpringAiRagService (generates response)
 *   ↓
 * Save message + response
 *   ↓
 * Return to user
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RagService ragService;
    private final SpringAiRagService springAiRagService;

    private static final int MEMORY_WINDOW = 10;  // Keep last 10 messages for context

    /**
     * Start a new conversation.
     * 
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param title conversation title
     * @return new Conversation
     */
    @Transactional
    public Conversation startConversation(Long tenantId, Long userId, String title) {
        log.info("Starting conversation for user: {} in tenant: {}", userId, tenantId);

        Conversation conversation = Conversation.builder()
                .tenantId(tenantId)
                .userId(userId)
                .title(title)
                .status(Conversation.ConversationStatus.ACTIVE)
                .messageCount(0)
                .build();
        conversation.setCreatedBy("USER");
        conversation.setUpdatedBy("USER");

        return conversationRepository.save(conversation);
    }

    /**
     * Send a message and get AI response.
     * 
     * Process:
     * 1. Save user message
     * 2. Get recent conversation history (memory window)
     * 3. Call RAG service to generate response
     * 4. Save AI response
     * 5. Return response to user
     * 
     * @param conversationId the conversation ID
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param userMessage the user's question
     * @return AI response
     */
    @Transactional
    public ChatResponse askQuestion(Long conversationId, Long tenantId, Long userId, String userMessage) {
        log.info("Processing question for conversation: {}", conversationId);

        // Verify conversation ownership
        Conversation conversation = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Save user message
        Message userMsg = Message.builder()
                .conversationId(conversationId)
                .tenantId(tenantId)
                .role(Message.MessageRole.USER)
                .content(userMessage)
                .tokenCount(estimateTokenCount(userMessage))
                .build();
        userMsg.setCreatedBy("USER");
        userMsg.setUpdatedBy("USER");

        messageRepository.save(userMsg);
        log.info("User message saved");

        // Get AI response from RAG
        String aiResponse = ragService.generateResponse(userMessage, tenantId);

        // Save AI response
        Message aiMsg = Message.builder()
                .conversationId(conversationId)
                .tenantId(tenantId)
                .role(Message.MessageRole.ASSISTANT)
                .content(aiResponse)
                .tokenCount(estimateTokenCount(aiResponse))
                .build();
        aiMsg.setCreatedBy("SYSTEM");
        aiMsg.setUpdatedBy("SYSTEM");

        messageRepository.save(aiMsg);
        log.info("AI response saved");

        // Update conversation message count
        conversation.setMessageCount(conversation.getMessageCount() + 2);
        conversationRepository.save(conversation);

        return ChatResponse.builder()
                .conversationId(conversationId)
                .userMessage(userMessage)
                .aiResponse(aiResponse)
                .build();
    }

    /**
     * Send a message and get an AI response using the Spring AI-backed RAG engine.
     *
     * <p>This is the v2 equivalent of {@link #askQuestion(Long, Long, Long, String)} and
     * intentionally keeps the same conversation/message persistence logic. Only the actual
     * retrieval-and-generation backend changes to the Spring AI implementation.
     *
     * @param conversationId the conversation ID
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param userMessage the user's question
     * @return AI response wrapped in the same chat response format used by v1
     */
    @Transactional
    public ChatResponse askQuestionWithSpringAi(Long conversationId, Long tenantId, Long userId, String userMessage) {
        log.info("Processing Spring AI question for conversation: {}", conversationId);

        Conversation conversation = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Message userMsg = Message.builder()
                .conversationId(conversationId)
                .tenantId(tenantId)
                .role(Message.MessageRole.USER)
                .content(userMessage)
                .tokenCount(estimateTokenCount(userMessage))
                .build();
        userMsg.setCreatedBy("USER");
        userMsg.setUpdatedBy("USER");

        messageRepository.save(userMsg);
        log.info("User message saved");

        String aiResponse = springAiRagService.ask(userMessage, tenantId);

        Message aiMsg = Message.builder()
                .conversationId(conversationId)
                .tenantId(tenantId)
                .role(Message.MessageRole.ASSISTANT)
                .content(aiResponse)
                .tokenCount(estimateTokenCount(aiResponse))
                .build();
        aiMsg.setCreatedBy("SYSTEM");
        aiMsg.setUpdatedBy("SYSTEM");

        messageRepository.save(aiMsg);
        log.info("AI response saved");

        conversation.setMessageCount(conversation.getMessageCount() + 2);
        conversationRepository.save(conversation);

        return ChatResponse.builder()
                .conversationId(conversationId)
                .userMessage(userMessage)
                .aiResponse(aiResponse)
                .build();
    }

    /**
     * Get recent conversation history (memory window).
     * 
     * Returns the last N messages for context in prompts.
     * Helps LLM maintain conversation context.
     * 
     * @param conversationId the conversation ID
     * @param tenantId the tenant ID
     * @return list of recent messages
     */
    @Transactional(readOnly = true)
    public List<Message> getConversationHistory(Long conversationId, Long tenantId) {
        conversationRepository.findByIdAndTenantId(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        return messageRepository.findRecentMessages(conversationId, MEMORY_WINDOW);
    }

    /**
     * Archive a conversation.
     * 
     * @param conversationId the conversation ID
     * @param tenantId the tenant ID
     */
    @Transactional
    public void archiveConversation(Long conversationId, Long tenantId) {
        Conversation conversation = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        conversation.setStatus(Conversation.ConversationStatus.ARCHIVED);
        conversationRepository.save(conversation);
        log.info("Conversation {} archived", conversationId);
    }

    /**
     * List user's conversations.
     * 
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @return list of conversations
     */
    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(Long tenantId, Long userId) {
        return conversationRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    /**
     * Estimate token count for cost tracking.
     * 
     * Rough estimate: ~4 characters = 1 token
     * Based on typical LLM tokenization patterns.
     * 
     * @param text the text
     * @return estimated token count
     */
    private int estimateTokenCount(String text) {
        return Math.max(1, text.length() / 4);
    }

    /**
     * Response DTO for chat interaction.
     */
    @lombok.Data
    @lombok.Builder
    public static class ChatResponse {
        private Long conversationId;
        private String userMessage;
        private String aiResponse;
    }
}
