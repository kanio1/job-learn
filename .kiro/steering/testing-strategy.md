# Testing Strategy — Payment Quality Engineering Lab

## Core Principle

Tests are not decorations. Every test must answer: *what behavior does this protect, and what risk does it cover?*

Do not hide important HTTP behavior behind excessive abstraction. The test should be readable enough to teach the contract it verifies.

---

## Test Layer Hierarchy

```
E2E (Playwright)          — user journeys through the real UI + proxy + backend
  ↑
REST Integration (REST Assured + Testcontainers)
  — full HTTP round-trip: controller → service → DB → response headers + body
  ↑
Module / Slice tests (Spring Modulith + @WebMvcTest + @SpringBootTest)
  — module contracts, web layer, security, validation
  ↑
Unit tests (JUnit 6 + Mockito + AssertJ)
  — domain logic, value objects, pure functions
```

Choose the **narrowest layer** that proves the behavior. Do not write a full integration test for a pure-function concern.

---

## Backend — Java Test Conventions

### Naming and location

| Test type | Location | Suffix |
|---|---|---|
| Unit tests | alongside source in `src/test/java/.../` | `*Test.java` |
| Integration tests | same, but Failsafe-picked | `*IT.java` |
| REST Assured tests | `apps/backend/src/test/java/lab/paymentquality/rest/` | `*IT.java` or `*Test.java` |
| Security tests | `apps/backend/src/test/java/lab/paymentquality/security/` | import `TestJwtConfiguration` |
| Architecture tests | top-level test package | `ModulithArchitectureTest`, `MerchantModuleTest`, `PaymentModuleTest` |

- `./mvnw test` — runs `*Test.java` via Surefire.
- `./mvnw verify` — runs `*IT.java` via Failsafe (includes Testcontainers).
- All Spring-context tests use `@ActiveProfiles("test")`.
- DB-dependent tests extend `PostgresContainerSupport`; Flyway owns the schema.
- Ignore learner copies (`My*`, `Lesson*`) unless the task explicitly concerns them.

### RestKit conventions

RestKit is a **thin REST Assured client layer** — not a framework. Build it incrementally.

- Location: `apps/backend/src/test/java/lab/paymentquality/rest/`
- One client class per domain: `MerchantRestClient`, `PaymentOrderRestClient`, `PaymentLifecycleRestClient`.
- Client methods are business-readable: `createPaymentOrder(...)`, `authorizePaymentOrder(...)`, `getPaymentOrder(...)`.
- Hide `RequestSpecification` and raw `given().when().then()` inside private helpers.
- Do **not** add a generic `call(...)` method that hides the HTTP verb and path — those are the contract.
- Extract a helper only after the same boilerplate appears in 2+ tests.
- Prefer `TypeRef<List<T>>` for generic list extraction.
- For negative tests, use `Map<String, Object>` builders to craft deliberately malformed or missing-field payloads.

### REST Assured assertion conventions

Every REST Assured test must verify:
1. **Status code** — exact, not just `2xx`.
2. **Response body** — at least the key business fields.
3. **Headers** — `ETag`, `Location`, `Vary`, `Cache-Control`, `X-Correlation-ID` where the contract requires them.
4. **Database state** — for write operations, verify via a follow-up GET or direct repo assertion.

```java
// Good — explicit, contract-driven
given()
    .spec(baseSpec)
    .header("Idempotency-Key", idempotencyKey)
    .body(createRequest)
.when()
    .post("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .statusCode(201)
    .header("Location", containsString("/payment-orders/"))
    .header("ETag", notNullValue())
    .header("Vary", containsString("Idempotency-Key"))
    .body("status", equalTo("CREATED"))
    .body("amountMinor", equalTo(1000));
```

### ProblemDetails assertions

Use dedicated assertion helpers — do not scatter inline `body("status", ...)` checks across tests.

```java
// Good — named helper makes the contract visible
ProblemDetailsAssertions.assertPreconditionRequired(response);
ProblemDetailsAssertions.assertIdempotencyConflict(response);
ProblemDetailsAssertions.assertStalePrecondition(response);
```

`ProblemDetailsAssertions` checks:
- `Content-Type: application/problem+json`
- `status` matches the HTTP status code
- `title` is non-blank
- `detail` is non-blank
- `type` is non-blank (may be `about:blank`)

### Header assertions

Use a `HeaderAssertions` helper for repeated header checks:

```java
HeaderAssertions.assertEtagPresent(response);
HeaderAssertions.assertVaryContains(response, "Authorization", "If-Match");
HeaderAssertions.assertCacheControlNoStore(response);
HeaderAssertions.assertCorrelationIdPresent(response);
```

### ETag / If-Match test pattern

```java
// 1. Create → capture ETag
String etag = createPaymentOrder(...).header("ETag");

// 2. Authorize with correct ETag → success, new ETag
String newEtag = authorizePaymentOrder(orderId, etag).header("ETag");
assertThat(newEtag).isNotEqualTo(etag);

// 3. Stale ETag → 412
authorizePaymentOrder(orderId, etag)  // old etag
    .statusCode(412);

// 4. Missing If-Match → 428
authorizePaymentOrderWithoutIfMatch(orderId)
    .statusCode(428);
```

### Idempotency test pattern

```java
String key = UUID.randomUUID().toString();

// Same key + same payload → 200 replay
Response r1 = createOrder(key, payload).statusCode(201);
Response r2 = createOrder(key, payload).statusCode(200);
assertThat(r2.body().asString()).isEqualTo(r1.body().asString());

// Same key + different payload → 409
createOrder(key, differentPayload).statusCode(409);
```

### AssertJ conventions

- Prefer `assertThat(actual).isEqualTo(expected)` when `equals` is meaningful.
- Use `extracting(...)` for collections of DTOs.
- Use `containsExactly` when order matters; `containsExactlyInAnyOrder` when it does not.
- Create domain-specific assertion helpers when the same assertion pattern appears in 3+ tests.
- Never assert only on a field that does not reflect the behavior under test.

### Security test conventions

- Use `TestJwtConfiguration` to mint JWTs with specific roles, `merchant_id` claims, and expiry.
- Every protected endpoint needs: valid token (success), missing token (401), wrong role (403), wrong merchant scope (403 or masked 404).
- Do not hardcode JWT values — use the factory.

### Spring Modulith architecture tests

`ModulithArchitectureTest` verifies no cross-module internal package imports.
`MerchantModuleTest` and `PaymentModuleTest` verify module-level integration behavior.
Run these with `./mvnw verify` — they require the Spring context.

---

## Frontend — Playwright Conventions

### Location and setup

```
apps/frontend/tests/
├── .auth/platform-operator.json   # Auth storage state (generated by setup)
├── auth/auth.setup.ts             # OIDC login flow → saves storage state
└── e2e/                           # Feature specs
```

### Auth setup

Use the existing `auth.setup.ts` + storage state pattern. Do not re-authenticate in each spec. Load `tests/.auth/platform-operator.json` as the browser context state.

### Locator strategy — priority order

1. `data-testid` attribute (most stable, required for key elements — see `frontend-nuxt-ui.md`)
2. ARIA role + accessible name (`getByRole('button', { name: 'Activate' })`)
3. Visible text (`getByText(...)`) for non-interactive content
4. CSS selector or nth-child — only as a last resort, never for stable automation

Never use auto-generated class names, positional CSS selectors, or selectors based on visual styling.

### Route mocking (`page.route(...)`)

Use `page.route(...)` to mock `server/api/**` responses in unit-style E2E tests. This exercises the proxy path and keeps tests deterministic.

```ts
await page.route('/api/merchants/*/payment-orders/*', async route => {
  await route.fulfill({
    status: 200,
    headers: { 'ETag': '"5"', 'X-Correlation-ID': 'test-corr-123' },
    body: JSON.stringify(mockPaymentOrder),
  })
})
```

### Header capture tests (highest priority gap)

The core gap in the current test suite: no spec asserts that forwarded response headers (`ETag`, `Vary`, `X-Correlation-ID`) are visible in the UI. These must be added when the `HeaderKeyValuePanel` is implemented.

```ts
// Assert the HTTP headers panel shows the forwarded ETag
await expect(page.getByTestId('http-headers-panel')).toContainText('"5"')
await expect(page.getByTestId('http-headers-panel')).toContainText('test-corr-123')
```

### Error Lab tests

For each supported error (400/401/403/404/406/409/412/415/428):
1. Click the trigger button (`data-testid="error-lab-trigger-{status}"`).
2. Assert `HttpStatusBadge` shows the correct status code.
3. Assert `ProblemDetailsCard` (`data-testid="problem-details-card"`) is visible and contains the `detail` field.
4. Assert `Authorization` header value is masked (does not contain a real token).

### Lifecycle tests

```ts
// Authorize with If-Match
// 1. Load detail page → capture ETag from etag-display
// 2. Click lifecycle-authorize
// 3. Verify IfMatchInput is pre-filled with the captured ETag
// 4. Confirm → assert new ETag shown and status = AUTHORIZED

// Stale If-Match
// 1. Load detail page
// 2. Click lifecycle-authorize
// 3. Manually change If-Match to a stale value
// 4. Confirm → assert ProblemDetailsCard with 412
```

### Deterministic state assertions

Tests must assert on text content and `data-testid`, not visual appearance.

```ts
// Good
await expect(page.getByTestId('payment-order-detail')).toBeVisible()
await expect(page.getByTestId('problem-details-card')).toContainText('412')

// Bad — brittle, CSS-dependent
await expect(page.locator('.status-badge-error')).toBeVisible()
```

### Test data isolation

- Use `page.route(...)` mocks for pure UI/component behavior tests.
- Use API-assisted setup (`request` fixture) for tests that need real backend state.
- Each test creates its own merchant and payment order — never share state between tests.
- Clean up test data after the test if the backend is real (or use a dedicated test merchant that is reset between runs).

---

## What Not to Do

- Do not assert only on HTTP status code without asserting on the response body or headers that matter.
- Do not write a full `@SpringBootTest` for a concern that can be tested with `@WebMvcTest`.
- Do not use Playwright to test pure backend logic — use REST Assured.
- Do not hide `ETag`, `If-Match`, or `Idempotency-Key` behavior behind an abstraction that makes the contract invisible.
- Do not use `page.waitForTimeout(...)` in Playwright — use `page.waitForResponse(...)` or assertion-based waits.
- Do not assert on `Authorization` header values in any test output or debug panel — they must always be masked.
