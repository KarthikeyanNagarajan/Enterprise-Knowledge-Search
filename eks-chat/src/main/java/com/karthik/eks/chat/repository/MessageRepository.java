package com.karthik.eks.chat.repository;

import com.karthik.eks.chat.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find all messages in a conversation.
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Find recent messages (for context window).
     * 
     * SQL: SELECT * FROM messages WHERE conversation_id = :conversationId
     *      ORDER BY created_at DESC LIMIT :limit
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM messages WHERE conversation_id = :conversationId " +
                    "ORDER BY created_at DESC LIMIT :limit")
    List<Message> findRecentMessages(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    /**
     * Count messages in conversation.
     */
    long countByConversationId(Long conversationId);

    /**
     * Delete all messages in conversation.
     */
    void deleteByConversationId(Long conversationId);
}
