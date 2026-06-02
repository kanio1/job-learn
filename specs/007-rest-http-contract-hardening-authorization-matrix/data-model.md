# Data Model: REST HTTP Contract Hardening and Authorization Matrix

**Feature**: Lesson 10 — REST HTTP Contract Hardening and Authorization Matrix
**Date**: 2026-06-02

## Overview

This feature introduces no new persisted entities, tables, or schema changes. The data model describes test data structures: the authorization matrix row record, the HTTP edge test cases, and the seed data used by tests.

## Test Data Structures

### SummaryAccessCase (Authorization Matrix Row)

A record representing one row in the parameterized authorization matrix test.

```java
record SummaryAccessCase(
    String displayName,
    String bolaBflaLabel,
    int expectedStatus
)
```

**Fields**:
- `displayName` — human-readable test case name used in `@DisplayName` or `Arguments.of()`.
- `bolaBflaLabel` — one of `"BOLA"`, `"BFLA"`, or `""` (empty for authentication failures and success cases).
- `expectedStatus` — expected HTTP status code (`200`, `401`, or `403`).

**Token and merchant ID are supplied by the test method**, not stored in the record, because they depend on runtime-created merchants.

### Authorization Matrix Rows

| # | Display Name | Token Construction | Target Merchant | Expected Status | BOLA/BFLA |
|---|---|---|---|---|---|
| 1 | `unauthenticated request returns 401` | no token (public request) | any | `401` | — |
| 2 | `invalid issuer token returns 401` | `TestJwtSupport.invalidIssuerToken()` | any | `401` | — |
| 3 | `invalid signature token returns 401` | `TestJwtSupport.invalidSignatureToken()` | any | `401` | — |
| 4 | `expired token returns 401` | `TestJwtSupport.expiredToken()` | any | `401` | — |
| 5 | `denied token returns 403 [BFLA]` | `TestJwtSupport.deniedToken()` | created merchant | `403` | BFLA |
| 6 | `merchant create-only returns 403 [BFLA]` | `TestJwtSupport.merchantPaymentCreatorToken(merchantId)` | own merchant | `403` | BFLA |
| 7 | `merchant operate-only returns 403 [BFLA]` | `TestJwtSupport.merchantPaymentOperatorToken(merchantId)` | own merchant | `403` | BFLA |
| 8 | `merchant read without merchant_id claim returns 403 [BFLA]` | `TestJwtSupport.merchantPaymentReaderTokenWithoutMerchantIdClaim()` | created merchant | `403` | BFLA |
| 9 | `merchant read own merchant returns 200` | `TestJwtSupport.merchantPaymentReaderToken(merchantId)` | own merchant | `200` | — |
| 10 | `merchant read other merchant returns 403 [BOLA]` | `TestJwtSupport.merchantPaymentReaderToken(merchantA)` | merchant B | `403` | BOLA |
| 11 | `platform payment reader returns 200` | `TestJwtSupport.platformPaymentReaderToken()` | created merchant | `200` | — |
| 12 | `platform merchant-only returns 403 [BFLA]` | `TestJwtSupport.platformOperatorToken()` | created merchant | `403` | BFLA |

### HTTP Edge Test Cases

| # | Test Name | HTTP Method | URI | Headers | Expected Status | Expected Behavior |
|---|---|---|---|---|---|---|
| 1 | `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape` | `GET` | `/summary` | `Accept: application/json` | `200` | Response contains `totalOrders`, not `paymentOrderId` |
| 2 | `malformedMerchantIdReturnsValidationError` | `GET` | `/not-a-uuid/payment-orders/summary` | `Accept: application/json` | `400` | `error=validation`, message contains `must be a valid UUID` |
| 3 | `unsupportedMethodsDoNotExposeSummaryMutationSurface` | `PUT`, `PATCH`, `DELETE` | `/summary` | `Accept: application/json` | `405` | No mutation surface; `Allow` header contains `GET` |
| 4 | `unsupportedAcceptIsRejectedOrExplicitlyCharacterized` | `GET` | `/summary` | `Accept: text/xml` | `406` or `200` | Characterize first; assert actual behavior |
| 5 | `ifNoneMatchDoesNotEnableSummaryCaching` | `GET` | `/summary` | `If-None-Match: "some-etag"` | `200` | No `ETag` in response; normal summary body |

## Existing Entities Referenced (No Changes)

### PaymentOrder (existing)

Source of aggregation data. No new fields or status values.

| Field | Type | Notes |
|---|---|---|
| `paymentOrderId` | `UUID` | Primary key |
| `merchantId` | `UUID` | Foreign key to merchants |
| `amountMinor` | `long` | Amount in minor units |
| `currency` | `String` | `PLN`, `EUR`, `USD` |
| `status` | `String` | `CREATED` only |
| `clientOrderReference` | `String` | Client-provided reference |
| `createdAt` | `Instant` | Creation timestamp |
| `version` | `long` | Optimistic lock version |

### PaymentOrderSummaryResponse (existing)

Read-only aggregation projection. No changes.

| Field | Type | Notes |
|---|---|---|
| `totalOrders` | `long` | `COUNT(*)` |
| `totalAmountMinor` | `long` | `SUM(amount_minor)` |
| `byCurrency` | `List<CurrencySummary>` | Grouped by currency |
| `byStatus` | `List<StatusSummary>` | Grouped by status |

### PaymentErrorResponse (existing)

Error envelope. No changes.

| Field | Type | Notes |
|---|---|---|
| `error` | `String` | Machine-readable error code |
| `message` | `String` | Human-readable message |
| `details` | `List<FieldError>` | Optional field-level details |
| `correlationId` | `String` | Correlation identifier |

## Test Data Isolation

- Each test creates its own merchant via `PaymentApiTestSupport.createActiveMerchant()`.
- Each parameterized matrix row uses per-case token construction.
- No shared mutable state between tests.
- `PostgresContainerSupport` provides isolated PostgreSQL containers per test class.
- Unique merchant references via `MerchantApiTestSupport.uniqueMerchantReference()`.
- Unique idempotency keys via `PaymentApiTestSupport.uniqueIdempotencyKey()`.

## Database Impact

No schema changes. No new Flyway migration. No new indexes.

Existing indexes from V2 and V3 migrations support summary queries:
- `idx_payment_orders_merchant_created` — `(merchant_id, created_at DESC)`
- `idx_payment_orders_merchant_status` — `(merchant_id, status)`
- `idx_payment_orders_merchant_currency` — `(merchant_id, currency)`
