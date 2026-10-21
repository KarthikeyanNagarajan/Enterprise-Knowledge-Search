# EKS Document Module

## Purpose

The `eks-document` module manages document ingestion and metadata handling. It is responsible for storing the uploaded content and the chunked representation used later by the retrieval pipeline.

## Stack Used

- Java 21
- Spring Boot 3.5.0
- Spring Data JPA
- PostgreSQL
- Lombok

## Responsibilities

- Document domain model management
- Document metadata persistence
- Chunk-level metadata storage
- Tracking source, author, tenant, status, and version details
- Supporting retrieval and indexing features from the RAG layer

## Core Entities

### Document

Represents the uploaded document and tracks:

- title
- content
- source
- author
- file path
- status
- tenant id
- version

### DocumentChunk

Represents a chunk of the document after splitting. It stores:

- document id
- tenant id
- chunk index
- chunk text
- token count
- embedding status
- section metadata
- page number
- document author and source
- relevance metadata

## Working Model

1. A document is uploaded and saved.
2. It is split into chunks.
3. Each chunk is assigned metadata for retrieval and filtering.
4. The RAG module embeds the chunk content and stores vectors for semantic search.

## Structure

```text
eks-document/
├── pom.xml
├── README.md
├── src/main/java/com/karthik/eks/document/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
```

## Typical Usage

This module is used to:

- store files and metadata
- query documents by tenant or status
- support chunk generation and indexing orchestration
- provide document context to the RAG pipeline

## Important Notes

The document model is designed to work with the RAG and chat modules. It tracks the fields needed for filtering, retrieval, user traceability, and multi-tenant isolation.

## Use Case

This is the document foundation for enterprise knowledge search and retrieval operations.
