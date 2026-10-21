package com.karthik.eks.chat.domain;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Conversation entity for storing chat history.
 * 
 * Fields:
 * - tenantId: Multi-tenant isolation
 * - userId: User who started the conversation
 * - title: Conversation title
 * - status: ACTIVE or ARCHIVED
 * - messageCount: Number of messages in conversation
 * 
 * Lifecycle:
 * 1. User starts new conversation
 * 2. Messages are added to conversation
 * 3. Conversation marked ARCHIVED when done
 */
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Integer messageCount = 0;

    /**
     * Conversation status.
     */
    public enum ConversationStatus {
        ACTIVE,      // Conversation in progress
        ARCHIVED     // Conversation closed/archived
    }
}
