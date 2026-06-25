# Phase 8I — ResponseSpecs and Contract Assertion Cleanup

## Goal

Apply the pre-existing (but unused) `ResponseSpecs` to the clearest duplication hotspots in the
scenario layer. Fix a latent bug in `conditional()` and `created()` discovered during the
application exercise.

## Discovery: ResponseSpecs was defined but never used

Before this phase, `ResponseSpecs` had four specs (`sensitive`, `problemJson`, `conditional`,
`created`) that were written speculatively but never applied to any scenario test. All header
assertions — Cache-Control, Vary, X-Correlation-ID — were inline strings across all 67 live tests.

Grep pattern `spec(ResponseSpecs` returned zero hits in any scenario class.

## Bug found: Vary: Authorization inheritance chain was wrong

`conditional()` and `created()` both extended `sensitive()`:

```java
// BEFORE (buggy): conditional() extended sensitive()
public static ResponseSpecification conditional() {
    return new ResponseSpecBuilder()
            .addResponseSpecification(sensitive())     // sensitive() checks Vary: Authorization
            .expectHeader(Headers.VARY, containsStringIgnoringCase("If-Match"))
            .build();
}
```

`sensitive()` asserts `Vary: Authorization`. But lifecycle success responses (200 from
authorize/capture/cancel/refund) carry `Vary: If-Match` — NOT `Vary: Authorization`. Applying
`conditional()` to lifecycle tests would have failed the Vary: Authorization check.

The same defect applied to `created()`: 201 responses carry `Vary: Idempotency-Key` (the backend
sends only the Idempotency-Key value, not "Authorization, Idempotency-Key" — see Phase 7C notes).

**This is why the specs were never used.** Had anyone applied them, tests would have failed
immediately.

## Fix: extract private `noCache()` base

```
Vary header by response type:
  GET / HEAD / LIST / summary success  →  Vary: Authorization       →  sensitive()
  Error responses (4xx)                →  Vary: Authorization       →  problemJson() / ProblemAssert
  Lifecycle action 200 (write paths)   →  Vary: If-Match            →  conditional()
  POST 201 create                      →  Vary: Idempotency-Key     →  created()
```

The two assertions shared by ALL response types (X-Correlation-ID present, Cache-Control: no-store)
were extracted to a private `noCache()` method. Each public spec then adds its correct Vary header:

```java
private static ResponseSpecification noCache() {
    return new ResponseSpecBuilder()
            .expectHeader(Headers.CORRELATION_ID, notNullValue(String.class))
            .expectHeader(Headers.CACHE_CONTROL, containsString("no-store"))
            .build();
}

public static ResponseSpecification sensitive() {
    return new ResponseSpecBuilder()
            .addResponseSpecification(noCache())
            .expectHeader(Headers.VARY, containsStringIgnoringCase("Authorization"))
            .build();
}

public static ResponseSpecification conditional() {
    return new ResponseSpecBuilder()
            .addResponseSpecification(noCache())                              // NOT sensitive()
            .expectHeader(Headers.VARY, containsStringIgnoringCase("If-Match"))
            .build();
}

public static ResponseSpecification created() {
    return new ResponseSpecBuilder()
            .addResponseSpecification(noCache())                              // NOT sensitive()
            .expectHeader(Headers.VARY, containsStringIgnoringCase("Idempotency-Key"))
            .expectHeader(Headers.ETAG, matchesPattern("\"v\\d+\""))
            .expectHeader(Headers.LOCATION, notNullValue(String.class))
            .build();
}
```

`problemJson()` correctly continues to extend `sensitive()` — error responses carry
`Vary: Authorization` just like read responses.

## Files Modified

| File | Change |
|------|--------|
| `apps/api-tests/.../core/http/ResponseSpecs.java` | Fixed Vary inheritance bug; added private `noCache()`; added `Location` check to `created()` |
| `apps/api-tests/.../scenarios/PaymentOrdersContractSpec.java` | Applied specs at 6 locations |
| `apps/api-tests/.../scenarios/PaymentSummaryContractSpec.java` | Applied `sensitive()` to summary GET |
| `apps/api-tests/.../scenarios/PartialRefundContractSpec.java` | Applied `conditional()` to refund happy path |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Phase 8I row added |

## ResponseSpecs applied (8 locations)

### `PaymentOrdersContractSpec` — 6 locations

**Before (GET security headers test):**
```java
response.then().statusCode(200);
String cacheControl = response.header(Headers.CACHE_CONTROL);
assertThat(cacheControl).isNotNull().contains("no-store");
String vary = response.header(Headers.VARY);
assertThat(vary).isNotNull().containsIgnoringCase("Authorization");
assertThat(response.header(Headers.CORRELATION_ID)).isNotNull();
```

**After:**
```java
response.then().statusCode(200).spec(ResponseSpecs.sensitive());
```

**Before (authorize/capture/cancel/refund happy paths — repeated 4 times):**
```java
assertThat(response.header(Headers.VARY)).isNotNull().containsIgnoringCase("If-Match");
assertThat(response.header(Headers.CACHE_CONTROL)).isNotNull().contains("no-store");
```

**After:**
```java
response.then().spec(ResponseSpecs.conditional());
```

**Before (payment history GET):**
```java
assertThat(historyResponse.header(Headers.VARY)).containsIgnoringCase("Authorization");
assertThat(historyResponse.header(Headers.CACHE_CONTROL)).contains("no-store");
```

**After:**
```java
historyResponse.then().spec(ResponseSpecs.sensitive());
```

### `PaymentSummaryContractSpec` — 1 location

**Before:**
```java
assertThat(response.header(Headers.CACHE_CONTROL))
        .as("Cache-Control must be no-store — financial aggregates must never be cached")
        .isNotNull().contains("no-store");
assertThat(response.header(Headers.VARY))
        .as("Vary: Authorization — different JWT holders see different merchant totals")
        .isNotNull().containsIgnoringCase("Authorization");
assertThat(response.header(Headers.CORRELATION_ID))
        .as("X-Correlation-ID must be propagated by CorrelationIdFilter")
        .isNotNull();
```

**After:**
```java
response.then().spec(ResponseSpecs.sensitive());
```

### `PartialRefundContractSpec` — 1 location

**Before:**
```java
assertThat(refundResponse.header(Headers.VARY)).containsIgnoringCase("If-Match");
assertThat(refundResponse.header(Headers.CACHE_CONTROL)).containsIgnoringCase("no-store");
```

**After:**
```java
refundResponse.then().spec(ResponseSpecs.conditional());
```

## What was intentionally NOT refactored

**Idempotency replay test (200 response in `PaymentOrdersContractSpec`):**
The `Vary: Idempotency-Key` assertion at the 200 replay point has an explanatory comment about
why it observes "Idempotency-Key" rather than "Authorization, Idempotency-Key". That comment is
part of the test's educational value. Wrapping it in `spec(ResponseSpecs.created())` would hide
the comment and lose context. Kept explicit.

**201 create test:**
The test asserts `ETag: "v0"` (specific value) and `Location` contains the exact merchant ID.
The `created()` spec checks ETag as pattern `"vN"` and Location as not-null. Applying the spec
would either duplicate or weaken the existing assertions. The test is already reading clearly.
Kept explicit.

**`LifecycleIdempotencyContractSpec`, `TenantIsolationContractSpec`:**
These tests exercise cross-cutting concerns (idempotency, auth scoping) rather than header
contracts. Their header assertions are minimal and the tests are already concise. Adding
`.spec()` calls would not improve readability.

**All error path tests (ProblemAssert chains):**
`ProblemAssert` already consolidates error contract assertions (hasNoStore, varyContains, hasStatus,
hasError, hasCorrelationId, hasContentTypeProblemJson). Applying `spec(ResponseSpecs.problemJson())`
alongside `ProblemAssert` would be redundant — two oracles for the same headers. Kept as-is.

**Rule applied:** *If a ResponseSpecs abstraction makes a test less readable: do not apply it.*

## X-Correlation-ID coverage improvement

Applying `conditional()` to the 4 lifecycle happy-path tests added an X-Correlation-ID assertion
that was previously missing from those tests. This is a net improvement: the `CorrelationIdFilter`
contract is now asserted on write responses, not just read responses.

## SDET Interview Topics

**Q: Why did the pre-existing `conditional()` spec have a Vary: Authorization bug?**

Because `conditional()` extended `sensitive()`, which asserts `Vary: Authorization`. Lifecycle
success responses (200 from authorize/capture/cancel/refund) carry `Vary: If-Match` (the backend
uses `lifecycleResponse()` not `sensitivePaymentResponse()`). No one caught this because the specs
were never applied. The test suite passing wasn't evidence that the specs were correct — it was
evidence that the specs weren't tested.

**Q: What is the value of `ResponseSpecification` over ad-hoc `assertThat` header checks?**

Two things:
1. **Maintainability:** if the backend changes a header contract (e.g. renames `X-Correlation-ID`
   to `Trace-ID`), updating one spec fixes all tests that use it, versus hunting down 30+ inline
   assertions.
2. **Compositional correctness:** specs compose (`problemJson` extends `sensitive`) so the contract
   hierarchy is explicit in code, not implied by scattered assertions.

**Q: Why did you NOT refactor all 67 tests to use ResponseSpecs?**

Three reasons:
1. Many tests assert specific header *values* (not just presence) that are test-specific: exact ETag
   "v0", exact Location URL. The spec checks pattern or presence only — applying it alongside the
   specific assertion is redundant.
2. Some tests have explanatory comments about why a Vary header is structured a specific way
   (e.g. Phase 7C idempotency analysis). Hiding those header assertions inside a spec call loses
   the explanation.
3. The task principle: "if a ResponseSpecs abstraction makes a test less readable, do not apply it."

**Q: Why are `conditional()` and `created()` correct to NOT extend `sensitive()`?**

Because the Vary header is different:
- `sensitive()` → `Vary: Authorization` (GET/HEAD responses: the representation varies by who is
  asking, so `Authorization` is the cache key dimension)
- `conditional()` → `Vary: If-Match` (lifecycle write responses: the representation varies by
  which version was matched, so `If-Match` is the cache key dimension)
- `created()` → `Vary: Idempotency-Key` (201 responses: the representation varies by which
  idempotency key created it)

All three are "sensitive" in the business sense, but the Vary header serves caching infrastructure
semantics, not business semantics. The correct Vary value must match what the backend actually sends.

## Test Results

- **79 offline tests**: all pass (unchanged)
- **67 live tests**: all pass (no new live tests added; 8 existing tests now use ResponseSpecs)
