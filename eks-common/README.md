# EKS Common Module

## Purpose

The `eks-common` module is the foundational shared library for the entire project. It contains reusable infrastructure, common entities, cross-cutting abstractions, and project-wide utility components.

This module is not the runnable app. It is designed to be imported by the other modules as a dependency.

## Stack Used

- Java 21
- Spring Boot 3.5.0
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Lombok

## Responsibilities

- Shared base entities
- Audit fields such as created/updated timestamps
- Common JPA mappings
- Security and application-level utility classes
- Shared configuration support
- Database migration resources

## Core Concepts

### BaseEntity

The `BaseEntity` class provides standard database fields that every persisted entity can inherit:

- `id`
- `createdAt`
- `updatedAt`
- `createdBy`
- `updatedBy`

This helps maintain consistency across all modules.

### Migration Support

The module contains the Flyway SQL files used to initialize and evolve the schema.

## How It Works

The other modules extend or use shared infrastructure from `eks-common`. This reduces duplication and keeps the project consistent across layers.

## Structure

```text
eks-common/
├── pom.xml
├── README.md
├── src/main/java/com/karthik/eks/common/
│   └── entity/
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/
```

## Typical Usage

Modules use this library for common:

- JPA entity inheritance
- cross-module identity-related abstract behavior
- common configuration values
- shared bootstrap metadata and application conventions

## Execution Notes

This module should not be run independently as the main application. The runtime entry point must be in `eks-app`.

## Use Case

This module is used to provide the shared foundation for:

- persistence models
- security integration
- migration scripts
- cross-cutting domain conventions
