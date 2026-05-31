---
type: learning-os
status: active
date: 2026-05-28
tags:
  - learning-os
  - curriculum
  - technology-map
---

# Curriculum Backbone

How Java 25, REST Assured, HTTP/REST, AssertJ, SQL/PostgreSQL and Test Design connect to each lesson.

## Technology → Lesson Matrix

| Technology | Lessons 1-5 (Foundations) | Lesson 06 (Active) | Sprint 6.5-7 (Next) | Sprint 8+ (Future) |
|---|---|---|---|---|
| **Java 25** | Records, enums, `Map.of`, `java.time` | Value objects (`PaymentAmount`, `CurrencyCode`, `IdempotencyKey`, `RequestFingerprint`), `record` DTOs, entity state machines | Test data builders | BigDecimal, streams, deprecated API hygiene |
| **REST Assured** | `given/when/then`, path/query params, headers, JSON body | Contract tests for create/read, header assertions, idempotency replay, extraction, error codes | Spec builders, reusable error specs | JSON schema validation, GPath, typed extraction |
| **HTTP/REST** | Methods, status codes, request/response | `201 Created`, `Location`, `ETag`, `X-Correlation-ID`, `Idempotency-Key`, `401`, `403`, masked `404`, `409` variants | `415`, `406`, content negotiation | `If-Match`/`412`, `Retry-After`/`429` |
| **AssertJ** | Basic assertions, `extracting` | Recursive comparison, extracting collections | Soft assertions, complex extraction | Custom assertions |
| **SQL/PostgreSQL** | FK, unique, check (conceptual) | `payment_orders`, `idempotency_records`, `payment_order_status_history`, check constraints, Flyway migration | DB vs API decision framework, CTE basics | JOIN, GROUP BY, EXPLAIN, RLS |
| **Test Design** | BVA/EP, decision tables, state transitions | Decision table (idempotency), security matrix, error contract | Negative-path first, assertion strategy | State transitions for lifecycle |

## Lesson Backbone (Lesson 6 Onward)

| # | Lesson | Type | Capability | Key Technologies |
|---|---|---|---|---|
| **06A** | **REST Assured Response Assertions Refresh** | **Concept** | Assert status, headers, body, error shape | REST Assured, HTTP |
| **06B** | **Payment Order Idempotency Case Study** | **Case Study** | Idempotent create, replay, conflict | RA, Java (RequestFingerprint), SQL (unique constraint) |
| **06C** | **HTTP Headers: Idempotency-Key, Location, ETag, X-Correlation-ID** | **Concept** | Protocol-level contract headers | HTTP, REST Assured |
| **06D** | **SQL/Flyway Constraints in Payment Orders** | **Code Reading** | payment_orders, idempotency_records, status_history | SQL, Flyway, PostgreSQL |
| **06E** | **Security Matrix: Role vs Ownership vs Tenant Isolation** | **Case Study** | 7 roles × create/read × merchant scope | Security, Keycloak, JWT |
| **06F** | **AssertJ and Test Oracle Strategy** | **Concept** | RA body vs AssertJ vs DB query | AssertJ, RA |
| **06G** | **API Failure Analysis and Debugging** | **Concept** | App bug vs test bug vs data bug vs env bug | Debugging, diagnostics |
| **06H** | **REST Assured Framework Architecture** | **Concept** | Specs, support classes, fixtures | RA, test architecture |
| **07** | **Payment List/Report** | **Vertical Slice** | Filtered reads with WHERE, ORDER BY, LIMIT | RA, SQL |
| **08** | **Payment Aggregation** | **Vertical Slice** | GROUP BY, indexes, EXPLAIN | SQL |
| **09** | **Auth Ownership** | **Deep Dive** | BOLA/BFLA, RLS concept | Security |
| 10 | Webhook Subscription | Vertical Slice | Async notifications | REST, WireMock |
| 11 | Webhook Delivery | Vertical Slice | Async state | REST, WireMock |
| 12 | Kafka Events | Infrastructure | Event pipeline | Kafka |
| 13 | GraphQL Dashboard | Infrastructure | Read model | GraphQL |
| 14 | gRPC Simulator | Infrastructure | Internal service | gRPC |

## Connection Rules

- **Lessons 1-5 = foundation only.** Reference, don't re-study.
- **Lesson 06A-06H = active sub-lessons.** Break Lesson 06 into focused sessions — one topic, one mode, one exercise.
- **Sprint 7-9 = next.** Payment list/report, aggregation, auth ownership.
- **Sprint 10+ = read but don't touch.** Knowing they exist is enough now.
