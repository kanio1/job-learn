# Research: Payment Order Contract and Consumer Hardening

**Feature**: `008-payment-order-contract-consumer-hardening`

**Date**: 2026-06-04

## Purpose

This document records decisions needed before implementation task generation. It resolves the questions that were open after `/speckit.specify` and keeps the implementation inside Phase 0 guardrails.

## Decisions

### R-001: List Request Validation Boundary

**Decision**: Treat `PaymentOrderListRequest` or an equivalent request model as the effective validated request boundary.

**Rationale**: The request record already declares validation intent. A controller path that manually constructs the record from raw `@RequestParam` values can bypass Bean Validation and create misleading contracts.

**Implications**:

- Use `@Valid @ModelAttribute PaymentOrderListRequest` or equivalent validated binding.
- Preserve defaults for omitted `page`, `size` and `sort`.
- Reject invalid page, size, status, currency, date and amount filters before repository query execution.

### R-002: Cross-Field List Validation

**Decision**: Validate `fromDate <= toDate` and `minAmount <= maxAmount` at the request/application boundary before repository query construction.

**Rationale**: These are query contract rules, not database constraints. The repository should receive coherent filter ranges.

**Implications**:

- Controller branching should not become the main place for business-style range checks.
- Repository behavior should not be used to turn invalid ranges into empty results.
- Validation messages should describe the allowed range relationship.

### R-003: Sort Allowlist

**Decision**: Support only `createdAt,asc` and `createdAt,desc` in this feature.

**Rationale**: `createdAt` is the current stable list sort contract. Adding amount/status/currency sorting would widen API behavior and may require additional product and index review.

**Implications**:

- `sort=amountMinor,desc` and other fields are validation errors.
- Do not add database indexes for hypothetical new sort fields.

### R-004: Malformed JSON Error

**Decision**: Malformed create JSON returns `400` with `error=malformed_json` when handled by payment controller advice.

**Rationale**: Malformed JSON is a protocol/body parsing failure, not a domain validation failure. A specific error code improves API client behavior without adding business functionality.

**Implications**:

- Handle `HttpMessageNotReadableException` safely.
- Do not include raw request body content, stack traces or parser internals in normal responses.

### R-005: Unsupported Media Type Error

**Decision**: Unsupported create media type returns `415` with `error=unsupported_media_type`.

**Rationale**: The create endpoint should explicitly consume JSON. Non-JSON input should fail with the HTTP status designed for media type negotiation failures.

**Implications**:

- Add explicit JSON consumption to the create mapping.
- Handle `HttpMediaTypeNotSupportedException` safely where payment advice owns the error response.

### R-006: Missing Idempotency Header

**Decision**: Missing `Idempotency-Key` returns a stable validation response.

**Rationale**: The header is part of the retry-safe create contract. Missing it is a client request error, but it should not change create/idempotency semantics.

**Implications**:

- Preserve existing idempotent create/replay behavior.
- Keep validation messages safe and contract-oriented.

### R-007: Database No-Change Decision

**Decision**: Do not add a migration by default.

**Rationale**: Existing constraints and indexes already support the specified hardening. Query range validation belongs in request/application code.

**Implications**:

- Keep `payment_orders` constraints and V3 indexes unchanged.
- Stop and document if implementation reveals a real production schema gap.

### R-008: Security No-Change Decision

**Decision**: Do not add Keycloak roles or realm JSON changes.

**Rationale**: Existing roles already express merchant create, merchant read and platform read access. Backend ownership checks remain the source of truth.

**Implications**:

- Keep `merchant:payments:create`, `merchant:payments:read` and `platform:payments:read` behavior.
- Keep frontend as a consumer of backend authorization results, not an authorization source.

### R-009: Frontend Store Ownership

**Decision**: Move payment order detail loading and create API behavior into `usePaymentOrdersStore`.

**Rationale**: Centralized API state, Zod parsing and error normalization make the dashboard consumer coherent and future tests more focused.

**Implications**:

- Detail page uses store-owned state rather than direct `$fetch` and `any` state.
- Create form owns form-local state only.
- Create/detail responses parse through `paymentOrderResponseSchema`.

### R-010: Frontend Detail 404

**Decision**: A backend `404` on payment detail is rendered as a local dashboard resource-not-found state.

**Rationale**: The route exists; the missing resource is a backend data condition, not a missing frontend page.

**Implications**:

- Do not use route-level not-found handling for this case.
- Clear stale detail data and show neutral dashboard copy.

### R-011: Merchant Zod Schemas

**Decision**: Defer merchant response Zod schemas unless merchant consumer code is touched during implementation.

**Rationale**: Adding schemas for unrelated merchant flows would widen scope beyond this payment order consumer hardening slice.

**Implications**:

- Keep focus on `payment-order.schema.ts` and payment order store/page/component behavior.

## Rejected Alternatives

| Alternative | Reason Rejected |
|---|---|
| Treat unsupported list filters as empty results | Hides client errors and contradicts the request model allowlists. |
| Add more sortable fields now | Would widen API behavior and may require product/index review. |
| Add a database migration for query validation | Query ranges are request contract rules, not persistent data invariants. |
| Add new Keycloak roles | Existing role model is sufficient for this feature. |
| Add new tests as deliverables | Explicitly outside this feature's scope. |
| Add lifecycle or PSP behavior for richer examples | Violates Phase 0 guardrails. |
