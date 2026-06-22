# BA Discovery Pack: Payment Lifecycle Foundation

**Date:** 2026-06-04  
**Feature ID:** 009-payment-lifecycle-foundation  
**Status:** Discovery Complete - Ready for Spec Kit

---

## 1. Capability Proposal

### Working Name
**Payment Lifecycle Foundation** - Core payment state transitions with HTTP protocol hardening

### Why Now
- Lessons 06-13 zbudowały solidną bazę (create/read/list/summary)
- System ma tylko status `CREATED` - brak rzeczywistego payment lifecycle
- Brakuje kluczowych HTTP concepts: CORS, caching, PATCH/PUT, conditional updates
- Testerzy nie mogą ćwiczyć realistic payment scenarios bez lifecycle
- PayU-like system wymaga authorize/capture/cancel/refund flows

### Roadmap Fit
```
Lesson 06-09: Foundation (create/read/list/summary) ✅
Lesson 10-13: Hardening (HTTP edge, framework, assertions, Spring layers) ✅
Lesson 14: Payment Lifecycle Foundation ← YOU ARE HERE
Lesson 15: Advanced Lifecycle (refund, partial capture)
Lesson 16: PSP Integration Mock
Lesson 17: Webhooks & Event Pipeline
```

---

## 2. Business Goal

### Business Problem
Obecny system to "payment order registry" - można tworzyć i czytać orders, ale nie można:
- Autoryzować płatności (rezerwować środków)
- Capture'ować (pobierać zarezerwowane środki)
- Anulować (zwalniać rezerwację)
- Zwracać (refundować po capture)

To nie jest realistic PayU-like system. Testerzy nie mogą ćwiczyć:
- State machine transitions
- Conditional HTTP headers (`If-Match` / `412`)
- PATCH dla partial updates
- Idempotency dla lifecycle actions
- Concurrency control (optimistic locking)

### Desired Outcome
System z realistic payment lifecycle:
```
CREATED → AUTHORIZED → CAPTURED
    ↓         ↓
  CANCELLED  CANCELLED
                ↓
             REFUNDED
```

Każda transition:
- Wymaga odpowiedniej roli (`merchant:payments:lifecycle`)
- Jest idempotentna (retry-safe z `Idempotency-Key`)
- Używa optimistic locking (`If-Match` / `ETag` / `412`)
- Zwraca odpowiednie HTTP status codes (200, 409, 412, 422)
- Loguje business events (audit trail)

### Consequence of Not Solving
- Testerzy nie mogą ćwiczyć realistic payment scenarios
- System pozostaje "toy example" zamiast production-grade
- Brak coverage dla critical payment operations
- Nie można testować concurrency, retry logic, state machine correctness

---

## 3. Actors and Stakeholders

### Primary Actor
**Merchant Payment Operator**
- Role: `merchant:payments:lifecycle`
- Goal: Execute payment lifecycle actions (authorize, capture, cancel, refund)
- Boundary: Must have matching `merchant_id` claim

### Secondary Actors
**Platform Payment Administrator**
- Role: `platform:payments:lifecycle`
- Goal: Override/force lifecycle actions for dispute resolution
- Boundary: Can act on any merchant's payment orders

**System Auditor**
- Role: `platform:payments:audit`
- Goal: Read payment status history for compliance
- Boundary: Read-only access to all payment orders

### Internal Stakeholders
- **Risk Team**: Monitor cancel/refund rates
- **Finance Team**: Reconcile captured vs refunded amounts
- **Support Team**: Investigate payment disputes

### Actor Goals Summary
| Actor | Primary Goal | Secondary Goal |
|---|---|---|
| Merchant Operator | Execute lifecycle actions | Retry failed actions safely |
| Platform Admin | Override merchant actions | Resolve disputes |
| Auditor | Read status history | Generate compliance reports |

---

## 4. Business Workflow

### 4.1 Main Success Path: Full Payment Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│ 1. CREATE (Lesson 06)                                       │
│    POST /payment-orders                                     │
│    Status: CREATED                                          │
│    └─> Order created, awaiting authorization                │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. AUTHORIZE (NEW)                                          │
│    POST /payment-orders/{id}/authorize                      │
│    Status: CREATED → AUTHORIZED                             │
│    └─> Funds reserved, expiration timestamp set             │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. CAPTURE (NEW)                                            │
│    POST /payment-orders/{id}/capture                        │
│    Status: AUTHORIZED → CAPTURED                            │
│    └─> Funds transferred, settlement initiated              │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. REFUND (NEW - Lesson 15)                                 │
│    POST /payment-orders/{id}/refund                         │
│    Status: CAPTURED → REFUNDED                              │
│    └─> Funds returned to customer                           │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Alternate Paths

**Path A: Cancel Before Authorization**
```
CREATED → CANCELLED (via POST /payment-orders/{id}/cancel)
└─> Order cancelled, no funds reserved
```

**Path B: Cancel After Authorization**
```
AUTHORIZED → CANCELLED (via POST /payment-orders/{id}/cancel)
└─> Authorization voided, funds released
```

**Path C: Expiration**
```
AUTHORIZED → EXPIRED (automatic after 7 days)
└─> Authorization expired, funds released
```

### 4.3 Failure Paths

**Failure 1: Invalid State Transition**
```
POST /payment-orders/{id}/capture
Status: CREATED (not AUTHORIZED)
Response: 422 Unprocessable Entity
Body: { "error": "invalid_transition", "message": "Cannot capture from CREATED status" }
```

**Failure 2: Optimistic Locking Conflict**
```
POST /payment-orders/{id}/capture
If-Match: "etag-v1" (stale)
Current ETag: "etag-v2"
Response: 412 Precondition Failed
Body: { "error": "concurrency_conflict", "message": "Payment order modified by another request" }
```

**Failure 3: Idempotency Conflict**
```
POST /payment-orders/{id}/authorize
Idempotency-Key: "key-123" (already used for different action)
Response: 409 Conflict
Body: { "error": "idempotency_conflict", "message": "Idempotency key already used for different action" }
```

### 4.4 Key State Changes

| From | To | Trigger | Side Effects |
|---|---|---|---|
| CREATED | AUTHORIZED | authorize action | Set expiration timestamp, reserve funds |
| CREATED | CANCELLED | cancel action | Mark as cancelled |
| AUTHORIZED | CAPTURED | capture action | Transfer funds, clear expiration |
| AUTHORIZED | CANCELLED | cancel action | Void authorization, release funds |
| AUTHORIZED | EXPIRED | automatic (7 days) | Release funds |
| CAPTURED | REFUNDED | refund action | Return funds |

### 4.5 External Interactions

**PSP Mock (Lesson 16)**
- authorize → PSP reserves funds
- capture → PSP transfers funds
- cancel → PSP releases funds
- refund → PSP returns funds

**Webhooks (Lesson 17)**
- authorization.completed
- capture.completed
- refund.completed
- payment.expired

---

## 5. Business Rules and Decisions

### 5.1 Explicit Rules

**R1: Authorization Expiration**
- Authorization expires after 7 days (configurable)
- Expired authorizations automatically transition to EXPIRED
- Cannot capture expired authorization

**R2: Partial Capture**
- Capture amount can be less than authorized amount (partial capture)
- Remaining authorized amount is released
- Only one capture per authorization (no multi-capture in Lesson 14)

**R3: Refund Constraints**
- Refund amount cannot exceed captured amount
- Multiple partial refunds allowed (Lesson 15)
- Refund must be initiated within 180 days of capture

**R4: Cancellation Rules**
- CREATED → CANCELLED: Always allowed
- AUTHORIZED → CANCELLED: Always allowed (voids authorization)
- CAPTURED → CANCELLED: Not allowed (must use refund)

**R5: Idempotency**
- Each lifecycle action requires `Idempotency-Key` header
- Same key + same action = idempotent (returns cached result)
- Same key + different action = 409 Conflict

**R6: Optimistic Locking**
- All lifecycle actions require `If-Match` header with current ETag
- Stale ETag → 412 Precondition Failed
- Prevents lost updates in concurrent scenarios

### 5.2 Thresholds

| Parameter | Value | Rationale |
|---|---|---|
| Authorization expiration | 7 days | Industry standard |
| Refund window | 180 days | Payment card network rules |
| Max refund amount | Captured amount | Cannot refund more than captured |
| Max partial capture | Authorized amount | Cannot capture more than authorized |

### 5.3 Role/Permission Constraints

| Action | Required Role | Ownership Check |
|---|---|---|
| authorize | `merchant:payments:lifecycle` | `merchant_id` claim must match |
| capture | `merchant:payments:lifecycle` | `merchant_id` claim must match |
| cancel | `merchant:payments:lifecycle` | `merchant_id` claim must match |
| refund | `merchant:payments:lifecycle` | `merchant_id` claim must match |
| force-action | `platform:payments:lifecycle` | No ownership check (override) |

### 5.4 Timing Rules

**T1: Authorization Hold**
- Funds reserved immediately on authorize
- Hold expires after 7 days if not captured
- Merchant can cancel before expiration

**T2: Capture Settlement**
- Funds transferred within 1-3 business days (PSP dependent)
- Settlement timestamp recorded for reconciliation

**T3: Refund Processing**
- Refund initiated immediately
- Funds returned within 5-10 business days (PSP dependent)

### 5.5 Decision Tables

**Decision Table 1: Valid State Transitions**

| Current Status | authorize | capture | cancel | refund |
|---|---|---|---|---|
| CREATED | ✅ | ❌ | ✅ | ❌ |
| AUTHORIZED | ❌ | ✅ | ✅ | ❌ |
| CAPTURED | ❌ | ❌ | ❌ | ✅ |
| CANCELLED | ❌ | ❌ | ❌ | ❌ |
| EXPIRED | ❌ | ❌ | ❌ | ❌ |
| REFUNDED | ❌ | ❌ | ❌ | ❌ |

**Decision Table 2: HTTP Response Codes**

| Scenario | Status Code | Error Code |
|---|---|---|
| Valid transition | 200 OK | - |
| Invalid transition | 422 Unprocessable Entity | `invalid_transition` |
| Stale ETag | 412 Precondition Failed | `concurrency_conflict` |
| Idempotency conflict | 409 Conflict | `idempotency_conflict` |
| Missing header | 400 Bad Request | `missing_required_header` |
| Unauthorized | 403 Forbidden | `forbidden` |
| Not found | 404 Not Found | `not_found` |

---

## 6. Domain Vocabulary

### New Terms

**Authorization**
- Definition: Temporary hold on customer funds
- Lifecycle: CREATED → AUTHORIZED → (CAPTURED | CANCELLED | EXPIRED)
- Expiration: 7 days from authorization timestamp

**Capture**
- Definition: Transfer of authorized funds to merchant
- Prerequisite: AUTHORIZED status
- Side effect: Clears authorization expiration

**Void**
- Definition: Cancellation of authorization before capture
- Prerequisite: AUTHORIZED status
- Side effect: Releases reserved funds

**Refund**
- Definition: Return of captured funds to customer
- Prerequisite: CAPTURED status
- Constraint: Amount ≤ captured amount

**Settlement**
- Definition: Process of transferring funds from PSP to merchant
- Trigger: Capture action
- Duration: 1-3 business days

### New Statuses

| Status | Description | Terminal? |
|---|---|---|
| AUTHORIZED | Funds reserved, awaiting capture | No |
| CAPTURED | Funds transferred to merchant | No (can refund) |
| CANCELLED | Order cancelled (before or after authorization) | Yes |
| EXPIRED | Authorization expired automatically | Yes |
| REFUNDED | Funds returned to customer | Yes |

### New Entities

**PaymentStatusHistory**
- Purpose: Audit trail for all status transitions
- Fields: `payment_order_id`, `from_status`, `to_status`, `actor`, `timestamp`, `reason`, `idempotency_key`
- Constraint: Immutable (insert-only)

**AuthorizationMetadata**
- Purpose: Track authorization-specific data
- Fields: `payment_order_id`, `authorized_amount`, `authorized_at`, `expires_at`, `captured_amount`
- Lifecycle: Created on authorize, updated on capture/cancel

---

## 7. Data Needs

### 7.1 Required Inputs

**Authorize Action**
```json
{
  "amountMinor": 5000,  // Optional: partial authorization
  "currency": "PLN"
}
```

**Capture Action**
```json
{
  "amountMinor": 5000,  // Optional: partial capture
  "currency": "PLN"
}
```

**Cancel Action**
```json
{
  "reason": "Customer requested cancellation"  // Optional
}
```

**Refund Action**
```json
{
  "amountMinor": 2500,  // Required: refund amount
  "currency": "PLN",
  "reason": "Product returned"  // Optional
}
```

### 7.2 Optional Inputs

- `reason`: Human-readable explanation for cancel/refund
- `metadata`: Key-value pairs for custom data (e.g., order ID, customer ID)

### 7.3 Outputs

**Lifecycle Action Response**
```json
{
  "paymentOrderId": "uuid",
  "status": "AUTHORIZED",
  "amountMinor": 5000,
  "currency": "PLN",
  "authorizedAt": "2026-06-04T10:00:00Z",
  "expiresAt": "2026-06-11T10:00:00Z",
  "version": 2,
  "_links": {
    "self": { "href": "/api/merchants/{merchantId}/payment-orders/{id}" },
    "capture": { "href": "/api/merchants/{merchantId}/payment-orders/{id}/capture" },
    "cancel": { "href": "/api/merchants/{merchantId}/payment-orders/{id}/cancel" }
  }
}
```

### 7.4 Identifiers

- `payment_order_id`: UUID (existing)
- `idempotency_key`: String (existing, now required for lifecycle actions)
- `correlation_id`: UUID (existing, propagated via `X-Correlation-ID`)

### 7.5 Audit/Status Data

**PaymentStatusHistory Record**
```json
{
  "statusHistoryId": "uuid",
  "paymentOrderId": "uuid",
  "fromStatus": "CREATED",
  "toStatus": "AUTHORIZED",
  "actor": "user-123",
  "timestamp": "2026-06-04T10:00:00Z",
  "reason": null,
  "idempotencyKey": "key-456",
  "correlationId": "uuid"
}
```

### 7.6 Test Data Categories

| Category | Purpose | Example |
|---|---|---|
| Fresh order | Test authorize from CREATED | `CREATED` order, no prior actions |
| Authorized order | Test capture/cancel | `AUTHORIZED` order, valid expiration |
| Expired authorization | Test capture rejection | `AUTHORIZED` order, past expiration |
| Captured order | Test refund | `CAPTURED` order, within refund window |
| Cancelled order | Test invalid transitions | `CANCELLED` order, all actions rejected |
| Concurrent modifications | Test optimistic locking | Two requests with same ETag |
| Idempotency replay | Test retry logic | Same `Idempotency-Key`, same action |
| Idempotency conflict | Test conflict detection | Same `Idempotency-Key`, different action |

---

## 8. Candidate Acceptance Criteria

### 8.1 Authorization

**AC-AUTH-001**: Merchant with `merchant:payments:lifecycle` role can authorize CREATED payment order
- Given: Payment order in CREATED status
- When: POST `/payment-orders/{id}/authorize` with valid `Idempotency-Key` and `If-Match`
- Then: Status transitions to AUTHORIZED, expiration set to 7 days, 200 OK returned

**AC-AUTH-002**: Authorization requires `If-Match` header with current ETag
- Given: Payment order with ETag "v1"
- When: POST `/payment-orders/{id}/authorize` without `If-Match`
- Then: 400 Bad Request, error code `missing_required_header`

**AC-AUTH-003**: Stale ETag returns 412 Precondition Failed
- Given: Payment order with ETag "v2"
- When: POST `/payment-orders/{id}/authorize` with `If-Match: "v1"`
- Then: 412 Precondition Failed, error code `concurrency_conflict`

**AC-AUTH-004**: Idempotent authorization returns cached result
- Given: Payment order already authorized with `Idempotency-Key: "key-123"`
- When: POST `/payment-orders/{id}/authorize` with same `Idempotency-Key: "key-123"`
- Then: 200 OK, same response as original authorization

**AC-AUTH-005**: Idempotency conflict returns 409
- Given: Payment order with prior action using `Idempotency-Key: "key-123"`
- When: POST `/payment-orders/{id}/authorize` with `Idempotency-Key: "key-123"` (different action)
- Then: 409 Conflict, error code `idempotency_conflict`

### 8.2 Capture

**AC-CAP-001**: Merchant can capture AUTHORIZED payment order
- Given: Payment order in AUTHORIZED status, not expired
- When: POST `/payment-orders/{id}/capture` with valid headers
- Then: Status transitions to CAPTURED, 200 OK returned

**AC-CAP-002**: Partial capture allowed
- Given: AUTHORIZED order with amount 10000
- When: POST `/payment-orders/{id}/capture` with `amountMinor: 5000`
- Then: Status transitions to CAPTURED, captured amount = 5000, remaining 5000 released

**AC-CAP-003**: Capture rejects expired authorization
- Given: AUTHORIZED order with expiration in the past
- When: POST `/payment-orders/{id}/capture`
- Then: 422 Unprocessable Entity, error code `authorization_expired`

**AC-CAP-004**: Capture amount cannot exceed authorized amount
- Given: AUTHORIZED order with amount 10000
- When: POST `/payment-orders/{id}/capture` with `amountMinor: 15000`
- Then: 422 Unprocessable Entity, error code `capture_amount_exceeds_authorized`

### 8.3 Cancel

**AC-CAN-001**: Merchant can cancel CREATED payment order
- Given: Payment order in CREATED status
- When: POST `/payment-orders/{id}/cancel`
- Then: Status transitions to CANCELLED, 200 OK returned

**AC-CAN-002**: Merchant can cancel AUTHORIZED payment order (void)
- Given: Payment order in AUTHORIZED status
- When: POST `/payment-orders/{id}/cancel`
- Then: Status transitions to CANCELLED, authorization voided, 200 OK returned

**AC-CAN-003**: Cancel rejects CAPTURED payment order
- Given: Payment order in CAPTURED status
- When: POST `/payment-orders/{id}/cancel`
- Then: 422 Unprocessable Entity, error code `invalid_transition`

### 8.4 Refund

**AC-REF-001**: Merchant can refund CAPTURED payment order
- Given: Payment order in CAPTURED status, within 180-day window
- When: POST `/payment-orders/{id}/refund` with `amountMinor: 5000`
- Then: Status transitions to REFUNDED, 200 OK returned

**AC-REF-002**: Partial refund allowed
- Given: CAPTURED order with amount 10000
- When: POST `/payment-orders/{id}/refund` with `amountMinor: 5000`
- Then: Status transitions to REFUNDED, refunded amount = 5000

**AC-REF-003**: Refund amount cannot exceed captured amount
- Given: CAPTURED order with amount 10000
- When: POST `/payment-orders/{id}/refund` with `amountMinor: 15000`
- Then: 422 Unprocessable Entity, error code `refund_amount_exceeds_captured`

**AC-REF-004**: Refund rejects non-CAPTURED payment order
- Given: Payment order in AUTHORIZED status
- When: POST `/payment-orders/{id}/refund`
- Then: 422 Unprocessable Entity, error code `invalid_transition`

### 8.5 HTTP Protocol

**AC-HTTP-001**: Lifecycle actions return Cache-Control: no-store
- Given: Any lifecycle action
- When: Request executed
- Then: Response includes `Cache-Control: no-store` (sensitive financial data)

**AC-HTTP-002**: Lifecycle actions return updated ETag
- Given: Payment order with ETag "v1"
- When: POST `/payment-orders/{id}/authorize` with `If-Match: "v1"`
- Then: Response includes `ETag: "v2"` (version incremented)

**AC-HTTP-003**: Lifecycle actions return X-Correlation-ID
- Given: Request with `X-Correlation-ID: "corr-123"`
- When: Lifecycle action executed
- Then: Response includes `X-Correlation-ID: "corr-123"`

**AC-HTTP-004**: CORS headers present for lifecycle endpoints
- Given: Request with `Origin: http://localhost:3000`
- When: OPTIONS `/payment-orders/{id}/authorize`
- Then: Response includes `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`

### 8.6 Security

**AC-SEC-001**: Unauthorized role rejected
- Given: User with `merchant:payments:read` (not `lifecycle`)
- When: POST `/payment-orders/{id}/authorize`
- Then: 403 Forbidden, error code `forbidden`

**AC-SEC-002**: Cross-merchant access rejected
- Given: Merchant A's token
- When: POST `/merchants/B/payment-orders/{id}/authorize`
- Then: 403 Forbidden, error code `forbidden`

**AC-SEC-003**: Platform admin can override
- Given: User with `platform:payments:lifecycle`
- When: POST `/merchants/A/payment-orders/{id}/authorize`
- Then: 200 OK (platform override allowed)

---

## 9. Ambiguities and Open Questions

### 9.1 Unresolved Decisions

**Q1: Multi-Capture Support**
- Question: Can one authorization be captured multiple times (partial captures)?
- Options:
  - A) Single capture only (simpler, Lesson 14)
  - B) Multi-capture allowed (complex, Lesson 15)
- Recommendation: **A) Single capture only** for Lesson 14
- Impact: Simplifies state machine, reduces test scenarios

**Q2: Automatic Expiration Handling**
- Question: How to handle automatic expiration after 7 days?
- Options:
  - A) Scheduled job (cron) checks and expires
  - B) Lazy expiration (check on next access)
  - C) Both (scheduled + lazy)
- Recommendation: **B) Lazy expiration** for Lesson 14
- Impact: Simpler implementation, no background jobs needed

**Q3: Refund Status Model**
- Question: Should refund create new status or separate entity?
- Options:
  - A) New status `REFUNDED` (simple, single refund)
  - B) Separate `refunds` table (complex, multiple refunds)
- Recommendation: **A) New status `REFUNDED`** for Lesson 14
- Impact: Simpler data model, single refund per order

**Q4: PSP Mock Strategy**
- Question: Should Lesson 14 include PSP mock or defer to Lesson 16?
- Options:
  - A) Include simple PSP mock (always succeeds)
  - B) Defer PSP mock to Lesson 16
- Recommendation: **A) Include simple PSP mock** for Lesson 14
- Impact: Enables realistic testing without external dependencies

### 9.2 Assumptions

**A1: Authorization Amount**
- Assumption: Authorization amount equals payment order amount (no partial authorization in Lesson 14)
- Validation: Check if PayU supports partial authorization
- Risk: Low (can add partial authorization in Lesson 15)

**A2: Currency Consistency**
- Assumption: All lifecycle actions use same currency as original payment order
- Validation: Check if currency conversion is needed
- Risk: Low (currency mismatch can be rejected with 422)

**A3: Timezone Handling**
- Assumption: All timestamps stored in UTC, displayed in user's timezone
- Validation: Check existing timestamp handling in system
- Risk: Low (consistent with existing system)

### 9.3 Questions for Tester

**TQ1: Concurrency Testing**
- Question: How to test optimistic locking without flaky tests?
- Options:
  - A) Sequential requests with manual ETag manipulation
  - B) Parallel requests with retry logic
  - C) Mock repository to force conflict
- Recommendation: **A) Sequential requests** for Lesson 14
- Impact: Deterministic tests, easier to debug

**TQ2: Expiration Testing**
- Question: How to test 7-day expiration without waiting 7 days?
- Options:
  - A) Mock system clock
  - B) Use test-specific expiration (e.g., 10 seconds)
  - C) Directly manipulate database timestamp
- Recommendation: **B) Test-specific expiration** for Lesson 14
- Impact: Fast tests, no mocking complexity

---

## 10. Initial Tester Lens

### 10.1 Highest Product Risks

**Risk 1: Invalid State Transitions**
- Scenario: User attempts to capture CREATED order
- Impact: Financial loss, system inconsistency
- Test Strategy: Exhaustive state transition matrix (6 statuses × 4 actions = 24 scenarios)

**Risk 2: Lost Updates (Concurrency)**
- Scenario: Two requests with same ETag, different actions
- Impact: One action lost, financial discrepancy
- Test Strategy: Optimistic locking tests with sequential and parallel requests

**Risk 3: Idempotency Failures**
- Scenario: Retry with same `Idempotency-Key` creates duplicate action
- Impact: Double charge, customer dispute
- Test Strategy: Idempotency replay tests for each action

**Risk 4: Authorization Expiration**
- Scenario: Capture succeeds on expired authorization
- Impact: Funds not actually reserved, settlement fails
- Test Strategy: Expiration boundary tests (just before, just after)

### 10.2 Visible States and Boundaries

**States:**
- CREATED → AUTHORIZED → CAPTURED → REFUNDED
- CREATED → CANCELLED
- AUTHORIZED → CANCELLED
- AUTHORIZED → EXPIRED

**Boundaries:**
- Authorization amount: 0 < amount ≤ order amount
- Capture amount: 0 < amount ≤ authorized amount
- Refund amount: 0 < amount ≤ captured amount
- Expiration: 7 days from authorization
- Refund window: 180 days from capture

### 10.3 Ownership and Authorization Questions

**Q1: Platform Override Scope**
- Question: Can platform admin force invalid transitions (e.g., CAPTURED → AUTHORIZED)?
- Recommendation: No, platform admin can only force valid transitions with relaxed ownership check
- Test Strategy: Platform admin matrix (valid + invalid transitions)

**Q2: Merchant Self-Cancel**
- Question: Can merchant cancel after capture?
- Recommendation: No, must use refund
- Test Strategy: Cancel rejection tests for CAPTURED status

### 10.4 Hard-to-Test Scenarios

**Scenario 1: Expiration Race Condition**
- Challenge: Authorization expires during capture request
- Test Strategy: Mock expiration check to return "expired" mid-request
- Risk: High (requires careful mocking)

**Scenario 2: PSP Failure**
- Challenge: PSP mock fails intermittently
- Test Strategy: Defer to Lesson 16 (PSP integration)
- Risk: Low (Lesson 14 PSP mock always succeeds)

### 10.5 Concurrency and Retry Risks

**Risk 1: Double Capture**
- Scenario: Network timeout, retry with same `Idempotency-Key`
- Mitigation: Idempotency check before capture
- Test: Idempotency replay test

**Risk 2: Cancel During Capture**
- Scenario: User cancels while capture is in progress
- Mitigation: Optimistic locking (ETag check)
- Test: Concurrent cancel + capture test

---

## 11. Feature Sequencing Recommendation

### Recommendation: **NEXT**

**Rationale:**
- Builds on solid foundation (Lessons 06-13)
- Introduces critical payment concepts (lifecycle, state machine)
- Enables realistic testing scenarios
- Prerequisite for advanced features (PSP, webhooks)

**Not Deferred Because:**
- System incomplete without lifecycle
- Testers cannot practice realistic scenarios
- HTTP concepts (If-Match, 412) require state transitions

**Not Split Because:**
- Lifecycle actions are tightly coupled (authorize → capture → refund)
- Splitting would create artificial boundaries
- Single sprint can deliver minimal viable lifecycle

**Not Merged Because:**
- Scope already substantial (4 actions, 6 statuses, 24 transition scenarios)
- Merging with PSP/webhooks would explode complexity

### Sequencing

```
Lesson 14: Payment Lifecycle Foundation (authorize, capture, cancel, refund)
    ↓
Lesson 15: Advanced Lifecycle (partial capture, multi-refund, disputes)
    ↓
Lesson 16: PSP Integration Mock (realistic PSP responses, failures)
    ↓
Lesson 17: Webhooks & Event Pipeline (async notifications)
```

---

## 12. Spec Kit Input Summary

### Suggested Feature Title
**Payment Lifecycle Foundation with HTTP Protocol Hardening**

### One-Paragraph Feature Intent
Introduce realistic payment lifecycle (authorize, capture, cancel, refund) with state machine transitions, optimistic locking (`If-Match` / `412`), idempotency for lifecycle actions, and HTTP protocol hardening (CORS, caching, PATCH). This transforms the system from a "payment order registry" into a production-grade payment platform, enabling testers to practice realistic payment scenarios including state transitions, concurrency control, retry logic, and security matrices.

### Recommended Scope

**In Scope:**
- 4 lifecycle actions: authorize, capture, cancel, refund
- 6 statuses: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED
- Optimistic locking with `If-Match` / `ETag` / `412`
- Idempotency for lifecycle actions
- Payment status history (audit trail)
- HTTP protocol hardening: CORS, Cache-Control, Vary
- Simple PSP mock (always succeeds)
- Lazy expiration (check on access)

**Out of Scope:**
- Partial authorization (Lesson 15)
- Multi-capture (Lesson 15)
- Multi-refund (Lesson 15)
- PSP failure scenarios (Lesson 16)
- Webhooks (Lesson 17)
- Scheduled expiration job (Lesson 16)
- Dispute handling (Lesson 15)

### Recommended Non-Goals

- ❌ Complete PSP integration (Lesson 16)
- ❌ Webhook/event pipeline (Lesson 17)
- ❌ Partial authorization (Lesson 15)
- ❌ Multi-capture (Lesson 15)
- ❌ Multi-refund (Lesson 15)
- ❌ Dispute handling (Lesson 15)
- ❌ Scheduled expiration job (Lesson 16)
- ❌ GraphQL/gRPC (out of scope)
- ❌ Performance/load testing (out of scope)

### Must-Preserve Acceptance Criteria

1. **State Machine Correctness**: All 24 state transition scenarios tested (6 statuses × 4 actions)
2. **Optimistic Locking**: Stale ETag returns 412, prevents lost updates
3. **Idempotency**: Same key + same action = idempotent, same key + different action = 409
4. **Security**: Role-based access control, ownership checks, platform override
5. **HTTP Protocol**: CORS headers, Cache-Control: no-store, ETag versioning
6. **Audit Trail**: All status transitions logged in PaymentStatusHistory

### Open Questions for Clarification

1. **Multi-Capture**: Single capture only (Lesson 14) or multi-capture (Lesson 15)?
2. **Expiration Handling**: Lazy expiration (Lesson 14) or scheduled job (Lesson 16)?
3. **Refund Model**: Single refund status (Lesson 14) or separate refunds table (Lesson 15)?
4. **PSP Mock**: Include simple mock (Lesson 14) or defer to Lesson 16?

---

## Appendix A: HTTP Protocol Hardening Details

### A.1 CORS Configuration

```java
@Configuration
@Profile({"dev", "test"})
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key", "If-Match", "X-Correlation-ID")
            .exposedHeaders("ETag", "Location", "X-Correlation-ID")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

**Test Scenarios:**
- OPTIONS preflight → 200 + CORS headers
- Cross-origin POST → CORS headers in response
- Disallowed origin → 403 or no CORS headers

### A.2 Cache-Control Headers

```java
@PostMapping("/{id}/authorize")
public ResponseEntity<PaymentOrderResponse> authorize(...) {
    // ... lifecycle logic ...
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())  // Sensitive financial data
        .header("Vary", "Authorization, If-Match")
        .eTag("\"v" + updatedOrder.getVersion() + "\"")
        .header("X-Correlation-ID", correlationId)
        .body(response);
}
```

**Test Scenarios:**
- Lifecycle response includes `Cache-Control: no-store`
- Lifecycle response includes `Vary: Authorization, If-Match`
- ETag version incremented after each action

### A.3 PATCH for Metadata Updates

```java
@PatchMapping("/{id}")
public ResponseEntity<PaymentOrderResponse> updateMetadata(
    @PathVariable UUID id,
    @RequestBody Map<String, String> metadata,
    @RequestHeader("If-Match") String ifMatch) {
    // Update metadata without changing status
    // Requires optimistic locking
}
```

**Test Scenarios:**
- PATCH updates metadata without status change
- PATCH requires `If-Match` header
- PATCH returns 412 on stale ETag

---

## Appendix B: Learning Outcomes

### B.1 Technical Skills

After Lesson 14, learner can:
- [ ] Design and test state machine transitions
- [ ] Implement optimistic locking with `If-Match` / `ETag` / `412`
- [ ] Implement idempotency for non-create actions
- [ ] Test CORS preflight requests
- [ ] Test Cache-Control headers for sensitive data
- [ ] Test PATCH for partial updates
- [ ] Design security matrix for lifecycle actions
- [ ] Test concurrent modifications

### B.2 Business Skills

After Lesson 14, learner can:
- [ ] Explain payment lifecycle (authorize → capture → refund)
- [ ] Explain authorization expiration and void
- [ ] Explain partial capture and partial refund
- [ ] Explain idempotency for financial transactions
- [ ] Explain optimistic locking for concurrency control

### B.3 Interview Questions

**Q1: How do you test state machine transitions?**
> I use a decision table with all possible state × action combinations (24 scenarios for 6 statuses × 4 actions). Each scenario is a parameterized test that verifies the expected HTTP status code and error code. This ensures exhaustive coverage and makes it easy to add new states or actions.

**Q2: How do you prevent lost updates in concurrent scenarios?**
> I use optimistic locking with ETag and If-Match headers. Each payment order has a version number that increments on every update. Clients must send the current ETag in If-Match header. If the ETag is stale (version mismatch), the server returns 412 Precondition Failed. This prevents lost updates without database locks.

**Q3: How do you test idempotency?**
> I test two scenarios: (1) replay with same Idempotency-Key and same action returns cached result (200 OK), (2) replay with same Idempotency-Key but different action returns 409 Conflict. This ensures retry logic works correctly and prevents accidental double-charges.

---

**End of BA Discovery Pack**
