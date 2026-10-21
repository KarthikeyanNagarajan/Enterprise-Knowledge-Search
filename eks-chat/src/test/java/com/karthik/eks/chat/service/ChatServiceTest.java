package com.karthik.eks.chat.service;

import com.karthik.eks.chat.domain.Conversation;
import com.karthik.eks.chat.domain.Message;
import com.karthik.eks.chat.repository.ConversationRepository;
import com.karthik.eks.chat.repository.MessageRepository;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import com.karthik.eks.rag.service.RagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private RagService ragService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void startConversationShouldPersistNewConversation() {
        Conversation conversation = Conversation.builder()
                .tenantId(1L)
                .userId(2L)
                .title("Hello")
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        Conversation saved = chatService.startConversation(1L, 2L, "Hello");

        assertEquals("Hello", saved.getTitle());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void askQuestionShouldReturnAiResponseAndSaveMessages() {
        Conversation conversation = Conversation.builder()
                .tenantId(1L)
                .userId(2L)
                .title("Chat")
                .messageCount(0)
                .build();
        conversation.setId(99L);

        when(conversationRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.of(conversation));
        when(ragService.generateResponse("question", 1L)).thenReturn("answer");

        ChatService.ChatResponse response = chatService.askQuestion(99L, 1L, 2L, "question");

        assertEquals("answer", response.getAiResponse());
        verify(messageRepository, times(2)).save(any(Message.class));
        verify(conversationRepository).save(conversation);
    }

    @Test
    void getConversationHistoryShouldThrowWhenConversationMissing() {
        when(conversationRepository.findByIdAndTenantId(4L, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> chatService.getConversationHistory(4L, 1L));

        assertEquals("Conversation not found", ex.getMessage());
    }

    @Test
    void getUserConversationsShouldReturnTenantUserResults() {
        Conversation c = Conversation.builder().tenantId(1L).userId(2L).title("A").build();
        when(conversationRepository.findByTenantIdAndUserId(1L, 2L)).thenReturn(List.of(c));

        List<Conversation> results = chatService.getUserConversations(1L, 2L);

        assertEquals(1, results.size());
        assertEquals("A", results.get(0).getTitle());
    }
}
