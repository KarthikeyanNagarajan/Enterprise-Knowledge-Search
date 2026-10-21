# EKS RAG Module

## Purpose

The `eks-rag` module is the retrieval and generation engine of the project. It integrates embeddings, vector search, LLM interactions, and result ranking to provide semantic retrieval from stored documents.

## Stack Used

- Java 21
- Spring Boot 3.5.0
- PostgreSQL
- pgvector
- Spring Data JPA
- Spring WebFlux
- Ollama integration
- Lombok

## Responsibilities

- Store embedding vectors
- Query vector similarity results
- Integrate with local LLMs via Ollama
- Combine lexical and semantic search results
- Rank results for quality and relevance
- Support search feedback analytics

## Key Components

### VectorEmbedding

Stores embedding text in pgvector format for semantic retrieval.

### SearchFeedback

Captures ratings and feedback on search results for quality monitoring and reranking.

### HybridSearchService

Combines multiple retrieval strategies for better result quality.

### Ollama integration

Connects to local inference and embedding models running through Ollama for LLM generation and vector generation.

## Working Flow

1. Document chunks are generated in the document module.
2. Embeddings are created using the configured embedding model.
3. Embeddings are stored in the vector table.
4. Search requests use similarity search and ranking logic.
5. The LLM may synthesize a response using the retrieved context.

## Structure

```text
eks-rag/
├── pom.xml
├── README.md
├── src/main/java/com/karthik/eks/rag/
│   ├── config/
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
```

## Typical Usage

This module powers:

- semantic search
- document retrieval
- contextual answer generation
- ranking improvements based on results and feedback

## Local Execution Dependency

The application expects:

- PostgreSQL with pgvector enabled
- local Ollama service available on `http://localhost:11434`
- embedding model and LLM model configured in application properties

## Use Case

This is the core intelligence layer that turns raw documents into searchable and answerable knowledge.
