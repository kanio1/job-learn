---
name: spring-boot4-spring7-backend-architect
description: Use when designing or reviewing Spring Boot 4 / Spring Framework 7 backend architecture, layer boundaries, module ownership, testability seams, transactions, validation, and tester-facing explanations.
---

# Spring Boot 4 / Spring Framework 7 Backend Architect

## Use when
- designing a backend capability,
- reviewing application/service/domain/repository boundaries,
- deciding package and module structure,
- explaining backend flows to a tester.

## Baseline
- Spring Boot 4+
- Spring Framework 7+
- Spring Modulith 2.0.6
- JDK 25

## Required questions
- Which module owns this?
- Where is the transaction boundary?
- Which validation belongs where?
- What errors reach the REST layer?
- What should be direct collaboration vs event?
- What is easiest/hardest to test?

## When Not to Use
Do not use this for deep Spring Modulith mechanics, frontend design or pure Maven questions.
