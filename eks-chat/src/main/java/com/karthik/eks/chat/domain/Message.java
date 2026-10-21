package com.karthik.eks.chat.domain;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Message entity storing individual messages in a conversation.
 * 
 * Fields:
 * - conversationId: Parent conversation
 * - tenantId: Multi-tenant isolation
 * - role: Who sent the message (USER or ASSISTANT)
 * - content: The message text
 * - tokenCount: Tokens used in this message (for cost tracking)
 * 
 * Indexing:
 * - Quick lookup by conversation
 * - Enables memory window calculations
 * - Supports conversation export/audit
 */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_conversation_id", columnList = "conversation_id"),
        @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Integer tokenCount = 0;

    /**
     * Who sent the message.
     */
    public enum MessageRole {
        USER,       // Human user
        ASSISTANT   // AI assistant
    }
}
