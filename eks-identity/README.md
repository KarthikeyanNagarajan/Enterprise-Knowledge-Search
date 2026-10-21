# EKS Identity Module

## Purpose

The `eks-identity` module handles authentication, authorization, user management, and tenant management for the application.

It is the security backbone of the project and provides user login, JWT token validation, and tenant-aware access control.

## Stack Used

- Java 21
- Spring Boot 3.5.0
- Spring Security
- Spring Data JPA
- JWT (JJWT)
- PostgreSQL
- Validation

## Main Responsibilities

- User registration and login
- Password hashing and validation
- JWT generation and parsing
- Role-based authorization
- Tenant entity management
- Security configuration
- `UserDetailsService` integration

## Key Components

### User

Stores user identity and access information.

### Tenant

Stores tenant or organization-level metadata used for multi-tenant separation.

### SecurityConfiguration

Defines public endpoints and protected routes.

### JwtAuthenticationFilter

Intercepts incoming requests, validates JWT tokens, and populates the Spring Security context.

### CustomUserDetailsService

Loads a user record and converts it to a Spring Security user principal.

## Authentication Flow

1. User calls `auth/login` or `auth/register`.
2. Credentials are validated.
3. A JWT token is generated and returned.
4. Protected API routes validate the JWT via filter.
5. Spring Security checks the user's roles and permissions.

## Structure

```text
eks-identity/
├── pom.xml
├── README.md
├── src/main/java/com/karthik/eks/identity/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── security/
│   └── service/
```

## Typical Usage

This module is used for:

- login and signup APIs
- authorization checks on protected routes
- tenant and user isolation in the application
- generating secure tokens for API clients

## Run Notes

This module is not intended to be started independently; it is used by the application bootstrap module and other modules that require identity-aware access control.

## Use Case

The identity layer is essential for protecting enterprise APIs and securing access to tenant-specific data.
