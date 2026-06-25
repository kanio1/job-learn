# Phase 8A — Async Audit Event Contract with Awaitility

## Goal

Verify that payment lifecycle operations emit durable audit events visible through
`GET /api/audit` after the async write completes, and that the endpoint enforces
authorization correctly.

## Files Added / Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/AuditContractSpec.java` | New spec: 2 tests |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/audit/AuditApi.java` | New facade |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/audit/dto/AuditListResponse.java` | New DTO |
| `apps/api-tests/pom.xml` | Awaitility 4.3.0 dependency |

## Async Write Architecture

`PaymentLifecycleService.authorize()` publishes `AuditableActionOccurred` via Spring's
`ApplicationEventPublisher`. `AuditEventListener` is annotated with
`@ApplicationModuleListener` — a Spring Modulith meta-annotation combining
`@TransactionalEventListener(phase=AFTER_COMMIT)` and `@Async`. The event listener
runs in a separate thread and transaction AFTER the lifecycle operation's transaction
commits. The HTTP response is therefore returned BEFORE the audit event is persisted.

Consequence: there is a non-deterministic delay (typically <500 ms in test, up to a
few seconds under load) between the lifecycle HTTP response and the audit event being
visible via `GET /api/audit`.

## Why Awaitility, Not Thread.sleep

`Thread.sleep` introduces a fixed latency that is either too short (flaky) or too long
(slow). Awaitility polls at a short interval and succeeds as soon as the condition
holds, making the test both fast and stable. `atMost(15s)` is generous for a test JVM;
typical completion is under 1 second.

## Critical Discovery: Awaitility Thread Model

**Awaitility 4.x evaluates conditions in a dedicated background thread** named
`awaitility-thread`, NOT the calling (main) thread.

`Ctx` is a `ThreadLocal`. When `Ctx.set(TestContext.of(auditIdentity))` is called on
the main thread, the value is NOT visible on `awaitility-thread`. As a result,
`AuthFilter.currentOrNull()` returns null on the polling thread → no `Authorization`
header is added → Spring Security returns `401` for an unauthenticated request
(not `error=invalid_token` from a bad JWT).

**Symptom**: `WWW-Authenticate: Bearer resource_metadata="..."` without
`error="invalid_token"` — this is Spring Security 6.4+ / Spring Boot 4's format for a
missing (not invalid) token.

**Investigation path**: decoded the JWT for `tenantAdmin` inside the main thread — it had
correct `azp="payment-quality-dashboard"`, `iss` matching backend config, `tenant:audit:read`
in `realm_access.roles`. The JWT itself was valid; the header just wasn't being sent.

**Fix**: call `Ctx.set(TestContext.of(auditIdentity))` at the START of every Awaitility
condition lambda. This establishes the identity on the polling thread before `AuditApi.list()`
triggers `AuthFilter`.

```java
Identity auditIdentity = Identities.tenantAdmin("TENANT_ALPHA");
Ctx.set(TestContext.of(auditIdentity));  // for post-await assertions on main thread

await()
    .atMost(Duration.ofSeconds(15))
    .pollDelay(Duration.ofMillis(200))
    .pollInterval(Duration.ofMillis(500))
    .until(() -> {
        Ctx.set(TestContext.of(auditIdentity));  // establish Ctx on awaitility-thread
        var r = AuditApi.list("PAYMENT_AUTHORIZED", "PAYMENT_ORDER");
        ...
    });
```

This pattern must be used in ALL Awaitility conditions that call REST Assured via
`RequestSpecs.base()` (which includes `AuthFilter`).

## Tests Added

### `authorize_emits_payment_authorized_audit_event`

Full async event contract chain:
1. Create payment order (seededMerchantCreator → 201)
2. Authorize payment order (seededMerchantCreator → 200), capture `X-Correlation-ID`
3. Switch to `tenantAdmin("TENANT_ALPHA")` (has `tenant:audit:read`)
4. Awaitility polls `GET /api/audit?action=PAYMENT_AUTHORIZED&target_type=PAYMENT_ORDER` until
   an entry with `targetId == paymentOrderId` appears
5. Assert stable fields: `action`, `targetType`, `targetId`, `correlationId` (matches step 2),
   `occurredAt`, `outcome=SUCCESS`, `id`, `actorDisplay`

Key assertion: `correlationId` in the audit event equals the `X-Correlation-ID` header
from the authorize response. This verifies the tracing ID survives the async hop across
transaction and thread boundaries — critical for PCI-DSS / PSD2 audit trail completeness.

### `audit_list_returns_403_for_denied_user`

Authorization boundary test: `denied()` (no roles, valid JWT) calls `GET /api/audit` →
`@PreAuthorize("hasAnyAuthority('platform:audit:read', 'tenant:audit:read')")` fires →
`AccessDeniedException` → `AuditExceptionHandler.handleForbidden()` → 403.

Confirms 401 vs 403 semantics: the user IS authenticated (JWT validates) but is NOT
authorized. 401 would be incorrect here.

## Audit Event Fields

| Field | Source |
|-------|--------|
| `id` | Auto-generated UUID |
| `action` | `"PAYMENT_AUTHORIZED"` (from `publishSuccess("PAYMENT_AUTHORIZED", ...)`) |
| `targetType` | `"PAYMENT_ORDER"` |
| `targetId` | `paymentOrderId.toString()` |
| `tenantId` | JWT `tenant_id` claim of the actor (`"TENANT_ALPHA"`) |
| `correlationId` | MDC `correlationId` set by `CorrelationIdFilter`, propagated via `AuditableActionEventFactory` |
| `occurredAt` | `Instant.now()` at event publication time |
| `outcome` | `"SUCCESS"` |
| `actorDisplay` | JWT `preferred_username` claim |

## Authorization Model

- `platform:audit:read` — sees all events (platform-scoped, no tenant filter)
- `tenant:audit:read` — sees only events with matching `tenantId` (scoped to JWT `tenant_id`)
- No audit authority → 403 from `@PreAuthorize` (handled by `AuditExceptionHandler`)

## Isolation

Audit events are NOT cleared by `SeedApi.reset()` (which only clears payments/merchants/tenants).
Tests isolate their events by filtering on `targetId == paymentOrderId.toString()` after
querying by `action + target_type`. This is safe within a single `mvn verify` run (fresh container).

## SDET Interview Topics

- What is the difference between `@TransactionalEventListener` and `@EventListener`? Why does the
  former guarantee the event is not fired on rollback?
- Why does `@ApplicationModuleListener` combine `@Async` with `@TransactionalEventListener(AFTER_COMMIT)`?
- Why does the audit event carry the same `correlationId` as the HTTP response? What breaks in
  incident response if it does not?
- Why does payment CREATE not emit an audit event but AUTHORIZE does? (Compliance risk consideration.)
- Why is testing asynchronous behavior harder than synchronous? What can go wrong with `Thread.sleep`?
- What is the difference between HTTP 401 and 403? Which one does a valid JWT with insufficient
  authority produce, and which one does a missing/invalid JWT produce?
- Why does Awaitility's condition lambda need its own `Ctx.set()` call when `Ctx` is a ThreadLocal?

## Test Results

- **79 offline tests**: all pass (unchanged)
- **43 live tests**: all pass (2 new in this phase)
