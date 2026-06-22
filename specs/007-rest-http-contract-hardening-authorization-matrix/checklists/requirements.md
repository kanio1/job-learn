# Requirements Checklist: REST HTTP Contract Hardening and Authorization Matrix

**Feature**: Lesson 10 — REST HTTP Contract Hardening and Authorization Matrix
**Spec**: [spec.md](./spec.md)
**Plan**: [plan.md](./plan.md)
**Date**: 2026-06-02
**Status**: COMPLETE — all requirements implemented and verified

## Functional Requirements

- [x] FR-401: Summary route returns summary response shape, not single-order read shape
- [x] FR-402: Malformed `merchantId` returns `400 validation`
- [x] FR-403: Unsupported methods (`PUT`, `PATCH`, `DELETE`) do not expose mutation surface
- [x] FR-404: Unsupported `Accept` behavior is explicitly tested or characterized
- [x] FR-405: Normal `Accept: application/json` returns `200 OK` with JSON
- [x] FR-406: `If-None-Match` does not enable summary cache semantics
- [x] FR-407: Summary response does not include `ETag`
- [x] FR-408: Error responses preserve existing error contract and `X-Correlation-ID`
- [x] FR-409: Unauthenticated requests return `401`
- [x] FR-410: Invalid issuer/signature/expired tokens return `401`
- [x] FR-411: Denied authenticated callers return `403`
- [x] FR-412: Create-only role returns `403` (BFLA)
- [x] FR-413: Operate-only role returns `403` (BFLA)
- [x] FR-414: Read without `merchant_id` claim returns `403` (BFLA)
- [x] FR-415: Read own merchant returns `200`
- [x] FR-416: Read other merchant returns `403` (BOLA)
- [x] FR-417: Platform payment reader returns `200`
- [x] FR-418: Platform merchant-only role returns `403` (BFLA)
- [x] FR-419: BOLA and BFLA labels are visible in test names or matrix row labels
- [x] FR-420: No new payment business capability is introduced
- [x] FR-421: No new endpoint, role, claim, status, or business dashboard is introduced
- [x] FR-422: Test failure output does not expose bearer token values
- [x] FR-423: Existing summary/list/security regression tests remain green
- [x] FR-424: `PaymentModuleTest` remains green

## Non-Functional Requirements

- [x] NFR-401: HTTP edge tests characterize Spring MVC defaults before locking assertions
- [x] NFR-402: Authorization matrix uses `@ParameterizedTest` / `@MethodSource`
- [x] NFR-403: Test classes do not duplicate existing test coverage
- [x] NFR-404: Test data is created per test or per parameterized case group
- [x] NFR-405: No shared mutable fixtures
- [x] NFR-406: No new Flyway migration

## Acceptance Criteria

- [x] AC-01: New HTTP edge contract tests compile and pass
- [x] AC-02: New authorization matrix tests compile and pass
- [x] AC-03: Route collision risk is covered by a test
- [x] AC-04: Malformed path, unsupported method, and unsupported accept are asserted or characterized
- [x] AC-05: Authorization matrix covers 401, 403, and 200 outcomes
- [x] AC-06: BOLA and BFLA are visible in test names or matrix row labels
- [x] AC-07: No new payment business capability is introduced
- [x] AC-08: No new endpoint, role, claim, status, or business dashboard is introduced
- [x] AC-09: Existing summary/list/security regression tests remain green
- [x] AC-10: `PaymentModuleTest` remains green
- [x] AC-11: Vault lesson evidence is updated after implementation

## Definition of Done

- [x] DoD-01: All functional requirements are implemented
- [x] DoD-02: All non-functional requirements are implemented
- [x] DoD-03: All acceptance criteria pass
- [x] DoD-04: No constitution violations
- [x] DoD-05: Post-design constitution check passes
