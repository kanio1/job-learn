---
type: moc
status: draft
area: JUnit REST Assured
date: 2026-05-21
tags:
  - moc
  - rest-assured
  - junit
  - backend-testing
---

# JUnit REST Assured - MOC

This area collects backend API testing learning material for Java/JUnit/REST Assured.

It is part of the broader project roadmap in `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`. This area focuses specifically on REST Assured and backend API testcraft; the project roadmap explains when to combine it with Java 25, Spring MVC, PostgreSQL, Nuxt, TypeScript and Playwright lessons.

## Start Here

1. `REST Assured from Zero to Professional Backend API Testing/README.md`
2. `REST Assured from Zero to Professional Backend API Testing/Context7 Research Notes.md`
3. `REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`
4. `REST Assured from Zero to Professional Backend API Testing/13-22 Professional Practice After Refactoring.md`
5. `../Backend Testing Review/Professional Backend API Testing Reviewer Checklist.md`

## Learning Levels

### Foundation

- What REST Assured is.
- `given / when / then`.
- HTTP methods, endpoint, content type and accept.
- Path params, query params, headers.
- Request body with JSON, `Map.of`, DTO and serialization.
- Response assertions, nested paths and lists.
- Extraction, deserialization and multi-step flows.
- Auth and negative API tests.

### Intermediate

- Choosing the right test level.
- Structuring readable REST Assured tests.
- Separating unit, HTTP and integration test responsibilities.

### Professional Practice

- Reusable `RequestSpecification`.
- Spec builders and response specs.
- Logging only on validation failure.
- Header blacklisting.
- Test data design.
- Architecture and testability review.
- SOLID from an SDET perspective.
- JDK 25 / Mockito / Maven tooling awareness.
- Deferred risk notes after green builds.

## Related Project Notes

- `../REST API From Zero/Merchant Request and Response Flow.md`
- `../Testing Architecture/Testing - Phase 0 Quality Baseline.md`
- `../Testing Architecture/Testing - Parallel Readiness Principles.md`
- `../Spring Modulith/Merchant Module Architecture.md`
- `../../Business Product and Testing Thinking/Phase 1 Test Design.md`
- `../../../01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`
