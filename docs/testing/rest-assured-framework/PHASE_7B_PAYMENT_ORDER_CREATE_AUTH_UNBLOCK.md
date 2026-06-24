# Phase 7B — Payment Order Create Auth Unblock

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 25 live specs
> (1 status + 2 security smoke + 13 merchant contract + 9 payment order contract).

---

## Summary

Phase 7B unblocks payment order create (`POST /api/merchants/{merchantId}/payment-orders`)
by resolving a Keycloak realm gap identified in Phase 7A. A new test-only Keycloak user
(`merchant.alpha.creator`) is added to the test realm JSON with the exact UUID of seeded
MERCHANT_ALPHA_001 as its `merchant_id` attribute. This satisfies the backend controller's
merchant-scope check, enabling 3 new live create contract tests.

---

## The Realm Gap (Phase 7A blocker)

The backend controller (`PaymentOrderController`) checks this before authority evaluation:

```java
String merchantIdClaim = jwt.getClaimAsString("merchant_id");
if (merchantIdClaim == null || !merchantId.toString().equals(merchantIdClaim)) {
    throw new AccessDeniedException("Merchant scope mismatch");
}
```

`merchantId.toString()` produces a UUID string, e.g. `"00000000-0000-0000-0000-0000000000b1"`.

**Why existing users could not satisfy this:**

| User | Enabled | `merchant_id` attribute | Verdict |
|---|---|---|---|
| `merchant.payment.lifecycle` | ✅ | `"PLACEHOLDER_MERCHANT_ID"` | Not a UUID — string compare always fails |
| `merchant.payment.creator` | ❌ | `"PLACEHOLDER_MERCHANT_ID"` | Disabled AND not a UUID |
| `merchant.manager` | ✅ | `"MERCHANT_ALPHA_001"` | String ref, not UUID — compare fails |

---

## Resolution

Added a new test-only user to `apps/api-tests/src/test/resources/keycloak/payment-quality-realm.json`:

```json
{
  "username": "merchant.alpha.creator",
  "enabled": true,
  "credentials": [{"type": "password", "value": "merchant.alpha.creator", "temporary": false}],
  "attributes": {
    "merchant_id": "00000000-0000-0000-0000-0000000000b1",
    "tenant_id": "TENANT_ALPHA"
  },
  "realmRoles": ["MERCHANT_MANAGER"]
}
```

The `merchant_id` attribute value is the exact seeded UUID of MERCHANT_ALPHA_001 (ACTIVE status).
`MERCHANT_MANAGER` is a composite role that expands to:
- `merchant:payments:create` — required authority for POST
- `merchant:payments:read` — for read access as the same persona
- `merchant:payments:lifecycle` — for future lifecycle tests with this persona

The Keycloak protocol mapper `merchant-id-mapper` maps the `merchant_id` user attribute
to the `merchant_id` JWT claim. The backend then compares this claim with the path UUID.

**Why a new user instead of fixing `merchant.manager`?**
`merchant.manager` already has `merchant_id = "MERCHANT_ALPHA_001"` (a human-readable label,
not a UUID). Changing it would silently alter the behavior of any test using `merchantManager()`.
A separate user scoped explicitly to Phase 7B is self-documenting and carries no cross-test risk.

---

## Files Changed

| File | Change |
|---|---|
| `keycloak/payment-quality-realm.json` | Added `merchant.alpha.creator` user |
| `core/auth/Identities.java` | Added `seededMerchantCreator()` persona |
| `api/payment/dto/CreatePaymentOrderRequest.java` | New test-side request DTO |
| `api/payment/PaymentOrdersApi.java` | Implemented `create()` + added `createWithoutIdempotencyKey()` |
| `scenarios/PaymentOrdersContractSpec.java` | Added 3 create tests (9 total payment order specs) |

---

## New API Facade Methods

### `PaymentOrdersApi.create()`

```java
public static Response create(
        String merchantId, CreatePaymentOrderRequest requestBody, String idempotencyKey) {
    return RequestSpecs.idempotent(idempotencyKey)
            .contentType(ContentTypes.JSON)
            .pathParam("merchantId", merchantId)
            .body(requestBody)
            .when()
            .post(LIST_PATH);
}
```

`RequestSpecs.idempotent(key)` builds `base().header(Headers.IDEMPOTENCY_KEY, key)`.
This always includes the idempotency key — the method cannot be called without one.

### `PaymentOrdersApi.createWithoutIdempotencyKey()`

```java
public static Response createWithoutIdempotencyKey(
        String merchantId, CreatePaymentOrderRequest requestBody) {
    return RequestSpecs.base()
            .contentType(ContentTypes.JSON)
            .pathParam("merchantId", merchantId)
            .body(requestBody)
            .when()
            .post(LIST_PATH);
}
```

A negative-test variant that intentionally omits the required header. Exists at the facade layer
so scenario code never calls `given()/when()` directly — the facade owns the "how."

---

## New Persona

### `Identities.seededMerchantCreator()`

```java
public static Identity seededMerchantCreator() {
    return Identity.of("seeded-merchant-alpha-001-creator",
            List.of("MERCHANT_MANAGER"),
            null,
            keycloakOrPlaceholder("merchant.alpha.creator", "merchant.alpha.creator"));
}
```

Pinned to MERCHANT_ALPHA_001. Returns 403 if used with any other merchant UUID.

---

## Tests Added (Phase 7B)

| Test | Contract verified | HTTP/SDET concept |
|---|---|---|
| `create_payment_order_returns_201_with_body_and_headers` | POST → 201 + Location + ETag "v0" + body fields | 201 vs 200; Location header; initial ETag |
| `create_with_mismatched_merchant_scope_returns_403` | JWT merchant_id ≠ path UUID → 403 | Controller-level scope check; role ≠ scope |
| `create_without_idempotency_key_returns_400` | Missing Idempotency-Key → 400 validation | Required header validation; same error shape as bean validation |

---

## HTTP/REST Concepts — Create Contract

### 201 vs 200

`POST` to a collection creates a new resource → `201 Created`. The response includes a
`Location` header pointing to the new resource's canonical URL. `200 OK` would be wrong
because the resource is new, not updated.

### ETag: "v0" on creation

The JPA `@Version` counter starts at `0` for new entities. The backend serializes ETag as
`"vN"` (quoted). A freshly created payment order has version 0, so `ETag: "v0"`.
Subsequent lifecycle operations (authorize, capture) increment the version.

### Vary: Authorization, Idempotency-Key

On create, the `Vary` header includes both `Authorization` and `Idempotency-Key` because:
- Same JWT but different idempotency key → different (new) resource
- Same idempotency key but different JWT → should not serve cached response

### Idempotency-Key required (not optional)

The backend enforces idempotency key as a required header on create. This prevents accidental
double-submission without a key (which would create two identical orders). Missing → 400.

### merchant_id scope check vs. authority check

Two separate gates must both pass to create a payment order:
1. **Authority gate** (Spring Security `@PreAuthorize`): caller must have `merchant:payments:create`
2. **Scope gate** (controller code): JWT `merchant_id` claim must equal the path UUID

A caller can have the right authority but the wrong scope — that's the 403 scenario tested here.
This is a common pattern in multi-tenant payment APIs: the role says "you can create orders"
but the claim scopes it to a specific merchant.

---

## `CreatePaymentOrderRequest` DTO

```java
public record CreatePaymentOrderRequest(
        Long amountMinor, String currency, String clientOrderReference) {

    public static CreatePaymentOrderRequest valid(
            long amountMinor, String currency, String clientOrderReference) { ... }

    public static CreatePaymentOrderRequest withBlankCurrency(
            long amountMinor, String clientOrderReference) { ... }
}
```

Factory method convention: `valid(...)` builds a valid request; `withBlankCurrency(...)` builds
an invalid one for 400 testing. Adding more factories here is the correct place to grow validation
coverage — not inline in scenario methods.

---

## Validation

```bash
# Offline (unit tests — no containers)
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live (requires Docker image)
cd apps/api-tests
BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 25 IT specs (1 status + 2 security smoke + 13 merchant + 9 payment order), BUILD SUCCESS
```

---

## Deferred to Phase 7C+

| Item | Reason |
|---|---|
| Idempotency replay (same key → 200 with same body) | Stable; needs a newly created order from this session |
| Idempotency conflict (same key, different body → 409) | Stable; needs two creates |
| `HEAD /api/merchants/{id}/payment-orders/{id}` — ETag-only response | Low complexity; unblocked now |
| LIST with filters (`status`, `currency`, `fromDate`, etc.) | Stable; add incrementally |
| `GET /summary` | Needs several payment orders in various states |
| PATCH (metadata update, requires If-Match) | Needs created order + ETag |
| Authorize/capture/cancel/refund lifecycle | Requires If-Match + Idempotency-Key; needs created order in right state |
| `GET /history` | Requires lifecycle actions first |
| `merchant:payments:read` scope (merchant-scoped reads instead of platform reader) | Now unblocked — `seededMerchantCreator` also has `merchant:payments:read` |
