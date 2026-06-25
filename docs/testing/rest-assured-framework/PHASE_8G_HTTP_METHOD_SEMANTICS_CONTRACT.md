# Phase 8G — HTTP Method Semantics and Content-Negotiation Contract

## Goal

Verify the HTTP method semantics and content-negotiation error contract for
`/api/merchants/{merchantId}/payment-orders/{paymentOrderId}`.

This phase focuses on four HTTP-level contracts that are distinct from the business lifecycle
contracts tested in Phases 7D–8F:

1. **HEAD** — presence check, ETag, caching headers, no body
2. **OPTIONS** — capability discovery: Allow + Accept-Patch, no authentication required
3. **405** — unsupported method (DELETE) returns Allow header per RFC 9110 §15.5.6
4. **406** — unacceptable Accept header (text/xml on GET) returns not_acceptable

## Files Added / Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/payment/PaymentOrdersApi.java` | Added `headById()`, `optionsById()`, `deleteById()`, `getByIdWithAccept()` facade methods |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/HttpMethodSemanticsContractSpec.java` | New spec: 4 tests |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Phase 8G row added |

## Backend Implementation (Discovered)

### HEAD `/api/merchants/{merchantId}/payment-orders/{paymentOrderId}`

```java
// PaymentOrderController:
@RequestMapping(value = "/{paymentOrderId}", method = RequestMethod.HEAD)
public ResponseEntity<Void> headPaymentOrder(...) {
    PaymentOrder order = findReadablePaymentOrder(merchantId, paymentOrderId, authentication, jwt);
    return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                    PaymentHttpHeaders.VARY_AUTHORIZATION)
            .header("ETag", PaymentEtag.from(order))
            .build();   // ResponseEntity<Void> — Spring suppresses body automatically
}
```

**Explicit handler** — not Spring MVC's default HEAD-from-GET behavior.
Returns `ResponseEntity<Void>`, so the response body is empty regardless of serialization.
The `sensitivePaymentResponse()` helper injects `Cache-Control: no-store`, `Vary: Authorization`,
and `X-Correlation-ID`.

**Security**: requires `merchant:payments:read` OR `platform:payments:read`.
The `findReadablePaymentOrder()` private method applies the same ownership check as GET:
platform scope bypasses the `merchant_id` JWT claim check; merchant scope requires it to match.

### OPTIONS `/api/merchants/{merchantId}/payment-orders/{paymentOrderId}`

```java
@RequestMapping(value = "/{paymentOrderId}", method = RequestMethod.OPTIONS)
public ResponseEntity<Void> optionsPaymentOrder(...) {
    return ResponseEntity.noContent()
            .allow(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.PATCH, HttpMethod.OPTIONS)
            .header(PaymentHttpHeaders.ACCEPT_PATCH, PaymentHttpHeaders.MERGE_PATCH_JSON)
            .header(PaymentHttpHeaders.X_CORRELATION_ID, PaymentHttpHeaders.correlationId())
            .build();
}
```

**Explicit handler** — returns a fixed list of methods rather than Spring MVC's auto-enumerated list.
No authentication required: `SecurityConfig` has `requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()`.
Note: `Accept-Patch` is on OPTIONS (capability discovery), not only on 415 (error response).

### 405 Method Not Allowed (e.g. DELETE)

```java
// PaymentExceptionHandler:
@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
public ResponseEntity<PaymentErrorResponse> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException ex) {
    HttpHeaders headers = paymentErrorHeaders();
    if (ex.getSupportedHttpMethods() != null) {
        headers.setAllow(ex.getSupportedHttpMethods());
    }
    return problem(HttpStatus.METHOD_NOT_ALLOWED, ERROR_METHOD_NOT_ALLOWED, ..., headers);
}
```

`paymentErrorHeaders()` injects `Cache-Control: no-store`, `Vary: Authorization`, `X-Correlation-ID`.
`headers.setAllow(ex.getSupportedHttpMethods())` copies the Spring-derived set of registered handlers
for the URL pattern into the `Allow` response header.

**Guard order for DELETE on `/{paymentOrderId}`:**
1. Spring Security filter: `anyRequest().authenticated()` — requires a valid JWT
2. Spring MVC dispatcher: no handler found for DELETE → `HttpRequestMethodNotSupportedException`
3. `handleMethodNotSupported()` → 405 + Allow header

Without auth, the security filter returns 401 before the 405 can fire. This is why the test
uses `Identities.merchantReader(MERCHANT_ALPHA_001_ID)` — not to authorize deletion, but to
pass the security filter and reach the Spring MVC layer.

### 406 Not Acceptable

```java
// PaymentExceptionHandler:
@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
public ResponseEntity<PaymentErrorResponse> handleHttpMediaTypeNotAcceptable(
        HttpMediaTypeNotAcceptableException ex) {
    return problem(HttpStatus.NOT_ACCEPTABLE, ERROR_NOT_ACCEPTABLE,
            "Accept header must allow application/json");
}
```

Spring MVC throws `HttpMediaTypeNotAcceptableException` when the client's `Accept` header does
not include any media type that the handler can produce. The GET handler declares
`produces = "application/json"`. If the client sends `Accept: text/xml`, content negotiation
fails before the handler body executes.

**Key fact:** the 406 error body is returned as `application/problem+json` regardless of the
client's `Accept` header. Spring MVC `@ExceptionHandler` methods bypass content-type negotiation
when writing the error response — the `ResponseEntity<PaymentErrorResponse>` with its explicit
`Content-Type: application/problem+json` is written directly. This is consistent behavior
observed across all 4xx error handlers in this backend.

**Guard order:**
1. Spring Security filter: `anyRequest().authenticated()` — requires auth
2. Spring MVC: content negotiation for GET handler → `HttpMediaTypeNotAcceptableException`
3. `handleHttpMediaTypeNotAcceptable()` → 406 + problem+json body

Without auth, the security filter returns 401 first. The test uses `merchantReader` to
pass the filter.

## New API Facade Methods

### `PaymentOrdersApi.headById(merchantId, paymentOrderId)`

```java
public static Response headById(String merchantId, String paymentOrderId) {
    return RequestSpecs.base()
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .when()
            .head(BY_ID_PATH);
}
```

Uses `RequestSpecs.base()` — auth required (security filter enforces it before HEAD handler).
REST Assured sends a proper HTTP HEAD request. `response.body().asString()` returns `""` for
all HEAD responses.

### `PaymentOrdersApi.optionsById(merchantId, paymentOrderId)`

```java
public static Response optionsById(String merchantId, String paymentOrderId) {
    return RequestSpecs.anonymous()
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .when()
            .options(BY_ID_PATH);
}
```

Uses `RequestSpecs.anonymous()` — no `Authorization` header injected. Callers do NOT need to
call `Ctx.set()` before using this method. OPTIONS is `permitAll()` in the security config.

### `PaymentOrdersApi.deleteById(merchantId, paymentOrderId)`

```java
public static Response deleteById(String merchantId, String paymentOrderId) {
    return RequestSpecs.base()
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .when()
            .delete(BY_ID_PATH);
}
```

Uses `RequestSpecs.base()` — auth required to pass the security filter. This method exists
exclusively for 405 contract testing. If DELETE were ever added as a supported method, this
facade would need updating.

### `PaymentOrdersApi.getByIdWithAccept(merchantId, paymentOrderId, accept)`

```java
public static Response getByIdWithAccept(String merchantId, String paymentOrderId, String accept) {
    return RequestSpecs.base()
            .accept(accept)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .when()
            .get(BY_ID_PATH);
}
```

Uses REST Assured's `.accept(mimeType)` — the dedicated method for the `Accept` request header
(distinct from `.contentType()` which sets `Content-Type`). Overrides any default Accept value
from the base spec (`*/*`).

## Tests Added

### Test 1: `head_existing_payment_order_returns_200_with_etag_and_no_body`

**Identity:** `merchantReader(MERCHANT_ALPHA_001_ID)` (has `platform:payments:read`)
**Resource:** seeded `PAYMENT_ORDER_ALPHA_001_CREATED_ID` (version 0, no transitions)

**Assertions:**
- 200 OK
- `ETag: "v0"` (seeded CREATED order is at JPA `@Version = 0`)
- `Vary: Authorization`
- `Cache-Control: no-store`
- `X-Correlation-ID` present
- `response.body().asString()` is empty

**Learning point:** ETag on HEAD must match what GET would return for the same resource.
Caches use HEAD to check staleness without paying the full body transfer cost.

### Test 2: `options_payment_order_resource_returns_204_with_allow_and_accept_patch`

**Identity:** none (no `Ctx.set()`, anonymous request)
**Resource:** seeded `PAYMENT_ORDER_ALPHA_001_CREATED_ID`

**Assertions:**
- 204 No Content
- `Allow` contains: GET, HEAD, PATCH, OPTIONS
- `Accept-Patch: application/merge-patch+json`
- `X-Correlation-ID` present
- response body is empty

**Learning point:** OPTIONS is the HTTP capability-discovery mechanism. `Accept-Patch` on the
OPTIONS response (RFC 5789 §3.1) advertises the patch content-type before the client attempts PATCH.

### Test 3: `unsupported_method_delete_returns_405_with_allow_header`

**Identity:** `merchantReader(MERCHANT_ALPHA_001_ID)` (to pass security filter)
**Resource:** seeded `PAYMENT_ORDER_ALPHA_001_CREATED_ID`
**Method:** DELETE

**Assertions:**
- ProblemAssert: 405, `method_not_allowed`, `application/problem+json`, correlationId, no-store
- `Allow` header: not blank, contains GET, HEAD, PATCH

**Learning point:** RFC 9110 §15.5.6 mandates `Allow` on 405. The backend's exception handler
extracts the supported methods from Spring MVC's exception object (`ex.getSupportedHttpMethods()`),
which reflects the actual registered handlers — not a hardcoded list.

**Common test-writing mistake:** not authenticating for this test. Without a valid JWT,
`anyRequest().authenticated()` returns 401 before Spring MVC can produce the 405.

### Test 4: `get_with_unacceptable_accept_header_returns_406_not_acceptable`

**Identity:** `merchantReader(MERCHANT_ALPHA_001_ID)`
**Resource:** seeded `PAYMENT_ORDER_ALPHA_001_CREATED_ID`
**Accept header:** `text/xml`

**Assertions:**
- ProblemAssert: 406, `not_acceptable`, `application/problem+json`, correlationId, no-store

**Learning point:** the 406 error body is `application/problem+json` even though the client
said `Accept: text/xml`. This is expected — Spring MVC exception handlers bypass content
negotiation. A client testing only for `Accept: */*` will miss this behavior.

## Security Config Summary (Relevant Excerpts)

```java
// OPTIONS: no auth required on any /api/** path
.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()

// HEAD: requires payment-read authority
.requestMatchers(HttpMethod.HEAD, "/api/merchants/*/payment-orders/*")
    .hasAnyAuthority(MERCHANT_PAYMENTS_READ, PLATFORM_PAYMENTS_READ)

// CORS: exposes Allow and Accept-Patch headers to browsers
allowedMethods = ["GET","HEAD","POST","PATCH","OPTIONS"]
exposedHeaders = [..., "Allow", "Accept-Patch"]
```

## HTTP Concepts Covered

| Concept | Specification | Test |
|---------|--------------|------|
| HEAD semantics (same headers, no body) | RFC 9110 §9.3.2 | Test 1 |
| OPTIONS capability discovery | RFC 9110 §9.3.7 | Test 2 |
| Accept-Patch advertising | RFC 5789 §3.1 | Test 2 |
| 405 + mandatory Allow header | RFC 9110 §15.5.6 | Test 3 |
| Accept content negotiation / 406 | RFC 9110 §12.5.1 | Test 4 |
| Problem+json bypasses Accept on error | Spring MVC behavior | Test 4 |

## REST Assured Concepts Covered

| REST Assured API | Usage |
|-----------------|-------|
| `.when().head(path)` | Send real HTTP HEAD request |
| `.when().options(path)` | Send HTTP OPTIONS request |
| `.when().delete(path)` | Send HTTP DELETE request (for 405 test) |
| `.accept("text/xml")` | Set `Accept` header (distinct from `.contentType()`) |
| `RequestSpecs.anonymous()` | No auth injection (OPTIONS test) |
| `response.body().asString()` is `""` | Expected for HEAD and OPTIONS responses |

## SDET Interview Topics

**Q: Why does HEAD require authentication when OPTIONS does not?**

HEAD reads resource data — `findReadablePaymentOrder()` accesses the DB to build the ETag.
Without auth, the security filter cannot determine which resource the caller is authorized to read.
OPTIONS only returns a static capability list (hardcoded `Allow` + `Accept-Patch`) — it never
accesses the DB, so no identity context is needed.

**Q: If OPTIONS is `permitAll()`, could an attacker use it to enumerate payment order IDs?**

The OPTIONS handler returns the same `Allow` + `Accept-Patch` response regardless of whether the
`paymentOrderId` path variable is a real UUID, a fake UUID, or malformed. It performs no DB lookup,
so it reveals nothing about which orders exist. A real BOLA attack would require the GET or HEAD
endpoint, both of which require authentication and ownership validation.

**Q: Why does the 406 error body have `Content-Type: application/problem+json` when the
client sent `Accept: text/xml`?**

Spring MVC's `@ExceptionHandler` methods produce a `ResponseEntity` with explicit headers.
When writing the error response, the `ContentNegotiatingViewResolver` and response-writing pipeline
bypass content-type negotiation — the `Content-Type: application/problem+json` in the returned
`ResponseEntity` is written directly. This is an intentional Spring MVC design decision: error
responses must always be writable, even if the client's `Accept` header is restrictive.

**Q: Why does the 405 test use `merchantReader` instead of an unauthenticated request?**

The Spring Security filter chain runs before the Spring MVC dispatcher. For a DELETE request on
`/api/merchants/.../payment-orders/...`, the security config matches
`.anyRequest().authenticated()`. Without a valid JWT, the filter returns 401. Only with a valid
JWT does the request reach the MVC dispatcher, which then throws
`HttpRequestMethodNotSupportedException` → 405.

**Q: What is the difference between 406 and 415?**

- **415 Unsupported Media Type**: the client sent a request body with an unsupported `Content-Type`.
  The server cannot parse the input. Fires at the dispatcher before the controller method.
- **406 Not Acceptable**: the client's `Accept` header does not include any media type the server
  can produce for the response. The server can parse the input but cannot format the output.
  Fires during content negotiation for the response.

## Deferred

| Item | Reason |
|------|--------|
| OPTIONS on lifecycle action path `/{id}/{action}` → 204 + Allow:POST,OPTIONS | Separate controller handler — same pattern, lower value once the general case is covered |
| `Accept: */*` with `q=0` forcing 406 | Edge case of content negotiation weighting; low value |
| HEAD on non-existent order → 404 | Same not-found path as GET; covered implicitly by GET tests |
| HEAD If-None-Match (conditional HEAD) | RFC 9110 §13.1.2; not implemented in this backend |

## Test Results

- **79 offline tests**: all pass (unchanged)
- **64 live tests**: all pass (4 new in Phase 8G)
