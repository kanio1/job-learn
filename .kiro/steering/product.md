# Product Context — Payment Quality Engineering Lab

## Project Purpose

The Payment Quality Engineering Lab is a **learning-oriented payment platform** used to practice:
- Java / Spring Boot backend engineering
- REST API design and HTTP contract hardening
- REST Assured / Playwright API and UI test automation
- Security testing with Keycloak / OAuth / OIDC
- Frontend contract consumption with Nuxt 4

This is not a production payment system. It simulates a realistic payment domain at a level of detail appropriate for Senior QA Automation / SDET learning and interview preparation.

## Learning Goals

- REST/HTTP fundamentals: status codes, headers (ETag, If-Match, Idempotency-Key, Location, Vary, Cache-Control, X-Correlation-ID), content negotiation, problem+json.
- API contract testing: request/response validation, error contract coverage (400/401/403/404/406/409/412/415/422/428).
- Concurrency and idempotency: ETag / If-Match optimistic locking, idempotent create/lifecycle operations.
- Security testing: JWT bearer tokens, Keycloak realm roles, authority-based access control, tenant/merchant ownership.
- Modular backend design: Spring Modulith module boundaries, public API vs internal packages, inter-module events.
- Frontend quality: Nuxt UI dashboard, Zod validation, Playwright E2E tests, HTTP learning panels.

## Business Domains

### Merchants
- Registry of payment platform tenants.
- Operations: create, list, retrieve, activate, suspend.
- Status lifecycle: PENDING → ACTIVE → SUSPENDED.
- Authority: `platform:merchants:create`, `platform:merchants:read`, `platform:merchants:update-status`.

### Payment Orders
- Merchant-scoped orders with amount (minor units), currency, client reference.
- Operations: create (requires `Idempotency-Key`; returns `Location` + `ETag`), retrieve (ETag), list (filtered/paginated), summary.
- Status values: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED.

### Payment Lifecycle
- State-changing operations: authorize, capture, cancel, refund.
- Each requires `Idempotency-Key` + `If-Match` (ETag-based optimistic locking).
- Metadata PATCH also requires `If-Match`.
- History/audit trail: `GET /{id}/history`.

### Idempotency
- `Idempotency-Key` header de-duplicates create and lifecycle operations.
- Conflict response: 409 with problem+json.

### ETag / If-Match
- Versioned resources use ETag for concurrency control.
- Mutations require `If-Match`; missing → 428, stale → 412.

### Problem Responses
- All errors use `application/problem+json` format.
- Standard members: `type`, `title`, `status`, `detail`, `instance`.
- Error codes: 400, 401, 403, 404, 406, 409, 412, 415, 422, 428.

### API / UI Learning Lab
- The frontend exposes HTTP learning panels (HeaderKeyValuePanel, ProblemDetailsCard, RawJsonViewer, ApiDebugPanel).
- A dedicated Error Lab page lets users intentionally trigger supported error responses.

## Current Phase

Branch `018-rest-security-p1-error-auth-method-hardening`.

Implemented domain scope:
- Merchant Registry: create, list, retrieve, activate, suspend.
- Payment Orders: merchant-scoped create/read/list/summary.
- Payment Lifecycle: authorize, capture, cancel, refund, metadata PATCH, status history, ETag/If-Match and idempotency hardening.

## Active Non-Goals

- No `POST /payments` top-level API.
- No real PSP provider integration.
- No Kafka, webhooks, outbox, settlement, payout, reconciliation, KYC, card/PAN/PCI, 3DS, or microservice split.
- No fake KPI dashboard or broad business dashboard metrics beyond backend summary endpoints.
- No complete OAuth/OIDC production integration; local Keycloak for development and tests only.
