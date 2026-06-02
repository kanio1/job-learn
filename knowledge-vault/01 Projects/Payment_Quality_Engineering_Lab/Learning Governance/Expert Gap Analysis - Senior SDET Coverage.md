---
type: analysis
status: active
project: Payment Quality Engineering Lab
area: Learning Governance
date: 2026-05-28
tags:
  - gap-analysis
  - senior-sdet
  - learning-plan
  - expert-advice
  - sql-database
  - test-data
  - roadmap
---

# Expert Gap Analysis: Senior SDET Competency Coverage

## Cel

Porównanie obecnego planu nauki Payment Quality Engineering Lab z rekomendacjami eksperta ds. Senior QA Automation / SDET, ze szczególnym uwzględnieniem pogranicza SQL/baza danych/test data/migracje/transakcje/izolacja.

## Executive Summary

Payment Quality Engineering Lab ma solidne fundamenty w REST API testing, security/authorization matrices, PostgreSQL constraints i idempotency design. Największa luka zidentyfikowana przez eksperta — **SQL/Database & Test Data Engineering jako osobny filar** — jest częściowo adresowana przez istniejące ścieżki vault (`PostgreSQL and SQL From Zero`, `Spring Data JPA and Flyway`) oraz testy Flyway/constraint/repository z Lekcji 6, ale vault brakuje dedykowanego pokrycia dla: decyzji kiedy weryfikować przez DB vs API, CTE/window functions, EXPLAIN-based performance awareness, oraz systematycznej strategii test data ownership/cleanup. Z 25 obszarów eksperta: 7 jest dobrze pokrytych, 10 częściowo, 8 nie jest pokrytych. Większość luk można adresować w ramach rozszerzeń Lekcji 6 lub odroczyć na przyszłe lekcje.

## Gap Analysis Table

| # | Expert Area | Status | Evidence | Gap | Priority | When |
|---|---|---|---|---|---|---|
| 1 | SQL fundamentals (SELECT, JOIN, GROUP BY, CTE, window, EXPLAIN) | PARTIAL | `PostgreSQL and SQL From Zero/README.md` lessons 1-16, Lesson 6 SQL exercises | CTEs, window functions, EXPLAIN absent from vault | Medium | Sprints 7-8 |
| 2 | Database verification as test layer | PARTIAL | Lesson 6 "Reguły według warstwy", reviewer checklist | No explicit decision framework: when DB vs API | **High** | Lesson 6 extension |
| 3 | Test data management (ownership, isolation, cleanup) | PARTIAL | `Parallel Readiness Principles`, per-test merchant creation, `uniqueIdempotencyKey()` | No documented lifecycle strategy, no cleanup patterns | **High** | Lesson 6 extension |
| 4 | Database migrations testing (Flyway) | COVERED | `V2__create_payment_orders.sql`, `JpaPaymentOrderRepositoryTest`, Flyway README | No gap | Low | — |
| 5 | Transactions, isolation, concurrency | PARTIAL | `PaymentOrderIdempotencyConcurrencyTest`, `@Transactional` | No deadlock testing, no isolation level testing | Medium | Future lifecycle |
| 6 | Contract testing & service virtualization (Pact, WireMock) | PARTIAL | `wiremock/README.md` placeholder | No Pact, no actual WireMock stubs | Low | Future contract/async sprint after Lesson 10 REST hardening |
| 7 | Observability-driven testing (logs, metrics, traces) | PARTIAL | `CorrelationIdFilter`, `X-Correlation-ID` tested | No log capture assertions, no metrics testing | Medium | Sprint 12+ |
| 8 | Flaky test & CI failure diagnosis | NOT COVERED | No vault notes, no code patterns | Complete gap | Medium | Future note |
| 9 | Performance-light API testing | NOT COVERED | No `.time()` assertions, no N+1 detection | Complete gap | Low | After lifecycle |
| 10 | Failure analysis & debugging skills | NOT COVERED | No structured debugging approach | Complete gap | Medium | Future note |
| 11 | Business-readable test naming | PARTIAL | Good technical names exist (`crossTenantReadReturns404`) | No `@DisplayName`, no formal naming convention | Low | Lesson 6 minor |
| 12 | Framework evolution (raw → specs → clients → flows) | PARTIAL | `MerchantApiTestSupport` returns `RequestSpecification` | No documented evolution path | Medium | Future refactoring |
| 13 | REST Assured GPath traps & JSON path | NOT COVERED | No GPath-specific notes | Complete gap | Low | When list endpoints exist |
| 14 | Money values & BigDecimal | PARTIAL | `PaymentAmount` uses `long` minor units | No BigDecimal contrast | Low | When decimal input appears |
| 15 | JSON Schema validation | NOT COVERED | No `.json` schema files, no `matchesJsonSchemaInClasspath()` | Complete gap | Medium | After API stabilizes |
| 16 | API test coverage beyond code coverage | NOT COVERED | No coverage matrix for endpoints × behaviors | Complete gap | Low | Future quality metrics |
| 17 | API documentation smells | NOT COVERED | Contract exists in markdown but no smells analysis | Complete gap | Low | Future reviewer checklist |
| 18 | Assertion strategy (RA vs AssertJ vs DB) | PARTIAL | RA uses Hamcrest, repo tests use AssertJ | No explicit decision framework | **High** | Lesson 6 extension |
| 19 | Negative-path first testing | PARTIAL | Comprehensive 400/401/403/404/409 tests exist | Not explicitly "negative first" methodology | Low | Lesson 6 exercise |
| 20 | Content negotiation (415/406) | PLANNED | Lesson 10 note/prompt | No implemented tests yet | Medium | Lesson 10 HTTP contract hardening |
| 21 | Idempotency & retry testing | COVERED | Concurrency test, replay/conflict tests, Lesson 6 deep dive | Well-covered for create | Low | — |
| 22 | Observability assertions | NOT COVERED | No log/metric/trace assertions | Complete gap | Medium | Sprint 12+ |
| 23 | API consumer compatibility | NOT COVERED | Frontend exists but no backward-compat testing | Complete gap | Low | Future API evolution |
| 24 | LLM-assisted test design | NOT COVERED | No documented process | Complete gap | Low | Future process note |
| 25 | Framework teardown (OSS review) | NOT COVERED | No repo review exercises | Complete gap | Low | Future exercise |
| 26 | Browser traffic capture as reverse engineering | NOT COVERED | No vault notes or exercises | Complete gap — ekspert wprost wymienił jako przydatne narzędzie nauki API | Low | Future note or exercise |
| 27 | SQL injection and data security basics | NOT COVERED | Security matrix covers auth, not injection | Complete gap — ekspert wymienił jako PostgreSQL-specific | Medium | Database security note in later sprint |
| 28 | RLS and multi-tenant data isolation | NOT COVERED | Tenant isolation via claim, not row-level security | Complete gap — ekspert wymienił jako PostgreSQL-specific | Medium | Sprint 9 extension with RLS concept |

## Lesson 6 Extension Opportunities (NOW)

### 1. Assertion Strategy Decision Framework (#18)
Dodać sekcję do Lesson 6 wyjaśniającą kiedy użyć:
- REST Assured body assertions (HTTP contract)
- AssertJ extraction (complex field comparisons)
- Direct DB query assertions (data integrity, constraints)

### 2. Database Verification Decision Framework (#2)
Dodać decision table:
- Verify via API: HTTP contract, status codes, headers, response body
- Verify via DB: constraint enforcement, audit records, data integrity
- Verify via both: critical business flows where API success ≠ DB correctness

### 3. Test Data Ownership Note (#3)
Udokumentować obecną strategię:
- Per-test merchant creation (isolation)
- Namespaced references
- Idempotency key reuse only within idempotency tests
- No cleanup needed for immutable records

### 4. Business-Readable Test Naming Exercise (#11)
Dodać `@DisplayName` do 3-5 kluczowych testów.

### 5. Negative-Path First Exercise (#19)
Ćwiczenie: "Napisz test 409 idempotency_conflict PRZED testem 201 success."

## Future Lesson Recommendations

| Lesson | Topic | Expert Areas | Sprint |
|---|---|---|---|
| 6.5 | REST Assured Assertion Strategy Deep Dive | #18, #11, #12 | After Lesson 6 |
| 7b | SQL for Testers: CTEs, Window Functions, EXPLAIN | #1 | Sprints 7-8 |
| 7c | Database Verification as Test Layer | #2, #5 | Sprint 7 |
| 8b | Test Data Management Strategy | #3 | Sprint 8 |
| 10b | JSON Schema Validation and Contract Testing | #15, #17 | After API stabilizes |
| 12b | Observability Testing | #7, #22 | Sprint 12+ |
| 12c | Flaky Test Diagnosis and CI Failure Triage | #8, #10 | Sprint 12+ |
| 14b | Performance-Light API Testing | #9 | After lifecycle |
| 15b | Content Negotiation and API Robustness | #20 | Sprint 15+ |
| 16b | API Test Coverage Matrix | #16 | Sprint 16+ |
| 17b | REST Assured GPath Deep Dive | #13 | When list endpoints exist |
| 18b | Money Values: Minor Units vs BigDecimal | #14 | When decimal input appears |
| 19b | Framework Evolution: Specs → Clients → Flows | #12 | After RA refactoring |
| 20b | LLM-Assisted Test Design Process | #24 | Anytime |
| 21b | Framework Teardown: Learning from OSS | #25 | Anytime |
| 22b | Browser Traffic Capture as API Reverse Engineering | #26 | Anytime |
| 23b | SQL Injection and Database Security Basics | #27 | After security matrix is solid |
| 24b | RLS and Multi-Tenant Data Isolation in PostgreSQL | #28 | Sprint 9+ |

## Correctly Deferred

| Area | Why |
|---|---|
| #6 Contract testing & Pact | No downstream consumers yet |
| #23 API consumer compatibility | No versioning strategy needed yet |
| #24 LLM-assisted test design | Process note, not blocking |
| #25 Framework teardown | Learning exercise, not blocking |

## Interview Answer EN

> As a backend SDET, I do not stop at checking HTTP status codes. I connect API behavior with database state, transaction boundaries, test data isolation, migration safety, security rules and observability. I use REST Assured for API-level checks, AssertJ for object and database assertions, Testcontainers for realistic dependencies, and SQL to diagnose whether the system actually changed state correctly.

## Powiązane Notatki

- [[Payment Gateway SDET Learning Plan]]
- [[Lesson Evidence Tracker]]
- [[Senior SDET Competency Coverage Matrix]]
- [[Phase 2 - Payment Orders]]
- [[Lesson 06 - Payment Order Create Read Foundation]]
