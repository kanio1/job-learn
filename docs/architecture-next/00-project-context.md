# Project Context — Payment Quality Engineering Lab Next Architecture

Date: 2026-09-02

## Purpose

This document separates the repository's implemented state from the proposed Go/distributed-systems learning direction.

## Current state

The repository is a learning-oriented payment quality engineering platform. The current backend is a Java 25 / Spring Boot 4 modular monolith using Spring Modulith. The frontend is Nuxt 4 + TypeScript through a Nitro BFF. PostgreSQL 18 and Keycloak 26.6.1 run locally. The codebase already contains merchant and payment-order domains, lifecycle operations, idempotency, conditional HTTP semantics, audit, IAM, tenant settings, learning labs, REST Assured, Testcontainers, Playwright and a separate black-box API test application.

Kafka is not a product dependency today. The repository contains an Event Streaming learning overlay, but the product remains synchronous/modular-monolith-first. Microservice decomposition is intentionally not part of the current product architecture.

## Planned direction

Evolve the lab rather than rewrite it. Keep the Java/Spring system as the reference product/control plane and introduce a Go-based distributed payment execution plane gradually. The new plane exists to teach concurrency, messaging, failure handling, protobuf/gRPC, Kubernetes, observability and distributed data consistency.

## Architectural thesis

1. Preserve the working Spring modular monolith.
2. Introduce Go only at a real process boundary.
3. Introduce Kafka only when asynchronous delivery creates a real reliability problem.
4. Introduce gRPC only for synchronous service-to-service contracts that benefit from deadlines and protobuf schemas.
5. Keep PostgreSQL authoritative for business state.
6. Use outbox/inbox and idempotent state transitions rather than pretending Kafka alone provides end-to-end exactly-once guarantees.
7. Move to Kubernetes only after multiple independently deployable processes exist.

## State matrix

| Area | Current | Planned | Problem | Recommendation |
|---|---|---|---|---|
| Product backend | Java/Spring modular monolith | Java control plane + Go execution services | Rewriting loses existing learning value | Evolve incrementally |
| Payments | Merchant-scoped payment orders | Add asynchronous execution | Current lifecycle is mostly in-process | Extract execution only after event contract stabilizes |
| PostgreSQL | PostgreSQL 18 | PostgreSQL 19 target | Version upgrade can distract from architecture | Upgrade independently from service extraction |
| IAM | Keycloak/JWT | Same identity authority | Distributed services need consistent claims | Standardize token/tenant contract |
| Kafka | Learning overlay only | Product async backbone for selected flows | Kafka can become accidental RPC | Limit to domain/integration events |
| REST | Mature product API | Remains external API | None | Keep REST at product edge |
| Protobuf/gRPC | Planned | Selected internal APIs | Risk of technology-for-technology's-sake | Use for simulator/execution service boundary |
| Kubernetes | Not core | Final deployment lab | Premature operational complexity | Compose/processes first |
| Testing | Strong Java/TS pyramid | Add Go, event, resilience, contract tests | Cross-process failures not represented yet | Add test layers with each new boundary |

## Explicit non-goal

This is not a production PSP, bank, card processor or regulatory compliance implementation. All external financial networks and SCA mechanisms are controlled simulations.