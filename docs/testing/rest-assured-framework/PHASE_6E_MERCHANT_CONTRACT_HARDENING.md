# Phase 6E — Merchant Contract Hardening

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 16 live specs
> (1 status + 2 security smoke + 13 merchant contract).

---

## Summary

Phase 6E hardens the merchant API contract coverage started in Phase 6D. It adds
lifecycle transition tests (activate, suspend, invalid_transition), a tenant-filter
list test, a missing-tenantReference validation test, and refactors all 5 existing
error-path tests to use the `ProblemAssert` fluent API for richer failure diagnostics.

Two new `ProblemCodes` constants are added, and `MerchantsApi` gains `activate()` and
`suspend()` facade methods.

---

## Changes

### `ProblemCodes` additions

| Constant | Value | HTTP status |
|---|---|---|
| `DUPLICATE_MERCHANT_REFERENCE` | `"duplicate_merchant_reference"` | 409 |
| `INVALID_TRANSITION` | `"invalid_transition"` | 409 |

Both codes were already emitted by `MerchantExceptionHandler` and asserted via string
literals in Phase 6D. Promoting them to constants makes the contract stable and
grep-findable.

### `MerchantsApi` additions

| Method | Endpoint | Authority |
|---|---|---|
| `activate(String merchantId)` | `POST /api/merchants/{id}/activate` | `platform:merchants:update-status` |
| `suspend(String merchantId)` | `POST /api/merchants/{id}/suspend` | `platform:merchants:update-status` |

Both methods follow the existing return-type strategy: `Response` — the scenario
inspects status before choosing the assertion path.

---

## Tests Added / Refactored

### New tests (Phase 6E)

| Test | Contract verified | HTTP/SDET concept |
|---|---|---|
| `create_without_tenant_ref_for_platform_caller_returns_400` | `MissingTenantReferenceException` → 400 | Conditional service-layer validation vs bean validation; platform vs tenant-scoped paths |
| `list_with_tenant_filter_returns_merchants` | `GET ?tenantId=TENANT_ALPHA` → 200 non-empty | Optional query-parameter filtering; controller conditional path for platform-scoped callers |
| `activate_draft_merchant_returns_200_with_active_status` | DRAFT → ACTIVE → 200 | 200 (not 201) for state transitions; domain state machine via black-box HTTP |
| `suspend_active_merchant_returns_200_with_suspended_status` | DRAFT → ACTIVE → SUSPENDED → 200 | Full lifecycle chain; each test owns its merchant from creation |
| `activate_already_active_merchant_returns_409_invalid_transition` | ACTIVE → ACTIVE → 409 | `invalid_transition` vs `duplicate_merchant_reference` error discriminator; 409 for state conflict vs 400 for syntax error |

### Refactored tests (Phase 6D → 6E)

All five error-path tests were refactored from Hamcrest matchers chained on
`ValidatableResponse` to `ProblemAssert` — preserving the same contract while
adding:

- Full response body in failure messages (fast diagnosis in CI).
- Explicit `hasStatus()` verification separate from the body assertion.
- `hasContentTypeProblemJson()` on the 404 test — the only handler that explicitly
  sets `application/problem+json` (`handleNotFound`). The other handlers do not set
  the content type explicitly, so that assertion is intentionally omitted for 400/409.

---

## Key Decisions

### Why 200 for activate/suspend?

`POST /api/merchants/{id}/activate` mutates an existing resource, not creates a new
one — so 201 is wrong. The response body is the post-transition merchant resource.
This is the standard "command endpoint" pattern in REST: a verb URL returns 200 with
the updated state.

### Why each lifecycle test creates its own merchant?

Sharing a merchant across tests couples test execution order. If `activate` test
runs before `invalid_transition` test, the shared merchant is already ACTIVE.
If they share a DRAFT merchant and one activates it first, the other sees ACTIVE and
its preconditions fail. Creating a unique merchant per test eliminates this coupling
at the cost of a few extra POST calls — a worthwhile trade.

### Why not test DRAFT → SUSPEND or SUSPENDED → ACTIVE?

Those are valid candidates for Phase 7 "merchant lifecycle boundary" tests. They
require no new API methods and are stable. Deferring keeps Phase 6E focused on the
happy-path lifecycle chain and the most common invalid transition (ACTIVE → ACTIVE).

### Tenant filter: unknown tenant returns 200 + empty list

The controller deliberately catches `TenantResolutionException` when filtering and
returns an empty list rather than 404. This avoids leaking tenant existence to
platform-scoped callers. Not asserted as a new test here (not high-value enough for
Phase 6E scope) but documented for future reference.

---

## Lifecycle State Machine (confirmed from backend)

```
DRAFT ──activate()──▶ ACTIVE ──suspend()──▶ SUSPENDED
  │                     │                      │
  │     invalid         │     invalid           │  all transitions invalid
  └────────────────────▶│◀──────────────────────┘
         (409)                (409)
```

Valid transitions: `DRAFT → ACTIVE`, `ACTIVE → SUSPENDED`.
All other transitions throw `InvalidTransitionException` → 409 `invalid_transition`.

---

## Authority Used

`PLATFORM_ADMIN` composite role (assigned to Keycloak user `platform.admin`) expands
via `KeycloakRealmRoleConverter` to include `platform:merchants:update-status`, which
is the authority required by the activate and suspend controller endpoints.

---

## Validation

```bash
# Offline (unit tests — no containers)
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live (requires Docker image)
cd apps/api-tests
BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 16 IT specs (1 status + 2 security smoke + 13 merchant contract), BUILD SUCCESS
```

---

## Deferred to Phase 7+

| Item | Reason |
|---|---|
| Invalid lifecycle transitions: DRAFT→SUSPENDED, SUSPENDED→ACTIVE | Valid candidates; deferred to keep 6E focused |
| Tenant isolation contract (tenant.admin sees only TENANT_ALPHA merchants) | Requires tenant-scoped persona setup; Phase 7 |
| `GET /api/merchants?tenantId=UNKNOWN` returns 200 empty list | Low priority; controller behavior documented above |
| `PaymentOrdersApi` | Separate phase per framework plan |
