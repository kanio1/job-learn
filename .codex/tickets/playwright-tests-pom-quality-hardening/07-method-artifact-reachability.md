# 07 — Test-method artifact reachability and minimal data builders

**What to build:** Every retained decision table, partition, state machine, journey, metamorphic oracle and data builder has a real live-spec consumer and adds demonstrable test-design value; unreachable artifacts and stale documentation are removed.

**Blocked by:** 06 — Business-level steps and locator hygiene.

**Seams:** TypeScript static; test-design review; Playwright REST/E2E discovery

**Status:** ready-for-agent
**Category:** enhancement

## Implementation guidance

- Compute reachability from configured specs, including indirect imports.
- Review the known candidates: idempotency matrix, merchant access matrix, filter inclusion, checkout mode/outcome, create-order journey and payment-order draft.
- Connect an artifact only when it removes duplication or expresses a real technique better than inline rows.
- Delete unreachable artifacts and update method README/copy maps together.
- Keep a builder only when defaults/variants are used broadly; prefer a plain typed factory for two trivial callsites.
- Do not create artificial tests solely to retain a pattern example.

## Immutable acceptance IDs

- `T07-A01` — A repeatable reachability inventory starts at all configured live specs.
- `T07-A02` — Every retained method artifact has at least one real configured spec consumer.
- `T07-A03` — Retained artifacts state which technique and oracle they provide.
- `T07-A04` — Unreachable artifacts and their stale documentation entries are removed.
- `T07-A05` — The payment draft builder is either demonstrably reused for meaningful variants or replaced with a smaller typed factory/literal.
- `T07-A06` — No coverage or catalog ID is silently deleted; removals are reconciled with documentation.
- `T07-A07` — Strict POM typecheck, lint and all affected discovery commands are green.

## Validation and verification

Apply the goal's shared loop. Ticket-specific proof compares discovery before/after and uses `ponytail-review` to require net simplification.
