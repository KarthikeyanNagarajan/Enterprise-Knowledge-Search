# EKS Agent Module

## Purpose

The `eks-agent` module focuses on workflow automation and agent-based task orchestration. It is intended to support autonomous or semi-autonomous execution patterns for domain workflows and operational tasks.

## Stack Used

- Java 21
- Spring Boot 3.5.0
- Spring Data JPA
- Spring Web
- PostgreSQL
- Validation

## Responsibilities

- Agent definition and lifecycle management
- Workflow creation and execution tracking
- Step-by-step task orchestration
- Task status, retries, and timeouts
- Tenant-aware execution

## Core Entities

### Agent

Represents an operational agent with metadata such as:

- name
- type
- role
- configuration
- tenant id
- execution timeout

### Workflow

Represents a workflow execution instance and tracks:

- status
- execution status
- priority
- retries
- timing metadata
- user and tenant context

### WorkflowStep

Represents an individual step within the workflow with:

- step name
- type
- order
- status
- execution metadata
- condition and routing values

## Working Model

1. An agent is configured for a task or role.
2. A workflow is created for a higher-level operation.
3. Workflow steps are executed sequentially or conditionally.
4. Step results are tracked and retried if required.
5. Workflow outcomes feed back into the broader platform services.

## Structure

```text
eks-agent/
├── pom.xml
├── README.md
├── src/main/java/com/karthik/eks/agent/
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
```

## Typical Usage

This module is used for:

- automation workflows
- orchestration of long-running tasks
- multi-step operations tied to documents, retrieval, and search activities
- operational orchestration beyond simple CRUD flows

## Use Case

This is the platform extension layer for agent-driven business logic and workflow automation.
