---
name: rest-api-test-design
description: Use to design or review REST Assured tests and Playwright REST/BFF HTTP tests for merchant and payment-order APIs; do not use for PSP, Kafka, or header requirements that are not in current code/spec, or generic REST tutorials.
---

# REST API Test Design

Use this skill for focused REST/API test design in the current repository. When **writing** the tests test-first, follow `tdd` (REST Assured seam and Playwright REST seam). This skill owns coverage matrix, data/token setup, and assertion design.

## Inspect First

- Controller and exception handler for the endpoint.
- `SecurityConfig` authorities and ownership checks.
- Existing REST Assured support classes in `testsupport`.
- Playwright REST / BFF under `apps/frontend/tests-pom` (live POM is the only product Playwright tree).
- Flyway migration and repository behavior if persistence is part of the oracle.
- Current spec under `.codex/specs/` or `docs/testing/**`. Historical `specs/` is prior art only.

## Merchant API Coverage

Cover:

- `POST /api/merchants`
- `GET /api/merchants`
- `GET /api/merchants/{id}`
- `POST /api/merchants/{id}/activate`
- `POST /api/merchants/{id}/suspend`

Check status codes, response fields, duplicate normalized reference conflict, validation, malformed/unknown ID, lifecycle transitions, authorization roles, and persisted state when relevant.

## Payment API Coverage

Cover only implemented behavior:

- Create/read/list/summary/history.
- Authorize/capture/cancel/refund lifecycle.
- Metadata PATCH.
- Idempotency, `ETag`, `If-Match`, `X-Correlation-ID`, `Cache-Control`, `Vary`, `Location`, `Allow`, and `Accept-Patch` where the current code/spec requires them.
- Tenant masking, platform reader/operator exceptions, and merchant ownership.

## Output Format

Return:

1. Test purpose.
2. Existing coverage found.
3. Missing cases ranked by risk.
4. Proposed test names.
5. Data setup and token/authority setup.
6. REST Assured assertions for status, body, headers, and persistence.
7. Exact prompt for the main Codex session to implement one small test task.

## Guardrails

- Do not add payment/PSP/Kafka/settlement scope.
- Do not treat learner copies (`My*`, `Lesson*`) as regression coverage.
- Prefer unique references and idempotency keys.
- Keep tests readable; avoid framework expansion unless repeated pain is visible.
