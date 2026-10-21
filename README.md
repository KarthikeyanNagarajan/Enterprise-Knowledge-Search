# Enterprise Knowledge Search (EKS)

Enterprise Knowledge Search is a multi-module Java Spring Boot application for building a production-style knowledge platform with multi-tenancy, JWT authentication, document ingestion, vector search, conversational RAG, and agent-driven workflows.

## Overview

This project is designed to provide an end-to-end enterprise knowledge experience:

- Secure user and tenant management
- Document upload and metadata tracking
- Chunking and embedding generation
- Vector similarity retrieval using PostgreSQL pgvector
- Local LLM integration with Ollama
- Chat-based conversational retrieval
- Agent and workflow orchestration

The application is modularized so each layer has a distinct responsibility and each Maven module can evolve independently without creating circular dependencies.

## Tech Stack

- Java 21
- Maven multi-module build
- Spring Boot 3.5.0
- Spring Data JPA
- Spring Security
- PostgreSQL 17
- pgvector extension
- Flyway for database migrations
- JWT for authentication
- Ollama for local LLM and embedding models
- Hibernate ORM

## Module Architecture

The project is separated into the following modules:

- `eks-app` — bootstraps the runnable application
- `eks-common` — shared utilities, base entities, common configuration, data model support
- `eks-identity` — users, tenants, JWT security, authentication flow
- `eks-document` — document and document-chunk domain layer
- `eks-rag` — retrieval, embeddings, vector search, LLM integration
- `eks-chat` — conversation and messaging services
- `eks-agent` — agent workflow automation and orchestration

## Responsibilities and Design Principles

- `eks-common` is a shared library and should not run the application by itself.
- The actual Spring Boot entry point exists only in `eks-app`.
- Each module owns a business concern and exposes services or domain APIs for other modules.
- Database schema is managed by Flyway migrations.
- Shared cross-cutting concerns such as authentication and audit fields are centralized in common abstractions.

## Project Structure

```text
enterprise-knowledge-search/
├── pom.xml
├── README.md
├── .gitignore
├── eks-app/
│   ├── pom.xml
│   └── src/main/java/com/karthik/eks/
├── eks-common/
│   ├── pom.xml
│   └── src/main/java/com/karthik/eks/common
├── eks-identity/
│   ├── pom.xml
│   └── src/main/java/com/karthik/eks/identity
├── eks-document/
│   ├── pom.xml
│   └── src/main/java/com/karthik/eks/document
├── eks-rag/
│   ├── pom.xml
│   └── src/main/java/com/karthik/eks/rag
├── eks-chat/
│   ├── pom.xml
│   └── src/main/java/com/karthik/eks/chat
├── eks-agent/
│   ├── pom.xml
│   └── src/main/java/com/karthik/eks/agent
└── src/
    └── main/java/com/karthik/
```

## Local Prerequisites

Before running the application locally, make sure the following are installed:

- Java 21
- Maven
- PostgreSQL 17
- pgvector extension enabled
- Ollama with models available locally

Recommended local setup:

- Database: `enterprise_knowledge_search`
- User: `postgres`
- Password: `postgres`
- pgvector extension: `CREATE EXTENSION IF NOT EXISTS vector;`
- Ollama model: `llama3:latest`
- Embedding model: `nomic-embed-text:latest`

## Run the Project

From the root directory:

```bash
mvn clean install -DskipTests
cd eks-app
mvn spring-boot:run
```

Then open:

- Application: http://localhost:8080/api
- Health endpoint: http://localhost:8080/api/actuator/health

## Typical Runtime Flow

1. User registers/logs in through the identity layer.
2. Documents are uploaded and stored with metadata.
3. Documents are chunked and embedded into vector storage.
4. RAG services run similarity search over embeddings.
5. Chat services connect user queries with retrieval and LLM generation.
6. Agent workflows can orchestrate multi-step execution across resources.

## Key Features

- Multi-tenant design
- JWT-based security
- Role-based access
- Document management
- Vector search
- Conversational AI
- Agent workflows
- Database migration support via Flyway

## Notes for Contributors

- Do not place the application bootstrapping inside shared library modules.
- Keep `eks-common` focused on reusable abstractions and shared infrastructure.
- Run migrations through Flyway and validate schema changes before production use.
- Use `eks-app` as the single runtime entry point for local execution and deployment.

## Summary

This repository is a structured enterprise RAG platform that balances domain separation, runtime clarity, and practical operational needs. The architecture is intentionally modular so the project is easier to maintain, test, and extend as a real-world product.
