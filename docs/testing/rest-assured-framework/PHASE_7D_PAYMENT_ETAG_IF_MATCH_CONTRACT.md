# Phase 7D — Payment Order ETag / If-Match Lifecycle Foundation

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 30 live specs
> (1 status + 2 security smoke + 13 merchant contract + 14 payment order contract).

---

## Summary

Phase 7D adds black-box contract tests for the payment order lifecycle's conditional-request
model. The minimum viable lifecycle chain is: create → authorize with If-Match. Three new
live specs cover the happy path (correct If-Match → 200, ETag incremented), the missing
precondition (no If-Match → 428), and the stale precondition (wrong version → 412).
No new Keycloak user was required — `merchant.alpha.creator` (MERCHANT_MANAGER composite role)
already carries `merchant:payments:lifecycle`, which is the required authority for all lifecycle
endpoints.

---

## Backend Lifecycle Contract (Discovered)

### Authorize endpoint

```
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize
Content-Type:    application/json
If-Match:        "v{N}"              (required — functionally; declared optional at MVC layer)
Idempotency-Key: <key>               (required — MissingRequestHeaderException if absent)
Body:            {} or AuthorizeRequest(reason)   (required = false; reason is optional)
```

**Authority gate** (SecurityConfig): `merchant:payments:lifecycle` or
`platform:payments:lifecycle`.

**Scope gate** (`verifyMerchantOwnership`): JWT `merchant_id` claim must equal path UUID
for merchant-scoped users; platform-lifecycle callers bypass this check.

`merchant.alpha.creator` Keycloak user has:
- `MERCHANT_MANAGER` composite role → expands to `merchant:payments:lifecycle` ✓
- `merchant_id = "00000000-0000-0000-0000-0000000000b1"` (MERCHANT_ALPHA_001 UUID) ✓

### If-Match handling

`@RequestHeader(value = "If-Match", required = false)` in Spring MVC: Spring does not
reject a missing header at binding time. Instead, `PaymentEtag.requireVersion(ifMatch)`
enforces it programmatically:

```java
public static long requireVersion(String ifMatch) {
    if (ifMatch == null || ifMatch.isBlank()) {
        throw new PaymentPreconditionRequiredException("If-Match header is required");
    }
    // parse "vN" pattern; reject malformed formats → MalformedPaymentEtagException
    ...
}
```

Then `PaymentVersionPrecondition.requireCurrentVersion(order, expectedVersion)` compares the
parsed version against the JPA `@Version` field:

```java
if (currentVersion == null || currentVersion != expectedVersion) {
    throw new PaymentOrderVersionMismatchException();
}
```

### ETag format and increment

ETag is `"vN"` (quoted) where N is the JPA `@Version` counter.

| State transition | JPA version | ETag |
|---|---|---|
| Create (POST) | 0 | `"v0"` |
| Authorize | 1 | `"v1"` |
| Capture | 2 | `"v2"` |
| Cancel | 3+ | `"v{N}"` |

Each lifecycle action increments the version exactly once.

### Response contract — authorize success

| Header | Observed value |
|---|---|
| HTTP status | 200 OK |
| `ETag` | `"v1"` (N+1 of pre-authorize version) |
| `Vary` | `If-Match` (see observation below) |
| `Cache-Control` | `no-store` |
| `X-Correlation-ID` | propagated from request |
| `Content-Type` | `application/json` |

Response body fields: same JSON shape as `PaymentOrderResponse` (compatible DTO reuse).
Status field is `"AUTHORIZED"`; `authorizedAt` is non-null.

### Vary header observation

The backend sets `VARY_AUTHORIZATION_IF_MATCH = "Authorization, If-Match"` via `.varyBy()`,
but the observed `Vary` header on lifecycle responses is `If-Match` only (not the combined
form). This is consistent with the Phase 7C observation on create responses. Tests assert
`containsIgnoringCase("If-Match")` to match actual behavior.

---

## Exception → HTTP mapping

| Exception | Status | Error code |
|---|---|---|
| `PaymentPreconditionRequiredException` | 428 | `precondition_required` |
| `PaymentOrderVersionMismatchException` | 412 | `payment_order_version_mismatch` |
| `MalformedPaymentEtagException` | 400 | `malformed_if_match` |
| `InvalidStateTransitionException` | 422 | `invalid_transition` |
| `IdempotencyConflictException` | 409 | `idempotency_conflict` |
| `OptimisticLockingFailureException` | 412 | `concurrency_conflict` |

Error responses on lifecycle paths use `Vary: Authorization, If-Match` (observed: `If-Match`)
and `Cache-Control: no-store`.

---

## Tests Added (Phase 7D)

| Test method | Contract verified | HTTP/SDET concept |
|---|---|---|
| `authorize_with_correct_if_match_returns_200_and_increments_etag` | Correct If-Match → 200, status AUTHORIZED, ETag `"v0"` → `"v1"` | Optimistic locking; ETag increment; conditional request round-trip |
| `authorize_without_if_match_returns_428` | Missing If-Match → 428 `precondition_required` | RFC 6585 428 vs 412 vs 400; required = false at MVC layer vs functional requirement |
| `authorize_with_stale_if_match_returns_412` | Stale `"v1"` on `v0` order → 412 `payment_order_version_mismatch` | Stale ETag detection; 412 vs 428 distinction; lost update prevention |

---

## Files Changed

| File | Change |
|---|---|
| `core/problem/ProblemCodes.java` | Added `PRECONDITION_REQUIRED` and `PAYMENT_ORDER_VERSION_MISMATCH` |
| `api/payment/PaymentOrdersApi.java` | Added `AUTHORIZE_PATH`, `authorize()`, `authorizeWithoutIfMatch()` |
| `scenarios/PaymentOrdersContractSpec.java` | Added 3 lifecycle tests (14 total payment order specs) |
| `docs/.../PHASE_7D_PAYMENT_ETAG_IF_MATCH_CONTRACT.md` | This document |

---

## New API Facade Methods

### `PaymentOrdersApi.authorize()`

```java
public static Response authorize(
        String merchantId, String paymentOrderId, String ifMatch, String idempotencyKey) {
    return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
            .contentType(ContentTypes.JSON)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body("{}")
            .when()
            .post(AUTHORIZE_PATH);
}
```

`RequestSpecs.lifecycle(ifMatch, key)` adds both `If-Match` and `Idempotency-Key` to the
base authenticated spec. Body is `{}` — `AuthorizeRequest.reason` is optional and not
needed for contract foundation tests.

### `PaymentOrdersApi.authorizeWithoutIfMatch()`

```java
public static Response authorizeWithoutIfMatch(
        String merchantId, String paymentOrderId, String idempotencyKey) {
    return RequestSpecs.idempotent(idempotencyKey)  // adds Idempotency-Key, omits If-Match
            .contentType(ContentTypes.JSON)
            ...
}
```

Negative-test variant: `RequestSpecs.idempotent()` adds `Idempotency-Key` but not `If-Match`.
The facade owns "how to call the API incorrectly" so scenarios express pure business intent.

---

## New Problem Codes

| Constant | Value | Status | When |
|---|---|---|---|
| `PRECONDITION_REQUIRED` | `"precondition_required"` | 428 | If-Match absent on lifecycle endpoint |
| `PAYMENT_ORDER_VERSION_MISMATCH` | `"payment_order_version_mismatch"` | 412 | If-Match version ≠ current JPA version |

---

## Persona Reuse — No New Keycloak User Needed

`seededMerchantCreator()` (`merchant.alpha.creator`) has `MERCHANT_MANAGER` which expands to
`merchant:payments:create`, `merchant:payments:read`, and `merchant:payments:lifecycle`.
All three lifecycle tests use this persona for both the setup create and the authorize call.

---

## HTTP/REST Concepts Exercised

### Why 428 and not 400 for missing If-Match?

400 means the request is syntactically malformed (e.g., invalid JSON). A missing `If-Match`
is structurally valid — the JSON body and path are fine. The problem is semantic: this operation
category requires a precondition header. HTTP 428 (RFC 6585) was designed for exactly this:
"the server requires the request to be conditional." 400 would wrongly signal a syntax error;
428 tells the client "re-read the resource, get its ETag, and resend."

### Why 412 for stale If-Match and not 409?

409 Conflict means the request conflicts with the server's current state (e.g., duplicate
idempotency key). 412 Precondition Failed means a conditional header evaluated to false.
These are distinct: 412 targets the `If-Match` / `If-None-Match` conditional semantics; 409
targets business state conflicts. An ETag mismatch is precisely a 412 case.

### Why optimistic locking instead of pessimistic locking?

A distributed payment API cannot hold row locks across HTTP requests — a locked row would
block all concurrent readers while a client is "thinking." Optimistic locking reads freely,
detects conflict at write time with a version check, and lets the client retry with fresh
data. The tradeoff: the client must implement retry logic on 412. For payment lifecycle,
this is acceptable because state transitions are rare relative to reads.

---

## Deferred to Phase 7E+

| Item | Reason |
|---|---|
| Capture, cancel, refund lifecycle contract | Same If-Match pattern; add incrementally |
| Lifecycle idempotency replay (same key → 200, same order state) | Requires testing the same lifecycle action twice |
| Lifecycle idempotency conflict (same key + different body) | Same pattern as create conflict |
| Malformed If-Match → 400 `malformed_if_match` | Low priority; backend unit tests cover parsing |
| Invalid state transition (AUTHORIZED → authorize again) → 422 `invalid_transition` | Requires ordering two lifecycle actions |
| `PATCH /api/merchants/{merchantId}/payment-orders/{id}` — metadata update with If-Match | Merge-patch contract; needs `RequestSpecs.mergePatch()` |
| `HEAD /api/merchants/{merchantId}/payment-orders/{id}` — ETag-only response | HEAD method semantics |
| Lifecycle with missing Idempotency-Key → 400 `missing_required_header` | Low priority |

---

## Validation

```bash
# Offline (unit tests — no containers)
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live (requires Docker image)
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 30 IT specs (1 status + 2 security smoke + 13 merchant + 14 payment order), BUILD SUCCESS
```
