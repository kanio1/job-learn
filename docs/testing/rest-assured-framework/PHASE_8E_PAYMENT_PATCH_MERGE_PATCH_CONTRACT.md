# Phase 8E — Payment Order PATCH / JSON Merge Patch Contract

## Goal

Verify the `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` endpoint
contract: valid metadata update, missing `If-Match`, wrong `Content-Type`, and unknown
top-level field.

## Files Added / Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/payment/dto/PatchMetadataRequest.java` | New test-side DTO: `record PatchMetadataRequest(Map<String, String> metadata)` |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/payment/PaymentOrdersApi.java` | Added `patch()`, `patchWithoutIfMatch()`, `patchWithWrongContentType()`, `patchWithUnknownField()` |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/PatchMetadataContractSpec.java` | New spec: 4 tests |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Phase 8E row added |

## Backend PATCH Contract (Discovered)

### Endpoint

```
PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

### Security

```java
// SecurityConfig line 77:
.requestMatchers(HttpMethod.PATCH, "/api/merchants/*/payment-orders/*")
    .hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
```

Requires `merchant:payments:lifecycle` OR `platform:payments:lifecycle`.
For `merchant:payments:lifecycle`, the controller additionally checks that the JWT
`merchant_id` claim matches the `merchantId` path parameter.

### Content-Type

The endpoint accepts BOTH:
- `application/merge-patch+json` (preferred — semantically correct per RFC 7396)
- `application/json` (also accepted; backend treats both identically)

Any other Content-Type → 415 `unsupported_media_type` + `Accept-Patch: application/merge-patch+json`.

### Request Body

Only one top-level field is allowed: `metadata` (`Map<String, String>`).

```json
{"metadata": {"key1": "value1", "key2": "value2"}}
```

Any additional top-level field is captured by `@JsonAnySetter` in `MetadataPatchRequest`,
then `requireOnlyMetadataTopLevelField()` throws `UnknownMetadataPatchFieldException`
→ 400 `unknown_top_level_field` with a `details` array naming each rejected field.

### Guard Order in Controller

This differs from lifecycle action POSTs (which check ETag in the service layer after the
idempotency replay check):

```
1. Security filter        → hasAnyAuthority(lifecycle) — 403 if wrong authority
2. verifyMerchantOwnership() → 403 if merchant_id JWT claim ≠ merchantId path UUID
3. requireOnlyMetadataTopLevelField() → 400 if unknown top-level fields present
4. PaymentEtag.requireVersion(ifMatch) → 428 if null/blank; parses "v{N}" → version long
5. paymentLifecycleService.updateMetadata() → 412 if version stale; stores metadata JSON
```

**Key asymmetry:** step 3 (unknown field check) fires BEFORE step 4 (ETag check).
A request with an unknown field AND a stale or missing `If-Match` returns 400, not 412/428.

### Response (Success)

Same `lifecycleResponse()` helper as lifecycle actions:

| Property | Value |
|---|---|
| HTTP status | 200 |
| ETag | `"v{N+1}"` — JPA `@Version` counter incremented |
| Vary | `If-Match` |
| Cache-Control | `no-store` |
| Content-Type | `application/json` |
| Body | `PaymentLifecycleResponse` — same shape as lifecycle action responses |
| Order status | Unchanged — PATCH is metadata-only |

### Error Codes Summary

| Status | Error Code | Trigger |
|---|---|---|
| 400 | `unknown_top_level_field` | Unknown key in body (`MetadataPatchRequest.@JsonAnySetter`) |
| 403 | `forbidden` | Wrong authority or merchant scope mismatch |
| 412 | `payment_order_version_mismatch` | Stale `If-Match` (version counter mismatch) |
| 415 | `unsupported_media_type` | Content-Type not in `{merge-patch+json, application/json}` |
| 428 | `precondition_required` | `If-Match` missing or blank |

## Tests Added

### `valid_metadata_patch_returns_200_and_increments_etag`

**Setup**: create a fresh order (ETag = `"v0"`) → PATCH with `{metadata:{env:test,phase:8e}}`,
`If-Match: "v0"`, `Content-Type: application/merge-patch+json`.

**Assertions**: 200, `ETag: "v1"`, `Vary: If-Match`, `Cache-Control: no-store`,
`status: "CREATED"` (state machine not advanced).

**Business risk verified**: metadata updates are version-tracked — concurrent callers cannot
silently overwrite each other's changes; each must present the current ETag.

### `patch_without_if_match_returns_428`

**Setup**: PATCH the seeded `PAYMENT_ORDER_ALPHA_001_CREATED_ID` without `If-Match` header.

**Assertions**: 428 `precondition_required`, `application/problem+json`, `correlationId`,
`no-store`, `Vary: If-Match`.

**Learning**: 428 (RFC 6585 §3) = "server requires a precondition the client did not send".
Distinct from 412 ("precondition sent but did not match").

### `patch_with_wrong_content_type_returns_415_and_accept_patch_header`

**Setup**: PATCH the seeded created order with `Content-Type: text/plain`.

**Assertions**: 415 `unsupported_media_type`, `Accept-Patch: application/merge-patch+json`.

**Learning**: 415 fires at Spring MVC dispatcher level (`HttpMediaTypeNotSupportedException`)
— BEFORE the controller method is invoked, before ownership/field/ETag checks.
`Accept-Patch` (RFC 5789 §3.1) on the 415 response advertises the correct content-type.

### `patch_with_unknown_top_level_field_returns_400_with_field_details`

**Setup**: PATCH the seeded created order with body
`{"metadata":{},"unknownField":"forbidden"}`, `If-Match: "v0"`.

**Assertions**: 400 `unknown_top_level_field`, `application/problem+json`, `correlationId`,
`details[].field = "unknownField"`, `no-store`.

**Learning**: `requireOnlyMetadataTopLevelField()` fires BEFORE `requireVersion()`. Even a
stale `If-Match` returns 400 when an unknown field is present.

## New API Facade Methods

### `PaymentOrdersApi.patch()`

```java
public static Response patch(
        String merchantId, String paymentOrderId, String ifMatch, PatchMetadataRequest body) {
    return RequestSpecs.mergePatch(ifMatch)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body(body)
            .when()
            .patch(BY_ID_PATH);
}
```

Uses `RequestSpecs.mergePatch(ifMatch)` which sets `Content-Type: application/merge-patch+json`
(without charset — suppressed by `EncoderConfig` in `RestAssuredSetup`) and `If-Match`.

### `PaymentOrdersApi.patchWithoutIfMatch()`

```java
public static Response patchWithoutIfMatch(
        String merchantId, String paymentOrderId, PatchMetadataRequest body) {
    return RequestSpecs.base()
            .contentType(ContentTypes.MERGE_PATCH_JSON)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body(body)
            .when()
            .patch(BY_ID_PATH);
}
```

Omits `If-Match` to trigger 428. Uses `RequestSpecs.base()` directly and sets content-type
manually (as opposed to `RequestSpecs.mergePatch()` which always adds `If-Match`).

### `PaymentOrdersApi.patchWithWrongContentType()`

```java
public static Response patchWithWrongContentType(
        String merchantId, String paymentOrderId, String ifMatch) {
    return RequestSpecs.base()
            .contentType("text/plain")
            .header(Headers.IF_MATCH, ifMatch)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body("{}")
            .when()
            .patch(BY_ID_PATH);
}
```

Sends `Content-Type: text/plain` to trigger 415. The body is irrelevant (415 fires before it
is parsed), but sending `{}` keeps the request well-formed for readability.

### `PaymentOrdersApi.patchWithUnknownField()`

```java
public static Response patchWithUnknownField(
        String merchantId, String paymentOrderId, String ifMatch) {
    return RequestSpecs.mergePatch(ifMatch)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body("{\"metadata\":{},\"unknownField\":\"forbidden\"}")
            .when()
            .patch(BY_ID_PATH);
}
```

Sends a raw JSON string with an extra top-level key to trigger 400. The `If-Match` is syntactically
valid but irrelevant — `requireOnlyMetadataTopLevelField()` fires before `requireVersion()`.

## Test DTO

### `PatchMetadataRequest`

```java
public record PatchMetadataRequest(Map<String, String> metadata) {}
```

Mirrors the backend's `MetadataPatchRequest.metadata` field. Jackson serializes it to
`{"metadata": {...}}`. No annotations needed — record component names map directly to JSON keys.

## Content-Type Without Charset

REST Assured's default `EncoderConfig` appends `;charset=UTF-8` to any non-standard content-type,
turning `application/merge-patch+json` into `application/merge-patch+json; charset=UTF-8`.
The backend's `@PatchMapping(consumes = {"application/merge-patch+json", "application/json"})`
does not list the charset variant → Spring MVC would reject it as 415.

Fix applied in `RestAssuredSetup.install()`:
```java
.encoderConfig(EncoderConfig.encoderConfig()
        .appendDefaultContentCharsetToContentTypeIfUndefined(false))
```

This suppresses charset appending for all content-types where the test does not define one.
The `RequestSpecs.mergePatch()` method relies on this global setting — callers do not need
to do anything special.

## HTTP/REST and SDET Topics

**Why JSON Merge Patch (RFC 7396) instead of JSON Patch (RFC 6902)?**
Merge Patch sends the desired end-state of the changed sub-document; the server replaces that
sub-document with the received value. JSON Patch sends a list of operations (add, remove,
replace, …) applied atomically. Merge Patch is simpler and appropriate here because the only
patchable sub-document is `metadata` (a flat string map) — there is no benefit in expressing
a simple replacement as operations.

**Why does the PATCH endpoint accept `application/json` as well as `application/merge-patch+json`?**
Some HTTP clients (older SDKs, proxies, strict firewalls) cannot send custom content-types.
The backend treats both identically — the `metadata` field is the only accepted input regardless
of which variant is used. The `Accept-Patch: application/merge-patch+json` header on 415 responses
signals the preferred type to standards-compliant clients.

**Why is the unknown-field check before the ETag check?**
Field validation is a syntactic/structural check that can be answered without DB access.
The ETag check requires reading the current version from the DB. Failing fast on structural
errors is cheaper and gives the caller a clearer, more specific error signal.

**Why does `lifecycleResponse()` increment the ETag on PATCH?**
`paymentLifecycleService.updateMetadata()` calls `order.updateMetadata(newValue)` and then
saves the order. The JPA `@Version` column is incremented on every `UPDATE`, regardless of
which fields changed. Using the same `lifecycleResponse()` helper means PATCH and lifecycle
actions (authorize/capture/cancel/refund) return consistent header sets (`ETag`, `Vary`, `Cache-Control`).

## Test Results

- **79 offline tests**: all pass (unchanged)
- **57 live tests**: all pass (4 new in Phase 8E)
