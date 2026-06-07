# Feature Specification: REST Security Business Cases from BugHunter

**Feature Branch**: `012-rest-security-business-cases-from-bughunter`

**Created**: 2026-06-07

**Status**: Planning only

## Feature Goal

This planning feature converts selected defensive learning ideas inspired by `elementalsouls/Claude-BugHunter` into future business cases for the Payment Quality Engineering Lab.

The goal is not to create a bug-hunting tool. The goal is to define learning-friendly REST API contracts and payment-domain scenarios that can later be implemented in the existing Spring Modulith modular monolith and then verified with Rest Assured.

The future learning surface should help a Senior SDET practice:

- API misconfiguration detection through ordinary HTTP assertions.
- Merchant tenant isolation and BOLA/IDOR negative scenarios.
- Function-level authorization and role matrix testing.
- CORS and browser boundary testing without confusing preflight with business authorization.
- HTTP method, header, cache, conditional update, and problem-details contracts.
- Evidence hygiene for safe bug reports and test failure diagnostics.

## Defensive-Only Scope

This feature uses Claude-BugHunter only as a public taxonomy inspiration source. Its README describes a bundle for bug hunting and external red-team work, with skills around authorization, API misconfiguration, OAuth/JWT, cache poisoning, business logic, race conditions, and evidence hygiene. In this project those topics are translated into local defensive QA cases against a deliberately owned learning application.

Allowed interpretation:

- "What could go wrong in a payment REST API?"
- "Which contract should a defensive API enforce?"
- "Which status, header, body, and persistence oracle should a future Rest Assured test verify?"
- "How can a modular monolith expose realistic failure modes without offensive tooling?"

Disallowed interpretation:

- external recon,
- exploit automation,
- payload libraries,
- bug bounty workflow,
- red-team execution,
- bypass catalogues,
- scanning real targets.

## Relationship To Current Project State

The repository is currently on top of the Payment Orders lifecycle and HTTP contract hardening work.

Current implemented surface includes:

- `POST /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `GET /api/merchants/{merchantId}/payment-orders/summary`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history`
- lifecycle `POST` actions: `authorize`, `capture`, `cancel`, `refund`

Current HTTP contract concepts already present in code/specs:

- merchant-scoped access and platform read/lifecycle exceptions,
- `Idempotency-Key` for create and lifecycle actions,
- `ETag` and `If-Match` for conditional payment mutations,
- `HEAD` and `OPTIONS` on payment detail/action resources,
- `Accept-Patch: application/merge-patch+json`,
- `application/problem+json` payment errors,
- `X-Correlation-ID`,
- `Cache-Control: no-store`,
- `Vary: Authorization` and conditional variants,
- dev/test CORS configuration for `Authorization`, `Content-Type`, `Idempotency-Key`, `If-Match`, and `X-Correlation-ID`.

This feature should extend that direction with future defensive business cases, not replace it.

## Non-Goals

- No tests in this phase.
- No Rest Assured test classes.
- No JUnit tests.
- No Playwright tests.
- No backend production functionality.
- No frontend functionality.
- No offensive tooling.
- No exploit payload libraries.
- No top-level `POST /payments`.
- No microservices.
- No Kafka.
- No real PSP integration.
- No webhook implementation.
- No rate-limiting implementation.
- No settlement, payout, chargeback, or reconciliation implementation.
- No production OAuth/OIDC integration beyond the existing local Keycloak/JWT learning setup.
- No fake KPI dashboard.

## Modular Monolith Constraint

All future implementation candidates must remain inside the existing modular monolith.

Rules:

- Payment API behavior belongs to the payment module unless it is cross-cutting HTTP/security infrastructure.
- Shared HTTP/security utilities may live under `lab.paymentquality.shared` only when they do not depend on payment internals.
- The payment module must not depend on `merchant.internal`.
- Use module public APIs for merchant eligibility and ownership concepts.
- Do not split into gateway, auth service, PSP service, or audit microservice.
- Do not introduce Kafka or an outbox as part of these learning cases.

## REST-Only Constraint

The future cases are REST/API learning cases.

Allowed:

- endpoint contracts,
- request/response headers,
- status codes,
- problem-details bodies,
- DTO shaping,
- Spring Security route authorization,
- local CORS policy,
- local proxy header preservation,
- persistence effects only where needed as a future oracle.

Excluded:

- UI implementation,
- browser automation implementation,
- external scanning,
- non-HTTP protocol work,
- message brokers,
- PSP callbacks,
- webhook delivery.

## Future Rest Assured Learning Value

This planning set is designed for future Rest Assured lessons where each case can teach one concrete skill:

- structuring authorization matrix tests,
- asserting masked `404` behavior without leaking existence,
- asserting `Allow`, `Accept-Patch`, `Vary`, `Cache-Control`, `WWW-Authenticate`, and CORS headers,
- validating `application/problem+json` shape and correlation IDs,
- separating `401`, `403`, `404`, `405`, `409`, `412`, `415`, `422`, and `428`,
- checking idempotency replay versus idempotency conflict,
- keeping failure logs and evidence safe by redacting tokens and cookies.

The contract file in this feature intentionally names exact endpoint families and future test ideas, but does not implement tests.

## Fit With Existing Payment Lifecycle And HTTP Contract Work

Feature 011 hardened payment lifecycle HTTP semantics around `ETag`, `If-Match`, idempotency, cache headers, `HEAD`, `OPTIONS`, and problem details. Feature 012 should be treated as the next planning layer:

1. Keep the current payment lifecycle endpoints as the primary learning surface.
2. Add defensive case definitions around the existing lifecycle contract.
3. Only later choose a small P0 subset for backend implementation.
4. Review that implementation before writing tests.
5. Write Rest Assured tests after the behavior exists and has been reviewed.

The strongest immediate continuity is around:

- metadata PATCH mass assignment and unknown field policy,
- merchant ownership masking,
- role-level lifecycle authorization,
- CORS preflight for conditional/idempotent requests,
- method tampering and `Allow` behavior,
- cache safety for sensitive payment resources,
- idempotency and lifecycle race conditions,
- JWT negative cases,
- exposure policy for future Actuator/OpenAPI dependencies,
- evidence hygiene for Rest Assured failure output.

## Local Change Context At Planning Time

Before this work, local uncommitted changes existed only in:

- `.codex/agents/implementation-learning-mentor.toml`
- `.codex/agents/sdet-reviewer.toml`
- `.codex/agents/test-architect.toml`

The changes rename `instructions` to `developer_instructions` and remove `read_only = true`. They do not alter payment endpoints, shared security, frontend proxy files, specs, or vault notes. They should still be preserved.

No staged changes and no untracked files were present before creating this planning feature's directories/files.

## Clarifications Needed Before Implementation

- Should merchant ownership mismatch return `403` or masked `404` consistently for every payment endpoint family?
- Should platform lifecycle users also be allowed to read detail/history across merchants, or must they combine lifecycle with read/audit roles?
- Should metadata PATCH reject unknown top-level JSON fields strictly, and should metadata keys have an allow-list or only size/name validation?
- Should `Content-Type: application/json` remain temporarily accepted for metadata PATCH, or should future behavior require only `application/merge-patch+json`?
- Should `OPTIONS` return `204 No Content` consistently for contract discovery and CORS preflight?
- Should `Allow` values be centralized per endpoint family to avoid drift?
- Should unknown `Origin` produce no CORS allow headers, or an explicit defensive problem response for non-browser clients?
- Should `Access-Control-Expose-Headers` also include `Allow` and `Accept-Patch` for browser contract inspection?
- Should forwarded headers be ignored entirely in local development, or trusted only when an explicit proxy profile is enabled?
- Should future Actuator/OpenAPI be absent, disabled by default, or exposed only under a dev profile with explicit allow-list?
- Should JWT invalid audience be implemented as a real resource-server validation rule now or deferred as a test-only topic?
- What exact redaction policy should Rest Assured support apply to `Authorization`, `Cookie`, `Set-Cookie`, and token-looking values in bodies?

## Deliberately Excluded Topics

- SQL injection payload libraries.
- XSS payload catalogues.
- SSRF payloads.
- RCE, template injection, deserialization exploitation.
- Web cache poisoning exploitation chains.
- HTTP request smuggling.
- GraphQL introspection and GraphQL-specific testing.
- File upload attacks.
- Cloud IAM, Kubernetes, CI/CD, VPN, M365, Okta, SAML, SharePoint, APK, or supply-chain attack paths.
- Real brute-force tooling or bypasses.
- Rate limiting implementation in this phase.
- PSP failure modeling.
- Webhooks, Kafka, settlement, payout, chargeback, and reconciliation.

## Planning Artifacts

- `specs/012-rest-security-business-cases-from-bughunter/spec.md`
- `specs/012-rest-security-business-cases-from-bughunter/contracts/rest-security-learning-cases.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 16 - REST Security Business Cases from BugHunter.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Backlog/REST Security Learning Backlog.md`

## Suggested Next Prompts

### Next Prompt 1 - Implement P0 backend behavior only

Implement only P0 backend behavior from `specs/012-rest-security-business-cases-from-bughunter/contracts/rest-security-learning-cases.md`. Do not write tests. Keep changes in the Spring Modulith modular monolith, preserve existing REST contracts unless the P0 case explicitly changes them, and do not add PSP, Kafka, webhooks, rate limiting, or frontend functionality.

### Next Prompt 2 - Review the implementation as an architect

Review the current branch implementation against `specs/012-rest-security-business-cases-from-bughunter/spec.md` and the P0 cases, including local uncommitted changes. Do not modify code. Prioritize REST contract behavior, authorization, tenant isolation, headers, problem details, cache policy, and Spring Modulith boundaries.

### Next Prompt 3 - Write Rest Assured tests

After P0 backend behavior is implemented and reviewed, write focused Rest Assured tests only for the implemented P0 cases. Assert status codes, headers, `application/problem+json` bodies, authorization/tenant behavior, idempotency or ETag behavior where relevant, and persistence effects only where needed.
