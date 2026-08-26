# 08 — BFF contract responsibility matrix and targeted HTTP coverage

**What to build:** A documented and tested matrix states which status, body and header behaviors belong to the Nuxt BFF, then Playwright REST covers only the forwarding/transformation risks not already proven at the backend seam.

**Blocked by:** 02 — Typed BFF results and fixture lifecycle contracts; 07 — Test-method artifact reachability and minimal data builders.

**Seams:** Playwright REST/BFF; existing backend REST contract evidence

**Status:** ready-for-agent
**Category:** enhancement

## Implementation guidance

- Inventory current BFF routes and backend evidence before adding tests.
- Classify `Content-Type`, `ETag`, `Vary`, `Cache-Control`, `Idempotency-Replayed`, `X-Correlation-ID` and `Accept-Patch` per route as forwarded, transformed, generated or not applicable.
- Add tests only when the BFF adds a distinct risk or responsibility.
- Preserve auth/session through storage state and assert no token leak.
- If a header is absent from the current product contract, document `NOT_APPLICABLE`; do not change production code under this program.

## Immutable acceptance IDs

- `T08-A01` — Every high-value BFF route family has an explicit responsibility classification.
- `T08-A02` — `X-Correlation-ID` and `Accept-Patch` are VERIFIED or evidence-backed NOT_APPLICABLE.
- `T08-A03` — Existing ETag/If-Match/idempotency tests are reconciled with the matrix.
- `T08-A04` — New Playwright REST tests cover only BFF-specific behavior and do not duplicate a backend status matrix without reason.
- `T08-A05` — Success, Problem Details and empty-body cases use the typed result/narrowing model from ticket 02.
- `T08-A06` — Authorization/session material never appears in response artifacts or logs.
- `T08-A07` — Strict POM typecheck, focused BFF specs and POM-only lint are green.

## Validation and verification

Apply the goal's shared loop. Ticket-specific proof starts from a BFF-specific failure mode, cross-references existing backend evidence and ends with REST/API plus Playwright review.
