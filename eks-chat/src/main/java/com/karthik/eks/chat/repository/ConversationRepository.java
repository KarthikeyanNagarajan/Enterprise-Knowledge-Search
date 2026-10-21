package com.karthik.eks.chat.repository;

import com.karthik.eks.chat.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Conversation entity.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Find all conversations for a user in tenant.
     */
    List<Conversation> findByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Find conversation by ID in tenant (verify ownership).
     */
    Optional<Conversation> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Find active conversations.
     */
    List<Conversation> findByTenantIdAndStatus(Long tenantId, Conversation.ConversationStatus status);

    /**
     * Count conversations for a user.
     */
    long countByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Count total conversations in a tenant.
     */
    long countByTenantId(Long tenantId);
}
