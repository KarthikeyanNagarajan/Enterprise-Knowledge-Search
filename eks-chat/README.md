# EKS Chat Module

## Purpose

The `eks-chat` module manages conversational workflows. It stores chats, conversation history, and user messages while connecting them with the RAG retrieval layer to provide context-aware conversational search.

## Stack Used

- Java 21
- Spring Boot 3.5.0
- Spring Data JPA
- Spring Web
- PostgreSQL
- Validation

## Responsibilities

- Conversation lifecycle management
- Message persistence
- Chat session tracking
- Integration with the RAG pipeline for contextual retrieval
- User-facing chat orchestration

## Core Entities

### Conversation

Represents a chat session, containing:

- tenant id
- user id
- title
- status
- message count

### Message

Represents one individual message in the conversation, including:

- role (USER or ASSISTANT)
- content
- token count
- conversation id

## Working Model

1. User starts a conversation.
2. Messages are stored against the conversation.
3. The chat logic can combine conversation history with retrieved document context.
4. Responses are returned to the user with retrieval support.

## Structure

```text
eks-chat/
├── pom.xml
├── README.md
├── src/main/java/com/karthik/eks/chat/
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
```

## Typical Usage

This module is used for:

- chat APIs
- conversation history retrieval
- assistant interaction flows
- contextual answer generation

## Use Case

This module connects the user-facing conversation experience with the retrieval and generation layers of the platform.
