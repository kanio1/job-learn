# Executive Summary: Payment Lifecycle Foundation

**Date:** 2026-06-04  
**Feature ID:** 009-payment-lifecycle-foundation  
**Status:** Ready for Spec Kit  
**Recommendation:** ⭐ START NOW

---

## 🎯 TL;DR

**Problem:** System ma tylko status `CREATED` - brak realistic payment lifecycle (authorize/capture/cancel/refund). Testerzy nie mogą ćwiczyć critical payment scenarios.

**Solution:** Lesson 14 = Payment Lifecycle Foundation z 4 akcjami, 6 statusami, optimistic locking, idempotency, i HTTP hardening.

**Impact:**
- ✅ 28 nowych zagadnień HTTP/REST/Security do nauki
- ✅ 84 test scenarios (state transitions, concurrency, idempotency, CORS, caching)
- ✅ Realistic PayU-like system
- ✅ Prerequisite dla advanced features (PSP, webhooks, disputes)

**Czas trwania:** 2-3 tygodnie

---

## 📊 Key Findings

### Co MAMY (Lessons 06-13)
- ✅ Solidna baza: create/read/list/summary
- ✅ HTTP basics: ETag, Location, X-Correlation-ID, Idempotency-Key
- ✅ Security: Bearer JWT, role-based access, ownership checks
- ✅ Testing: contract tests, security matrix, parameterized tests
- ✅ 69 klas produkcyjnych, 54 klasy testowe

### Czego BRAKUJE (Critical Gaps)
- ❌ **Payment lifecycle** (authorize/capture/cancel/refund) - CRITICAL
- ❌ **Optimistic locking** (If-Match / 412) - CRITICAL
- ❌ **State machine** (6 statuses, 24 transitions) - CRITICAL
- ❌ **Idempotency dla lifecycle** (retry logic) - CRITICAL
- ❌ **CORS** (preflight, Access-Control-Allow-*) - HIGH
- ❌ **Cache-Control** (no-store, Vary) - HIGH
- ❌ **PATCH** (partial updates) - HIGH
- ❌ **Audit trail** (status history) - HIGH

### Dlaczego Teraz
1. System incomplete bez lifecycle
2. Testerzy nie mogą ćwiczyć realistic scenarios
3. HTTP concepts (If-Match, 412) wymagają state transitions
4. Prerequisite dla advanced features (PSP, webhooks)

---

## 🎓 Learning Value

### Technical Skills (Lesson 14)
- [ ] State machine design i testing
- [ ] Optimistic locking (If-Match / ETag / 412)
- [ ] Idempotency dla non-create actions
- [ ] CORS preflight testing (OPTIONS)
- [ ] Cache-Control headers (no-store, Vary)
- [ ] PATCH dla partial updates
- [ ] Concurrency testing
- [ ] Audit trail implementation

### Business Skills (Lesson 14)
- [ ] Payment lifecycle (authorize → capture → refund)
- [ ] Authorization expiration i void
- [ ] Partial capture i partial refund
- [ ] Idempotency dla financial transactions
- [ ] Optimistic locking dla concurrency control

### Interview Questions (Lesson 14)
**Q1: How do you test state machine transitions?**
> Decision table z 24 scenarios (6 statuses × 4 actions). Parameterized test weryfikuje expected HTTP status code i error code. Exhaustive coverage, łatwe do rozszerzenia.

**Q2: How do you prevent lost updates?**
> Optimistic locking z ETag i If-Match. Version number increments na każdy update. Stale ETag → 412 Precondition Failed. Prevents lost updates bez database locks.

**Q3: How do you test idempotency?**
> Two scenarios: (1) replay z same key + same action = cached result (200), (2) replay z same key + different action = conflict (409). Ensures retry logic works, prevents double-charges.

---

## 🏗️ Architecture Overview

### State Machine
```
CREATED → AUTHORIZED → CAPTURED → REFUNDED
    ↓         ↓
CANCELLED  CANCELLED
                ↓
             EXPIRED (automatic after 7 days)
```

### API Endpoints (NEW)
```
POST /api/merchants/{merchantId}/payment-orders/{id}/authorize
POST /api/merchants/{merchantId}/payment-orders/{id}/capture
POST /api/merchants/{merchantId}/payment-orders/{id}/cancel
POST /api/merchants/{merchantId}/payment-orders/{id}/refund
PATCH /api/merchants/{merchantId}/payment-orders/{id} (metadata update)
OPTIONS /api/merchants/{merchantId}/payment-orders/{id}/* (CORS preflight)
```

### HTTP Headers (NEW)
```
Request:
  If-Match: "v1" (optimistic locking)
  Idempotency-Key: "key-123" (retry-safe)

Response:
  ETag: "v2" (version incremented)
  Cache-Control: no-store (sensitive data)
  Vary: Authorization, If-Match (caching)
  Access-Control-Allow-Origin: http://localhost:3000 (CORS)
```

### Status Codes (NEW)
```
200 OK (success)
304 Not Modified (conditional GET)
412 Precondition Failed (stale ETag)
422 Unprocessable Entity (invalid transition)
409 Conflict (idempotency conflict)
```

---

## 🧪 Test Strategy

### Test Coverage
| Category | Scenarios | Priority |
|---|---|---|
| State transitions | 24 (6 statuses × 4 actions) | CRITICAL |
| Security matrix | 12 (roles × ownership) | CRITICAL |
| Optimistic locking | 8 (stale ETag, concurrent updates) | CRITICAL |
| Idempotency | 8 (replay, conflict) | CRITICAL |
| CORS | 4 (preflight, cross-origin) | HIGH |
| Cache-Control | 4 (no-store, Vary) | HIGH |
| PATCH | 4 (metadata update) | HIGH |
| Audit trail | 4 (status history) | HIGH |
| **Total** | **84** | - |

### Test Examples

**State Transition Test:**
```java
@ParameterizedTest
@MethodSource("validTransitions")
void validStateTransition(Status from, Action action, Status to) {
    // Given: Payment order in 'from' status
    // When: Execute 'action'
    // Then: Status transitions to 'to', 200 OK
}

@ParameterizedTest
@MethodSource("invalidTransitions")
void invalidStateTransition(Status from, Action action) {
    // Given: Payment order in 'from' status
    // When: Execute 'action'
    // Then: 422 Unprocessable Entity, error code 'invalid_transition'
}
```

**Optimistic Locking Test:**
```java
@Test
void staleETagReturns412() {
    // Given: Payment order with ETag "v2"
    // When: POST /authorize with If-Match: "v1"
    // Then: 412 Precondition Failed, error code 'concurrency_conflict'
}
```

**Idempotency Test:**
```java
@Test
void idempotentReplayReturnsCachedResult() {
    // Given: Payment order already authorized with Idempotency-Key: "key-123"
    // When: POST /authorize with same Idempotency-Key: "key-123"
    // Then: 200 OK, same response as original authorization
}

@Test
void idempotencyConflictReturns409() {
    // Given: Payment order with prior action using Idempotency-Key: "key-123"
    // When: POST /authorize with Idempotency-Key: "key-123" (different action)
    // Then: 409 Conflict, error code 'idempotency_conflict'
}
```

---

## 📋 Implementation Plan

### Phase 1: Data Model (Week 1, Days 1-2)
- [ ] Add `status` column to `payment_orders` table (enum: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED)
- [ ] Add `version` column to `payment_orders` table (optimistic locking)
- [ ] Add `authorized_at`, `expires_at`, `captured_at`, `cancelled_at`, `refunded_at` columns
- [ ] Create `payment_status_history` table (audit trail)
- [ ] Create Flyway migration `V4__add_payment_lifecycle.sql`

### Phase 2: Domain Model (Week 1, Days 3-4)
- [ ] Create `PaymentStatus` enum
- [ ] Create `PaymentLifecycleAction` enum (AUTHORIZE, CAPTURE, CANCEL, REFUND)
- [ ] Create `PaymentStatusHistory` entity
- [ ] Create `AuthorizationMetadata` value object
- [ ] Update `PaymentOrder` entity z version field i status transitions

### Phase 3: Service Layer (Week 1, Days 5-7)
- [ ] Create `PaymentLifecycleService` z 4 actions (authorize, capture, cancel, refund)
- [ ] Implement state machine validation (valid transitions)
- [ ] Implement optimistic locking (version check)
- [ ] Implement idempotency check (same key + same action = idempotent)
- [ ] Implement authorization expiration (lazy check)
- [ ] Create `PaymentStatusHistoryRepository`

### Phase 4: Controller Layer (Week 2, Days 1-3)
- [ ] Add 4 lifecycle endpoints (authorize, capture, cancel, refund)
- [ ] Add PATCH endpoint (metadata update)
- [ ] Add OPTIONS endpoint (CORS preflight)
- [ ] Implement `If-Match` header validation
- [ ] Implement `Idempotency-Key` header validation dla lifecycle actions
- [ ] Add `Cache-Control: no-store` i `Vary` headers
- [ ] Add CORS configuration

### Phase 5: Security (Week 2, Days 4-5)
- [ ] Add `merchant:payments:lifecycle` role
- [ ] Add `platform:payments:lifecycle` role
- [ ] Update `SecurityConfig` z lifecycle endpoint matchers
- [ ] Implement ownership checks dla lifecycle actions
- [ ] Implement platform override

### Phase 6: PSP Mock (Week 2, Days 6-7)
- [ ] Create `PspClient` interface
- [ ] Create `MockPspClient` (always succeeds)
- [ ] Integrate PSP mock z lifecycle service

### Phase 7: Testing (Week 3, Days 1-5)
- [ ] State transition tests (24 scenarios)
- [ ] Security matrix tests (12 scenarios)
- [ ] Optimistic locking tests (8 scenarios)
- [ ] Idempotency tests (8 scenarios)
- [ ] CORS tests (4 scenarios)
- [ ] Cache-Control tests (4 scenarios)
- [ ] PATCH tests (4 scenarios)
- [ ] Audit trail tests (4 scenarios)
- [ ] Regression tests (existing 54 tests still pass)

### Phase 8: Documentation (Week 3, Days 6-7)
- [ ] Update vault notes (Lesson 14 evidence)
- [ ] Update API documentation (Swagger/OpenAPI)
- [ ] Create interview prep questions
- [ ] Create learning outcomes checklist

---

## 🎯 Success Criteria

### Functional Requirements
- [ ] 4 lifecycle actions działają poprawnie (authorize, capture, cancel, refund)
- [ ] 6 statuses transition zgodnie z state machine
- [ ] Optimistic locking prevents lost updates (412)
- [ ] Idempotency działa dla lifecycle actions (replay = cached, conflict = 409)
- [ ] Authorization expiration działa (lazy check)
- [ ] Audit trail loguje wszystkie status transitions
- [ ] CORS headers obecne dla lifecycle endpoints
- [ ] Cache-Control: no-store obecne dla lifecycle responses

### Non-Functional Requirements
- [ ] 84 test scenarios pass (state transitions, security, concurrency, idempotency, HTTP hardening)
- [ ] Existing 54 tests still pass (regression)
- [ ] Response time < 500ms dla lifecycle actions
- [ ] No database deadlocks
- [ ] No race conditions w concurrent scenarios

### Learning Outcomes
- [ ] Learner can design i test state machine transitions
- [ ] Learner can implement optimistic locking
- [ ] Learner can implement idempotency dla non-create actions
- [ ] Learner can test CORS preflight requests
- [ ] Learner can test Cache-Control headers
- [ ] Learner can explain payment lifecycle (authorize → capture → refund)

---

## 🚀 Next Steps

### Immediate Actions (This Week)
1. **Approve Lesson 14 scope** (Payment Lifecycle Foundation)
2. **Create Spec Kit artifacts:**
   ```bash
   mkdir -p specs/009-payment-lifecycle-foundation
   cd specs/009-payment-lifecycle-foundation
   touch spec.md plan.md tasks.md research.md data-model.md
   mkdir contracts
   touch contracts/payment-lifecycle-api.md
   ```
3. **Run `/speckit.specify`** dla Lesson 14
4. **Run `/speckit.plan`** dla Lesson 14
5. **Run `/speckit.tasks`** dla Lesson 14

### Short-Term Actions (Next 2-3 Weeks)
1. **Implement production code** (no new tests w fazie specify/plan)
2. **Verify z istniejącymi testami** (regression)
3. **Update vault notes** (Lesson 14 evidence)
4. **Run `/speckit.analyze`** dla consistency check
5. **Run `/speckit.implement`** dla implementation

### Long-Term Actions (Next 2-3 Months)
1. **Lesson 15:** Advanced Lifecycle (partial capture, multi-refund, disputes)
2. **Lesson 16:** PSP Integration Mock (realistic PSP responses, failures)
3. **Lesson 17:** Webhooks & Event Pipeline (async notifications)
4. **Lesson 18:** Performance & Observability (query optimization, metrics)
5. **Lesson 19:** Chaos Engineering & Resilience (circuit breaker, bulkhead)
6. **Lesson 20:** GraphQL & gRPC (awareness)

---

## 📚 Additional Resources

### Documents Created
1. **BA_DISCOVERY_PACK.md** - Szczegółowy business analysis dla Lesson 14
   - Capability proposal
   - Business goal
   - Actors i stakeholders
   - Business workflow
   - Business rules i decisions
   - Domain vocabulary
   - Data needs
   - Candidate acceptance criteria
   - Ambiguities i open questions
   - Initial tester lens
   - Feature sequencing recommendation
   - Spec Kit input summary

2. **COMPREHENSIVE_GAP_ANALYSIS.md** - Kompletna lista zagadnień HTTP/REST/Security
   - 76 zagadnień w 7 kategoriach
   - Status (mamy/brak/częściowo)
   - Priority (critical/high/medium/low)
   - Learning value (1-5 gwiazdek)
   - Roadmapa lekcji 14-20
   - Test scenarios per lesson

3. **EXECUTIVE_SUMMARY.md** - Ten dokument
   - TL;DR
   - Key findings
   - Learning value
   - Architecture overview
   - Test strategy
   - Implementation plan
   - Success criteria
   - Next steps

### Skills Used
- `payment-quality-lab-orchestrator` - Project orchestration
- `business-analysis-and-product-discovery-for-payment-lab` - BA discovery
- `qa-architecture-sprint-team` - QA architecture
- `rest-api-security-oauth-testing` - Security matrix design

---

## 💡 Key Insights

### Insight 1: Payment Lifecycle = Foundation
Bez lifecycle, system to "payment order registry" - można tworzyć i czytać, ale nie można wykonywać realistic payment operations. Testerzy nie mogą ćwiczyć:
- State machine transitions
- Optimistic locking
- Idempotency dla non-create actions
- Concurrency control
- Audit trail

### Insight 2: HTTP Hardening Requires State Transitions
Wiele HTTP concepts (If-Match, 412, PATCH, Cache-Control) wymaga state transitions, żeby miały sens. Na przykład:
- **If-Match / 412** - potrzeba concurrent updates, żeby testować optimistic locking
- **PATCH** - potrzeba metadata updates bez zmiany statusu
- **Cache-Control: no-store** - potrzeba sensitive data (financial transactions)

### Insight 3: Realistic Testing Requires Realistic Business Logic
Testerzy nie mogą ćwiczyć realistic scenarios na toy example. Payment lifecycle daje:
- Realistic state machine (6 statuses, 24 transitions)
- Realistic concurrency scenarios (optimistic locking)
- Realistic retry logic (idempotency)
- Realistic security matrix (roles, ownership, platform override)

### Insight 4: Lesson 14 = Prerequisite dla Advanced Features
Bez lifecycle, nie można implementować:
- PSP integration (Lesson 16) - PSP musi wiedzieć, czy to authorize, capture, czy refund
- Webhooks (Lesson 17) - webhooks emitują events dla lifecycle actions
- Disputes (Lesson 15) - disputes wymagają refund logic
- Performance testing (Lesson 18) - performance zależy od state transitions

---

## 🎓 Final Recommendation

**START Lesson 14 NOW.**

**Dlaczego:**
1. System incomplete bez lifecycle
2. 28 critical HTTP/REST/Security gaps do zaadresowania
3. 84 test scenarios do nauczenia
4. Prerequisite dla advanced features
5. Realistic PayU-like system

**Co dostarczysz:**
- 4 lifecycle actions (authorize, capture, cancel, refund)
- 6 statuses (CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED)
- Optimistic locking (If-Match / ETag / 412)
- Idempotency dla lifecycle actions
- Payment status history (audit trail)
- HTTP hardening (CORS, Cache-Control, Vary, PATCH)
- Simple PSP mock

**Czas trwania:** 2-3 tygodnie

**Impact:** ⭐⭐⭐⭐⭐ (CRITICAL)

---

**End of Executive Summary**
