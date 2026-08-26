# 06 — Business-level steps and locator hygiene

**What to build:** Long live journeys produce readable Playwright reports with a small number of business-level steps, and touched locators prefer accessible contracts without mechanically removing stable test IDs.

**Blocked by:** 03 — Single-shot actions, POM boundaries, and visible business oracles; 05 — Event Lab Playwright hotspot hardening.

**Seams:** Playwright E2E; accessibility/locator review; test reporting

**Status:** ready-for-agent
**Category:** enhancement

## Implementation guidance

- Rank long/multi-stage journeys and improve them in small batches.
- Use 2–5 steps for arrange, user action, observable integration effect and final oracle.
- Keep short single-state tests unwrapped.
- Prefer role/name/label for interactive controls; retain stable test IDs for complex widgets and state containers.
- Replace the ancestor XPath in Notification Center with an owned semantic or test-id locator chain.
- Do not introduce mass decorators or wrap each page-object method.

## Immutable acceptance IDs

- `T06-A01` — Every selected long journey has 2–5 business-level steps with domain language.
- `T06-A02` — No step merely restates a click/fill implementation detail.
- `T06-A03` — Short tests remain concise and are not padded with steps.
- `T06-A04` — The Notification Center XPath is removed without weakening locator strictness.
- `T06-A05` — Touched interactive controls use role/name/label where the accessible contract exists.
- `T06-A06` — Stable test IDs for complex states are preserved where they are the clearest contract.
- `T06-A07` — Targeted specs, strict POM typecheck and POM-only lint are green.
- `T06-A08` — Playwright report/trace shows useful step boundaries for at least one representative long journey.

## Validation and verification

Apply the goal's shared loop per small batch. Ticket-specific proof is locator strictness plus visible behavior, never a raw role/test-id count.
