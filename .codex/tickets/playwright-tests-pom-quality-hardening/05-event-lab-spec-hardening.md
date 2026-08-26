# 05 — Event Lab Playwright hotspot hardening

**What to build:** Event Lab BFF, operator UI and security journeys keep their real Kafka behavior while using correct types, small scenario setup helpers, explicit business steps and no unused or duplicated setup.

**Blocked by:** 02 — Typed BFF results and fixture lifecycle contracts; 03 — Single-shot actions, POM boundaries, and visible business oracles; 04 — Config diagnostics, discovery matrix, and auth secret discipline.

**Seams:** Playwright REST/BFF; Playwright E2E; live Kafka Event Lab

**Status:** ready-for-agent
**Category:** enhancement

## Implementation guidance

- Start by recording the inherited dirty diff; preserve all existing Event Lab changes.
- Fix request/response types through canonical Playwright imports and safe parsing, not casts.
- Extract only the repeated create → ETag → authorize → poll setup. Return a small typed scenario value and leave DLT, duplicate, auth and tenant assertions in specs.
- Add 2–5 business steps per long journey.
- Split the spec only if API, operator E2E and security responsibilities become clearer and config discovery remains complete.
- Keep real Kafka and BFF paths; no route mocks, direct broker calls from browser, sleeps, or hidden poisoning.

## Immutable acceptance IDs

- `T05-A01` — No unresolved dynamic `playwright.Request` type, unused fixture/variable, or newly introduced cast remains.
- `T05-A02` — Repeated lifecycle setup has one typed helper with no hidden business oracle.
- `T05-A03` — API, operator UI and security responsibilities are easy to identify in reports.
- `T05-A04` — Long Event Lab journeys contain meaningful business-level steps.
- `T05-A05` — No `page.route`, `route.fulfill`, `waitForTimeout`, `kafkajs`, or direct browser-to-broker behavior exists.
- `T05-A06` — Event Lab touched files have zero lint errors and no remaining avoidable warnings.
- `T05-A07` — Strict POM typecheck and Event Lab discovery are green.
- `T05-A08` — Targeted Event Lab Playwright REST/E2E/security tests are green on the Kafka stack; otherwise status is `NOT_RUN`, not PASS.

## Validation and verification

Apply the goal's shared loop. Ticket-specific proof uses the real Kafka overlay and env-only auth, records runtime counts and confirms the existing poison/payment-state contract.
