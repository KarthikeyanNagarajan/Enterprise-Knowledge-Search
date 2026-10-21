# EKS App Module

## Purpose

The `eks-app` module is the runtime bootstrap layer for the entire application. It is the only module responsible for starting the Spring Boot application and composing the rest of the platform as dependencies.

This module keeps the project clean by ensuring the shared library modules do not also act as runnable application entry points.

## Stack Used

- Java 21
- Spring Boot 3.5.0
- Maven multi-module project
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Actuator

## Responsibilities

- Starts the application context
- Wires together the domain modules
- Loads configuration
- Connects to PostgreSQL
- Runs Flyway migrations
- Exposes the application endpoints

## Dependencies

This module depends on:

- `eks-common`
- `eks-identity`
- `eks-document`
- `eks-rag`
- `eks-chat`
- `eks-agent`

## How It Works

`eks-app` acts as the composition root. It brings together all of the feature modules and starts the application as a single executable Spring Boot service.

## Run Locally

From the project root:

```bash
mvn clean install -DskipTests
cd eks-app
mvn spring-boot:run
```

Application URL:

```text
http://localhost:8080/api
```

Health check:

```text
http://localhost:8080/api/actuator/health
```

## Structure

```text
eks-app/
├── pom.xml
├── README.md
└── src/main/java/com/karthik/eks/
    └── EksApplication.java
```

## Why This Module Exists

The project was structured to avoid a common anti-pattern: placing the application runner in a shared library. By keeping the bootstrap in `eks-app`, the codebase remains cleanly separated by responsibility and avoids circular dependency problems.

## Use Case

This module is used for:

- local startup
- runtime integration tests
- deployment packaging
- application bootstrapping
