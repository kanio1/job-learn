# Phase 7J — Payment History / Audit Evidence Contract

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 41 live specs
> (1 status + 2 security smoke + 13 merchant contract + 25 payment order contract).

---

## Discovery Summary

### History endpoint

```
GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history
```

**Synchronous** — history rows are written within the same `@Transactional` method as each
lifecycle operation (`PaymentLifecycleService`). No Awaitility polling needed.

**Response shape** (`PaymentStatusHistoryResponse`):
```json
{
  "content": [
    {
      "statusHistoryId": "<UUID>",
      "paymentOrderId": "<UUID>",
      "fromStatus": "CREATED",
      "toStatus": "AUTHORIZED",
      "action": "AUTHORIZE",
      "actorSubject": "<JWT sub>",
      "correlationId": "<X-Correlation-ID value>",
      "createdAt": "<ISO-8601 Instant>"
    },
    ...
  ]
}
```

**Critical design note — creation entry excluded**: the repository uses
`findByPaymentOrderIdAndActionIsNotNullOrderByCreatedAtAsc`. The creation history row has
`action = null` (set by `PaymentOrderStatusHistory.creationEntry()`), so it is excluded from
every history response. A freshly created order returns `{"content":[]}`.

**Ordering**: `ORDER BY createdAt ASC` — chronological, oldest lifecycle action first.

**Authorization** (enforced at the Spring Security filter chain level):
```java
.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*/history")
    .hasAnyAuthority(
        "merchant:payments:read",
        "merchant:payments:lifecycle",
        "platform:payments:read",
        "platform:payments:lifecycle",
        "platform:payments:audit"
    )
```
Additionally, within the controller: non-platform callers must have a `merchant_id` JWT claim
matching the `merchantId` path parameter.

**Response headers**: `Vary: Authorization`, `Cache-Control: no-store` (same as payment GET).

### Audit endpoint (`/api/audit`)

Discovered but deferred. The audit module (`AuditController`, `AuditEventListener`,
`AuditEventService`) is implemented and uses event-driven persistence (`AuditableActionOccurred`
Spring Application Events consumed by `AuditEventListener`). The audit event write path is
**asynchronous** (event listener runs in a separate transaction), making it unsuitable for
deterministic black-box testing without Awaitility stabilisation and additional authority setup
research. Deferred to a future phase.

---

## Seeded Data Behavior

Each seeded payment order gets exactly one creation history entry via
`PaymentOrderStatusHistory.seededCreationEntry()` with `action = null`. Since the history
endpoint filters `action IS NOT NULL`, **all seeded orders return empty history** — even
`PAYMENT_ORDER_ALPHA_001_AUTHORIZED_ID` (which is already in AUTHORIZED state). There are no
seeded lifecycle action history rows.

This is intentional: the seed represents point-in-time state snapshots, not replay logs.
To get non-empty history, lifecycle operations must be performed via the API.

---

## Implementation

### New files

| File | Purpose |
|---|---|
| `api/payment/dto/PaymentHistoryResponse.java` | Test-side DTO (nested `StatusHistoryEntry` record); mirrors backend's `PaymentStatusHistoryResponse` |

### Modified files

| File | Change |
|---|---|
| `api/payment/PaymentOrdersApi.java` | Added `HISTORY_PATH` constant + `history(merchantId, paymentOrderId)` method |
| `scenarios/PaymentOrdersContractSpec.java` | 3 new Phase 7J tests (25 total payment order specs) |

---

## Test Coverage

### Test 1: Full lifecycle history (create → authorize → capture)

**Spec**: `history_after_lifecycle_contains_ordered_entries`

Setup: create payment order → authorize → capture → GET history.

Assertions:
- 200 with `Vary: Authorization`, `Cache-Control: no-store`
- `content.size() == 2` (AUTHORIZE + CAPTURE entries, creation excluded)
- `content[0]`: `action=AUTHORIZE`, `fromStatus=CREATED`, `toStatus=AUTHORIZED`, `paymentOrderId` matches, `correlationId` non-null
- `content[1]`: `action=CAPTURE`, `fromStatus=AUTHORIZED`, `toStatus=CAPTURED`, `paymentOrderId` matches, `correlationId` non-null

Identity: `seededMerchantCreator()` — has `merchant:payments:create|read|lifecycle` and `merchant_id` = MERCHANT_ALPHA_001_ID.

### Test 2: Empty history for newly created order

**Spec**: `history_for_newly_created_order_returns_empty_list`

Setup: create payment order (no lifecycle operations).

Assertions:
- 200
- `content.isEmpty()` — documents that creation entry is excluded by `action IS NOT NULL` filter

### Test 3: Forbidden access

**Spec**: `history_access_forbidden_without_required_authority`

Identity: `denied()` (valid JWT, no roles).

Assertions:
- 403 at Spring Security filter chain level (before controller reached)

Uses seeded `PAYMENT_ORDER_ALPHA_001_CREATED_ID` — no data setup required since auth check fires before any service logic.

---

## Compliance / Observability Notes

| Concern | Finding |
|---|---|
| Synchronous write | History is written within the same `@Transactional` as the lifecycle operation → no race between API response and history persistence |
| `correlationId` propagation | `CorrelationIdFilter` sets MDC `correlationId`; `PaymentLifecycleService` passes it to `PaymentOrderStatusHistory.lifecycleEntry()` → visible in history response |
| Creation entry design | Excluded from history endpoint by design — creation is represented by `GET` on the payment order itself |
| Seeded orders | Have creation history entry only; no lifecycle action entries; history returns `[]` for all seeded orders |

---

## Deferred: Audit Endpoint

The audit module exposes:
```
GET /api/audit  (AuditController)
```
This is driven by `AuditableActionOccurred` Spring Application Events + `AuditEventListener`.
The event listener runs in a separate transaction context, making the audit write
**asynchronous** relative to the API response. Testing it reliably requires:
1. Awaitility with `await().atMost()` polling until the event is processed
2. Authority research (which roles can query audit events)
3. Correlation between `X-Correlation-ID` from the original request and the audit event

Deferred to Phase 8+ when these prerequisites are researched and stabilised.

---

## Validation

```bash
# Offline compilation
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live integration tests
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 41 IT specs (1 status + 2 security smoke + 13 merchant + 25 payment order), BUILD SUCCESS
```
