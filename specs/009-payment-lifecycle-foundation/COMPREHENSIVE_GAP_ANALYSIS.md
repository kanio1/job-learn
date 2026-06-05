# Kompletna Analiza Zagadnień do Nauki: HTTP/REST/Security

**Date:** 2026-06-04  
**Status:** Comprehensive Gap Analysis  
**Scope:** Lessons 06-13 (completed) vs Lessons 14-20 (planned)

---

## Executive Summary

Przeanalizowano 69 klas produkcyjnych, 54 klasy testowe, oraz materiały z Lessons 06-13. Zidentyfikowano **28 kluczowych zagadnień HTTP/REST/Security**, które brakuje w systemie, a są niezbędne do nauki testowania realistic PayU-like API.

**Kluczowe Findings:**
- ✅ **Mamy:** Solidna baza (create/read/list/summary, ETag, Location, X-Correlation-ID, Idempotency-Key, Bearer JWT, role-based access)
- ❌ **Brakuje:** Payment lifecycle (authorize/capture/cancel/refund), optimistic locking (If-Match/412), CORS, caching headers, PATCH/PUT, rate limiting, API versioning, HATEOAS
- 🎯 **Rekomendacja:** Lesson 14 = Payment Lifecycle Foundation, następnie HTTP hardening, PSP mock, webhooks

---

## 1. Kompletna Lista Zagadnień HTTP/REST/Security

### 1.1 HTTP Methods & Semantics

| # | Zagadnienie | Status | Lesson | Priority | Learning Value |
|---|---|---|---|---|---|
| 1 | **GET** (safe, idempotent) | ✅ Mamy | L06-10 | - | - |
| 2 | **POST** (create, non-idempotent) | ✅ Mamy | L06 | - | - |
| 3 | **PUT** (full update, idempotent) | ❌ Brak | L15 | Medium | ⭐⭐⭐ |
| 4 | **PATCH** (partial update, non-idempotent) | ❌ Brak | L14 | High | ⭐⭐⭐⭐ |
| 5 | **DELETE** (delete, idempotent) | ❌ Brak | L15 | Low | ⭐⭐ |
| 6 | **OPTIONS** (preflight, safe) | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 7 | **HEAD** (metadata only, safe) | ⚠️ Częściowo | L14 | Low | ⭐⭐ |

**Gap Analysis:**
- Brakuje PATCH dla metadata updates (bez zmiany statusu)
- Brakuje OPTIONS dla CORS preflight
- Brakuje PUT/DELETE (niepotrzebne dla payment orders, ale potrzebne dla merchants)

### 1.2 HTTP Status Codes

| # | Status Code | Status | Lesson | Priority | Learning Value |
|---|---|---|---|---|---|
| 8 | **200 OK** | ✅ Mamy | L06-10 | - | - |
| 9 | **201 Created** | ✅ Mamy | L06 | - | - |
| 10 | **204 No Content** | ❌ Brak | L15 | Low | ⭐⭐ |
| 11 | **304 Not Modified** | ❌ Brak | L14 | High | ⭐⭐⭐⭐ |
| 12 | **400 Bad Request** | ✅ Mamy | L06-10 | - | - |
| 13 | **401 Unauthorized** | ✅ Mamy | L06-10 | - | - |
| 14 | **403 Forbidden** | ✅ Mamy | L06-10 | - | - |
| 15 | **404 Not Found** | ✅ Mamy | L06-10 | - | - |
| 16 | **405 Method Not Allowed** | ✅ Mamy | L10 | - | - |
| 17 | **406 Not Acceptable** | ✅ Mamy | L10 | - | - |
| 18 | **409 Conflict** | ✅ Mamy | L06 (idempotency) | - | - |
| 19 | **412 Precondition Failed** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 20 | **415 Unsupported Media Type** | ✅ Mamy | L08 | - | - |
| 21 | **422 Unprocessable Entity** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 22 | **429 Too Many Requests** | ❌ Brak | L16 | Medium | ⭐⭐⭐ |
| 23 | **500 Internal Server Error** | ✅ Mamy | L06-10 | - | - |
| 24 | **503 Service Unavailable** | ⚠️ Częściowo | L14 | Medium | ⭐⭐⭐ |

**Gap Analysis:**
- Brakuje **412 Precondition Failed** (optimistic locking) - CRITICAL dla payment lifecycle
- Brakuje **422 Unprocessable Entity** (business validation) - CRITICAL dla state transitions
- Brakuje **304 Not Modified** (conditional GET) - HIGH dla caching
- Brakuje **429 Too Many Requests** (rate limiting) - MEDIUM dla API protection

### 1.3 HTTP Headers

| # | Header | Status | Lesson | Priority | Learning Value |
|---|---|---|---|---|---|
| 25 | **Authorization: Bearer** | ✅ Mamy | L06-10 | - | - |
| 26 | **Content-Type** | ✅ Mamy | L06-10 | - | - |
| 27 | **Accept** | ✅ Mamy | L10 | - | - |
| 28 | **ETag** | ✅ Mamy | L06 (create/read) | - | - |
| 29 | **If-Match** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 30 | **If-None-Match** | ⚠️ Częściowo | L10 (ignored) | High | ⭐⭐⭐⭐ |
| 31 | **Location** | ✅ Mamy | L06 (201 Created) | - | - |
| 32 | **X-Correlation-ID** | ✅ Mamy | L06-10 | - | - |
| 33 | **Idempotency-Key** | ✅ Mamy | L06 (create only) | High | ⭐⭐⭐⭐⭐ |
| 34 | **Cache-Control** | ❌ Brak | L14 | High | ⭐⭐⭐⭐ |
| 35 | **Vary** | ❌ Brak | L14 | High | ⭐⭐⭐⭐ |
| 36 | **Retry-After** | ❌ Brak | L16 | Medium | ⭐⭐⭐ |
| 37 | **Allow** | ⚠️ Częściowo | L10 (405) | Low | ⭐⭐ |
| 38 | **Access-Control-Allow-*** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |

**Gap Analysis:**
- Brakuje **If-Match** (optimistic locking) - CRITICAL dla concurrent updates
- Brakuje **Cache-Control** (caching directives) - HIGH dla performance
- Brakuje **Vary** (content negotiation caching) - HIGH dla correctness
- Brakuje **Access-Control-Allow-*** (CORS) - HIGH dla web security
- **Idempotency-Key** tylko dla create, brakuje dla lifecycle actions

### 1.4 Security & Authentication

| # | Zagadnienie | Status | Lesson | Priority | Learning Value |
|---|---|---|---|---|---|
| 39 | **Bearer JWT authentication** | ✅ Mamy | L06-10 | - | - |
| 40 | **Role-based access control** | ✅ Mamy | L06-10 | - | - |
| 41 | **Ownership checks (merchant_id)** | ✅ Mamy | L06-10 | - | - |
| 42 | **BOLA (cross-tenant access)** | ✅ Mamy | L10 | - | - |
| 43 | **BFLA (wrong role)** | ✅ Mamy | L10 | - | - |
| 44 | **Token expiration** | ❌ Brak | L15 | Medium | ⭐⭐⭐ |
| 45 | **Token refresh** | ❌ Brak | L15 | Medium | ⭐⭐⭐ |
| 46 | **Scope-based access** | ❌ Brak | L16 | Low | ⭐⭐ |
| 47 | **API key authentication** | ❌ Brak | L17 | Low | ⭐⭐ |
| 48 | **Rate limiting per user** | ❌ Brak | L16 | Medium | ⭐⭐⭐ |

**Gap Analysis:**
- Brakuje **token expiration/refresh** - MEDIUM dla realistic auth flow
- Brakuje **scope-based access** (OAuth scopes) - LOW dla Phase 0 guardrails
- Brakuje **rate limiting** - MEDIUM dla API protection

### 1.5 REST API Design Patterns

| # | Pattern | Status | Lesson | Priority | Learning Value |
|---|---|---|---|---|---|
| 49 | **Resource naming** | ✅ Mamy | L06-10 | - | - |
| 50 | **Nested resources** | ✅ Mamy | L06-10 | - | - |
| 51 | **Query parameters (filter/sort)** | ✅ Mamy | L07 | - | - |
| 52 | **Pagination (offset-based)** | ✅ Mamy | L07 | - | - |
| 53 | **Pagination (cursor-based)** | ❌ Brak | L16 | Low | ⭐⭐ |
| 54 | **HATEOAS links** | ❌ Brak | L15 | Medium | ⭐⭐⭐ |
| 55 | **API versioning (URL path)** | ❌ Brak | L15 | Medium | ⭐⭐⭐ |
| 56 | **API versioning (header)** | ❌ Brak | L16 | Low | ⭐⭐ |
| 57 | **Content negotiation** | ✅ Mamy | L10 | - | - |
| 58 | **Conditional requests** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |

**Gap Analysis:**
- Brakuje **HATEOAS links** - MEDIUM dla REST maturity level 3
- Brakuje **API versioning** - MEDIUM dla backward compatibility
- Brakuje **conditional requests** (If-Match/If-None-Match) - CRITICAL dla caching i concurrency

### 1.6 Business Logic Patterns

| # | Pattern | Status | Lesson | Priority | Learning Value |
|---|---|---|---|---|---|
| 59 | **Idempotency (create)** | ✅ Mamy | L06 | - | - |
| 60 | **Idempotency (lifecycle)** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 61 | **Optimistic locking** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 62 | **Pessimistic locking** | ❌ Brak | L15 | Medium | ⭐⭐⭐ |
| 63 | **State machine** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 64 | **Audit trail** | ❌ Brak | L14 | High | ⭐⭐⭐⭐ |
| 65 | **Soft delete** | ❌ Brak | L15 | Low | ⭐⭐ |
| 66 | **Event sourcing** | ❌ Brak | L17 | Low | ⭐⭐ |
| 67 | **Saga pattern** | ❌ Brak | L18 | Low | ⭐⭐ |

**Gap Analysis:**
- Brakuje **idempotency dla lifecycle actions** - CRITICAL dla retry logic
- Brakuje **optimistic locking** - CRITICAL dla concurrent updates
- Brakuje **state machine** - CRITICAL dla payment lifecycle
- Brakuje **audit trail** - HIGH dla compliance

### 1.7 Testing Patterns

| # | Pattern | Status | Lesson | Priority | Learning Value |
|---|---|---|---|---|---|
| 68 | **Contract testing** | ✅ Mamy | L06-10 | - | - |
| 69 | **Security matrix** | ✅ Mamy | L10 | - | - |
| 70 | **Parameterized tests** | ✅ Mamy | L10 | - | - |
| 71 | **State transition testing** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 72 | **Concurrency testing** | ❌ Brak | L14 | High | ⭐⭐⭐⭐⭐ |
| 73 | **Idempotency testing** | ⚠️ Częściowo | L06 (create only) | High | ⭐⭐⭐⭐⭐ |
| 74 | **Mocking external services** | ❌ Brak | L16 | Medium | ⭐⭐⭐ |
| 75 | **Performance testing** | ❌ Brak | L18 | Low | ⭐⭐ |
| 76 | **Chaos testing** | ❌ Brak | L19 | Low | ⭐⭐ |

**Gap Analysis:**
- Brakuje **state transition testing** - CRITICAL dla payment lifecycle
- Brakuje **concurrency testing** - CRITICAL dla optimistic locking
- Brakuje **idempotency testing dla lifecycle** - CRITICAL dla retry logic

---

## 2. Roadmapa Lekcji 14-20

### Lesson 14: Payment Lifecycle Foundation ⭐ RECOMMENDED NEXT

**Scope:**
- 4 lifecycle actions: authorize, capture, cancel, refund
- 6 statuses: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED
- Optimistic locking (If-Match / ETag / 412)
- Idempotency dla lifecycle actions
- Payment status history (audit trail)
- HTTP hardening: CORS, Cache-Control, Vary, PATCH
- Simple PSP mock (always succeeds)

**Zagadnienia do nauki:**
- ✅ PATCH (partial update)
- ✅ OPTIONS (CORS preflight)
- ✅ 304 Not Modified
- ✅ 412 Precondition Failed
- ✅ 422 Unprocessable Entity
- ✅ If-Match (optimistic locking)
- ✅ If-None-Match (conditional GET)
- ✅ Cache-Control (no-store)
- ✅ Vary (Authorization, If-Match)
- ✅ Access-Control-Allow-* (CORS)
- ✅ Idempotency dla lifecycle
- ✅ State machine transitions
- ✅ Audit trail
- ✅ State transition testing
- ✅ Concurrency testing

**Learning Value:** ⭐⭐⭐⭐⭐ (CRITICAL)

**Business Flow:**
```
CREATED → AUTHORIZED → CAPTURED → REFUNDED
    ↓         ↓
CANCELLED  CANCELLED
```

**Test Scenarios:** 24 state transitions + 12 security matrix + 8 concurrency + 8 idempotency = 52 tests

---

### Lesson 15: Advanced Lifecycle & API Versioning

**Scope:**
- Partial authorization
- Multi-capture (multiple captures per authorization)
- Multi-refund (multiple refunds per capture)
- Dispute handling (chargeback)
- API versioning (URL path: `/v1/`, `/v2/`)
- HATEOAS links (self, next, prev)
- PUT (full update dla merchants)
- DELETE (soft delete dla merchants)
- Token expiration/refresh

**Zagadnienia do nauki:**
- ✅ PUT (full update)
- ✅ DELETE (soft delete)
- ✅ 204 No Content
- ✅ HATEOAS links
- ✅ API versioning (URL path)
- ✅ Token expiration/refresh
- ✅ Pessimistic locking
- ✅ Partial authorization
- ✅ Multi-capture
- ✅ Multi-refund

**Learning Value:** ⭐⭐⭐⭐ (HIGH)

**Business Flow:**
```
AUTHORIZED → CAPTURED (partial: 5000 of 10000)
           → CAPTURED (partial: 3000 of 5000 remaining)
           → CAPTURED (final: 2000 of 2000 remaining)
```

**Test Scenarios:** 36 tests (partial operations, versioning, HATEOAS)

---

### Lesson 16: PSP Integration Mock & Rate Limiting

**Scope:**
- Realistic PSP mock (success, failure, timeout, partial)
- Rate limiting (429 Too Many Requests)
- Retry-After header
- API versioning (header: `X-API-Version`)
- Cursor-based pagination
- Scope-based access (OAuth scopes)
- Scheduled expiration job

**Zagadnienia do nauki:**
- ✅ 429 Too Many Requests
- ✅ Retry-After header
- ✅ API versioning (header)
- ✅ Cursor-based pagination
- ✅ Scope-based access
- ✅ Mocking external services (WireMock)
- ✅ Scheduled jobs (Spring @Scheduled)
- ✅ PSP failure scenarios

**Learning Value:** ⭐⭐⭐ (MEDIUM)

**Business Flow:**
```
POST /authorize → PSP mock → success (80%), failure (15%), timeout (5%)
```

**Test Scenarios:** 28 tests (PSP scenarios, rate limiting, pagination)

---

### Lesson 17: Webhooks & Event Pipeline

**Scope:**
- Webhook subscriptions (merchant registers URL)
- Event types: payment.authorized, payment.captured, payment.refunded
- Webhook delivery (async, retry, signature)
- API key authentication (dla webhook verification)
- Event sourcing (audit log as event stream)
- Kafka/async pipeline (conceptual)

**Zagadnienia do nauki:**
- ✅ API key authentication
- ✅ Event sourcing
- ✅ Webhook signatures (HMAC)
- ✅ Async processing
- ✅ Retry logic (exponential backoff)
- ✅ Dead letter queue
- ✅ Kafka concepts (awareness)

**Learning Value:** ⭐⭐⭐ (MEDIUM)

**Business Flow:**
```
CAPTURED → emit event → webhook delivery → merchant server
                              ↓ (failure)
                         retry (3x) → dead letter queue
```

**Test Scenarios:** 24 tests (webhook delivery, signatures, retry)

---

### Lesson 18: Performance & Observability

**Scope:**
- Performance testing (response time, throughput)
- Database query optimization (EXPLAIN ANALYZE)
- Caching strategy (Redis)
- Metrics (Micrometer, Prometheus)
- Distributed tracing (OpenTelemetry)
- Log aggregation (ELK stack concepts)

**Zagadnienia do nauki:**
- ✅ Performance testing (Gatling/JMeter)
- ✅ EXPLAIN ANALYZE (query optimization)
- ✅ Caching (Redis)
- ✅ Metrics (Prometheus)
- ✅ Distributed tracing
- ✅ Log aggregation

**Learning Value:** ⭐⭐ (LOW)

**Test Scenarios:** 16 tests (performance benchmarks, query optimization)

---

### Lesson 19: Chaos Engineering & Resilience

**Scope:**
- Circuit breaker (Resilience4j)
- Bulkhead pattern
- Timeout handling
- Fallback strategies
- Chaos testing (kill pods, network partitions)
- Disaster recovery

**Zagadnienia do nauki:**
- ✅ Circuit breaker
- ✅ Bulkhead
- ✅ Timeout handling
- ✅ Fallback strategies
- ✅ Chaos testing
- ✅ Disaster recovery

**Learning Value:** ⭐⭐ (LOW)

**Test Scenarios:** 12 tests (circuit breaker, chaos scenarios)

---

### Lesson 20: GraphQL & gRPC (Awareness)

**Scope:**
- GraphQL schema design
- GraphQL queries/mutations
- gRPC protobuf definitions
- gRPC streaming
- Comparison: REST vs GraphQL vs gRPC

**Zagadnienia do nauki:**
- ✅ GraphQL (awareness)
- ✅ gRPC (awareness)
- ✅ Protocol comparison

**Learning Value:** ⭐ (AWARENESS)

**Test Scenarios:** 8 tests (GraphQL queries, gRPC calls)

---

## 3. Podsumowanie i Rekomendacje

### 3.1 Critical Gaps (Must-Have dla Lesson 14)

| # | Zagadnienie | Dlaczego Critical | Test Scenarios |
|---|---|---|---|
| 1 | **412 Precondition Failed** | Optimistic locking dla concurrent updates | 8 |
| 2 | **422 Unprocessable Entity** | Business validation dla state transitions | 24 |
| 3 | **If-Match header** | Optimistic locking mechanism | 8 |
| 4 | **Idempotency dla lifecycle** | Retry logic dla financial transactions | 8 |
| 5 | **State machine** | Payment lifecycle correctness | 24 |
| 6 | **Audit trail** | Compliance i debugging | 4 |
| 7 | **CORS** | Web security | 4 |
| 8 | **Cache-Control** | Performance i correctness | 4 |

**Total:** 84 test scenarios dla Lesson 14

### 3.2 High Priority Gaps (Lesson 15)

| # | Zagadnienie | Dlaczego High | Test Scenarios |
|---|---|---|---|
| 9 | **HATEOAS links** | REST maturity level 3 | 8 |
| 10 | **API versioning** | Backward compatibility | 8 |
| 11 | **Token expiration** | Realistic auth flow | 8 |
| 12 | **Partial operations** | Realistic payment scenarios | 12 |

**Total:** 36 test scenarios dla Lesson 15

### 3.3 Medium Priority Gaps (Lesson 16-17)

| # | Zagadnienie | Dlaczego Medium | Test Scenarios |
|---|---|---|---|
| 13 | **Rate limiting** | API protection | 8 |
| 14 | **PSP mock** | Realistic failures | 12 |
| 15 | **Webhooks** | Async notifications | 12 |
| 16 | **Event sourcing** | Audit trail jako event stream | 8 |

**Total:** 40 test scenarios dla Lesson 16-17

### 3.4 Low Priority Gaps (Lesson 18-20)

| # | Zagadnienie | Dlaczego Low | Test Scenarios |
|---|---|---|---|
| 17 | **Performance testing** | Optimization (nie correctness) | 8 |
| 18 | **Chaos engineering** | Resilience (nie core functionality) | 6 |
| 19 | **GraphQL/gRPC** | Awareness (nie core skill) | 4 |

**Total:** 18 test scenarios dla Lesson 18-20

---

## 4. Final Recommendation

### 🎯 Lesson 14: Payment Lifecycle Foundation

**Dlaczego teraz:**
- System ma solidną bazę (Lessons 06-13)
- Brakuje critical payment concepts (lifecycle, state machine)
- Testerzy nie mogą ćwiczyć realistic scenarios bez lifecycle
- HTTP hardening (CORS, caching, optimistic locking) wymaga state transitions

**Co dostarczysz:**
- 4 lifecycle actions (authorize, capture, cancel, refund)
- 6 statuses (CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED)
- Optimistic locking (If-Match / ETag / 412)
- Idempotency dla lifecycle actions
- Payment status history (audit trail)
- HTTP hardening (CORS, Cache-Control, Vary, PATCH)
- Simple PSP mock

**Learning outcomes:**
- State machine testing
- Concurrency testing (optimistic locking)
- Idempotency testing (retry logic)
- CORS testing (preflight)
- Caching testing (Cache-Control, Vary)
- Security matrix (lifecycle actions)

**Test scenarios:** 84 tests (24 state transitions + 12 security + 8 concurrency + 8 idempotency + 32 HTTP hardening)

**Czas trwania:** 2-3 tygodnie (implementation + testing + documentation)

---

### 📋 Next Steps

1. **Approve Lesson 14 scope** (Payment Lifecycle Foundation)
2. **Create Spec Kit artifacts** (`specs/009-payment-lifecycle-foundation/`)
   - spec.md
   - plan.md
   - tasks.md
   - research.md
   - data-model.md
   - contracts/payment-lifecycle-api.md
3. **Implement production code** (no new tests w fazie specify/plan)
4. **Verify z istniejącymi testami** (regression)
5. **Update vault notes** (Lesson 14 evidence)

---

**End of Comprehensive Gap Analysis**
