# 02 — Typed BFF results and fixture lifecycle contracts

**What to build:** The live app-as-API client and fixtures expose accurate Playwright types, safe success/error response contracts, and deterministic setup/teardown without relying on optional intersections, broad JSON casts, or the wrong runner context type.

**Blocked by:** 01 — Strict compiler gate and runtime POM contracts.

**Seams:** TypeScript static; Playwright REST/BFF; fixture lifecycle

**Status:** ready-for-agent
**Category:** enhancement

## Implementation guidance

- Import library and runner types from their canonical modules; use `WorkerInfo` or a minimal structural contract for worker-scoped setup.
- Introduce one small HTTP result/parsing vocabulary that handles JSON, empty bodies, Problem Details, HEAD and 304.
- Do not model successful bodies as intersections with Problem Details.
- Add runtime narrowing only at high-risk BFF boundaries already covered by the suite; reuse existing Zod schemas where present.
- Keep the current facade unless a domain extraction demonstrably reduces duplication. If extracting, use expand → migrate → contract and keep every intermediate commit/checkpoint compilable.
- Verify disposal of API contexts and browser contexts on both success and failure paths.

## Immutable acceptance IDs

- `T02-A01` — `Playwright`, `Browser`, `TestInfo`, and `WorkerInfo` types come from correct modules and match fixture scope.
- `T02-A02` — Worker setup depends only on fields actually available in worker scope.
- `T02-A03` — Standard success/error flows no longer require `body!` or `as any`.
- `T02-A04` — Empty-body responses are parsed without throwing or manufacturing JSON.
- `T02-A05` — Problem Details can be narrowed independently of success DTOs.
- `T02-A06` — Existing REST/BFF assertions remain in specs and retain status, body, and header checks.
- `T02-A07` — API and browser contexts are disposed deterministically.
- `T02-A08` — Strict POM typecheck, focused BFF specs, and fixture-dependent discovery are green.

## Validation and verification

Apply the goal's shared loop one endpoint family at a time. Ticket-specific proof: focused BFF spec, full POM typecheck, POM lint and review for speculative abstraction.
