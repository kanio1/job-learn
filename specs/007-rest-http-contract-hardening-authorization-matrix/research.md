# Research: REST HTTP Contract Hardening and Authorization Matrix

**Feature**: Lesson 10 — REST HTTP Contract Hardening and Authorization Matrix
**Date**: 2026-06-02
**Status**: Complete

## Research Objectives

Resolve all unknowns about Spring MVC default behavior for the existing Payment Order Summary endpoint before writing contract tests.

## R-001: Spring MVC Behavior for Unsupported `Accept` Header

**Question**: What does Spring MVC return when a `@RestController` `@GetMapping` receives `Accept: text/xml` or `Accept: application/xml`?

**Decision**: Spring MVC with `@RestController` (which implies `@ResponseBody`) uses content negotiation via `HttpMessageConverter`. When no converter can produce the requested media type, Spring returns `406 Not Acceptable`. However, if the controller method does not specify `produces`, Spring may fall back to the default JSON converter and return `200 OK` with `application/json`.

**Rationale**: The `PaymentOrderController.summarizePaymentOrders()` method does not specify `produces` on `@GetMapping("/summary")`. Spring Boot auto-configures `MappingJackson2HttpMessageConverter` for JSON. The actual behavior must be characterized by sending a test request with `Accept: text/xml` and observing the response.

**Alternatives Considered**:
- Assume `406` always — rejected because `@RestController` without `produces` may be permissive.
- Assume `200` always — rejected because content negotiation may reject non-JSON types.

**Action**: Characterize first. The test `unsupportedAcceptIsRejectedOrExplicitlyCharacterized` must document the actual behavior before locking the assertion.

## R-002: Spring MVC Behavior for Unmapped HTTP Methods

**Question**: What does Spring MVC return when `PUT`, `PATCH`, or `DELETE` is sent to a URI that only has `@GetMapping`?

**Decision**: Spring MVC returns `405 Method Not Allowed` with an `Allow` header listing the supported methods (`GET`, `HEAD`).

**Rationale**: The `PaymentOrderController` maps `@GetMapping("/summary")` only. No `@PutMapping`, `@PatchMapping`, or `@DeleteMapping` exists for the summary URI. Spring MVC's `HttpRequestMethodNotSupportedException` is thrown and handled by the default `ResponseEntityExceptionHandler`, which returns `405`.

**Alternatives Considered**:
- Assume `404` — rejected because the URI exists, just not for that method.
- Assume `403` — rejected because method not allowed is not an authorization failure.

**Action**: Assert `405 Method Not Allowed`. Verify `Allow` header contains `GET`. If `PaymentExceptionHandler` intercepts `HttpRequestMethodNotSupportedException` differently, characterize first.

**Risk**: `PaymentExceptionHandler` is scoped to `assignableTypes = PaymentOrderController.class` and does not handle `HttpRequestMethodNotSupportedException`. The default Spring handler should apply. Verify during characterization.

## R-003: Spring MVC Behavior for `HEAD` and `OPTIONS`

**Question**: What does Spring MVC return for `HEAD` and `OPTIONS` on a `@GetMapping` URI?

**Decision**:
- `HEAD`: Spring MVC automatically supports `HEAD` for any `@GetMapping`. It returns `200 OK` with headers but no body.
- `OPTIONS`: Spring MVC returns `200 OK` with `Allow: GET, HEAD, OPTIONS` by default.

**Rationale**: These are standard HTTP behaviors implemented by the Spring MVC framework. They are not security risks and do not expose mutation surfaces.

**Action**: Characterize but do not assert as hard requirements. Document in test comments. If `HEAD` returns `200` with no body, that is acceptable. If `OPTIONS` returns `200` with `Allow` header, that is acceptable.

## R-004: `MethodArgumentTypeMismatchException` for Malformed UUID

**Question**: Does `PaymentExceptionHandler.handleTypeMismatch()` catch malformed UUID path variables for the summary endpoint?

**Decision**: Yes. The handler catches `MethodArgumentTypeMismatchException` and returns `400` with `error=validation` and message `Invalid merchantId: must be a valid UUID`.

**Rationale**: The `PaymentExceptionHandler` is annotated with `@RestControllerAdvice(assignableTypes = PaymentOrderController.class)`, which covers all controller methods including `summarizePaymentOrders()`. The `@PathVariable UUID merchantId` parameter triggers `MethodArgumentTypeMismatchException` when the path value is not a valid UUID.

**Evidence**: `PaymentExceptionHandler.java:59-64`:
```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<PaymentErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String message = "Invalid " + ex.getName() + ": must be a valid UUID";
    return ResponseEntity.badRequest()
            .body(PaymentErrorResponse.of("validation", message, getCorrelationId()));
}
```

**Action**: Assert `400`, `error=validation`, message contains `must be a valid UUID`.

## R-005: Route Resolution Order — `/summary` vs `/{paymentOrderId}`

**Question**: Does a request to `/api/merchants/{merchantId}/payment-orders/summary` resolve to the summary endpoint or the single-order read endpoint?

**Decision**: The literal `/summary` route wins over the wildcard `/{paymentOrderId}` route.

**Rationale**: Spring MVC resolves route mappings by specificity. Literal path segments take precedence over path variables. The `PaymentOrderController` maps:
- `@GetMapping("/summary")` — literal route
- `@GetMapping("/{paymentOrderId}")` — wildcard route

Spring MVC matches the literal route first.

**Security Config Evidence**: `SecurityConfig.java:40-41`:
```java
.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/summary").hasAnyAuthority("merchant:payments:read", "platform:payments:read")
.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*").hasAnyAuthority("merchant:payments:read", "platform:payments:read")
```

The summary matcher is registered before the wildcard matcher, ensuring security routing also resolves correctly.

**Action**: Assert that `GET /summary` returns the summary response shape (`totalOrders`, `totalAmountMinor`, `byCurrency`, `byStatus`), not the single-order shape (`paymentOrderId`, `amountMinor`, `currency`, `status`).

## R-006: `If-None-Match` Behavior When No `ETag` Is Returned

**Question**: What does Spring MVC do when a request includes `If-None-Match` but the response does not include `ETag`?

**Decision**: Spring MVC ignores `If-None-Match` when the response does not generate an `ETag`. The response is `200 OK` with the normal body.

**Rationale**: `If-None-Match` is a conditional request header. The server compares the provided ETag value with the current representation's ETag. If the controller does not set an `ETag` header, there is nothing to compare, and the conditional is effectively ignored. The `PaymentOrderController.summarizePaymentOrders()` method does not set `ETag` (unlike `createPaymentOrder()` and `getPaymentOrder()`).

**Evidence**: `PaymentOrderController.java:173-175`:
```java
return ResponseEntity.ok()
        .header("X-Correlation-ID", getCorrelationId())
        .body(response);
```

No `ETag` header is set.

**Action**: Assert `200 OK`, no `ETag` header in response, normal summary body.

## R-007: `TestJwtSupport` Gap Analysis

**Question**: Can `TestJwtSupport` express all 12 authorization matrix rows?

**Decision**: Almost. One gap exists: merchant reader token without `merchant_id` claim.

**Gap Analysis**:

| Matrix Row | Existing Method | Status |
|---|---|---|
| Unauthenticated | `MerchantApiTestSupport.publicRequest()` | Available |
| Invalid issuer | `TestJwtSupport.invalidIssuerToken()` | Available |
| Invalid signature | `TestJwtSupport.invalidSignatureToken()` | Available |
| Expired token | `TestJwtSupport.expiredToken()` | Available |
| Denied (no roles) | `TestJwtSupport.deniedToken()` | Available |
| Create-only | `TestJwtSupport.merchantPaymentCreatorToken(merchantId)` | Available |
| Operate-only | `TestJwtSupport.merchantPaymentOperatorToken(merchantId)` | Available |
| Read without `merchant_id` | **MISSING** | Needs new method |
| Read own merchant | `TestJwtSupport.merchantPaymentReaderToken(merchantId)` | Available |
| Read other merchant | `TestJwtSupport.merchantPaymentReaderToken(merchantA)` + target `merchantB` | Available |
| Platform reader | `TestJwtSupport.platformPaymentReaderToken()` | Available |
| Platform merchant-only | `TestJwtSupport.platformOperatorToken()` | Available |

**Action**: Add `merchantPaymentReaderTokenWithoutMerchantIdClaim()` to `TestJwtSupport`. This method creates a token with `merchant:payments:read` role but no `merchant_id` claim.

**Implementation**:
```java
public static String merchantPaymentReaderTokenWithoutMerchantIdClaim() {
    return tokenWithRoles("merchant.payment.reader.no-claim", List.of("merchant:payments:read"));
}
```

## R-008: `PaymentExceptionHandler` Coverage for HTTP Edge Cases

**Question**: Does `PaymentExceptionHandler` intercept any HTTP edge case exceptions that might change expected behavior?

**Decision**: No. The handler covers:
- Domain validation exceptions → `400 validation`
- `MethodArgumentNotValidException` → `400 validation`
- `BindException` → `400 validation`
- `IllegalArgumentException` → `400 validation`
- `DateTimeParseException` → `400 validation`
- `MethodArgumentTypeMismatchException` → `400 validation`
- `MerchantNotPaymentEligibleException` → `409`
- `IdempotencyConflictException` → `409`
- `PaymentOrderNotFoundException` → `404`
- `AccessDeniedException` → `403`

It does NOT handle:
- `HttpRequestMethodNotSupportedException` → default Spring `405`
- `HttpMediaTypeNotAcceptableException` → default Spring `406`
- `MissingPathVariableException` → default Spring `500` (should not occur with UUID type)

**Action**: HTTP edge tests for unsupported methods and unsupported accept rely on Spring MVC default handlers, not `PaymentExceptionHandler`. Characterize first to confirm.

## Summary of Decisions

| ID | Decision | Confidence |
|---|---|---|
| R-001 | Characterize `Accept` behavior first; may be `406` or `200` | Medium — needs runtime verification |
| R-002 | Unsupported methods return `405` | High |
| R-003 | `HEAD` returns `200` no body; `OPTIONS` returns `200` with `Allow` | High |
| R-004 | Malformed UUID returns `400 validation` via existing handler | High |
| R-005 | Literal `/summary` wins over wildcard `/{paymentOrderId}` | High |
| R-006 | `If-None-Match` is ignored when no `ETag` is returned | High |
| R-007 | One new `TestJwtSupport` method needed | High |
| R-008 | HTTP edge exceptions use Spring default handlers | High |
