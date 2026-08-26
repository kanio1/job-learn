# 04 — Config diagnostics, discovery matrix, and auth secret discipline

**What to build:** Every active Playwright configuration has useful failure artifacts, explicit discovery evidence, and environment-only passwords for every account including worker managers.

**Blocked by:** 01 — Strict compiler gate and runtime POM contracts; 03 — Single-shot actions, POM boundaries, and visible business oracles.

**Seams:** Playwright configuration/discovery; auth setup; TypeScript static

**Status:** ready-for-agent
**Category:** bug

## Implementation guidance

- Keep stateful live tests at zero retries and retain trace on failure in every active config.
- Preserve screenshots/video on failure, worker cap, serial projects, dependencies and `forbidOnly`.
- Require every password with the existing environment helper; usernames may retain public defaults.
- Add a documented discovery matrix for main, visual, TLS, RLS flag-off, RLS Spring-off, Mirror flag-off and learner configurations.
- Verify meaningful projects detect tests; do not treat a zero-test learner copy as a product regression when it is intentionally empty.
- Avoid a shared config abstraction unless it reduces conditions and makes overlay differences clearer.

## Immutable acceptance IDs

- `T04-A01` — No active config combines zero retries with `on-first-retry` trace.
- `T04-A02` — The selected policy is `retries: 0` plus `trace: retain-on-failure` for the live suite.
- `T04-A03` — Every `PLAYWRIGHT_*_PASSWORD` is required from env; no password falls back to username.
- `T04-A04` — Missing-password failures name the variable but never reveal values.
- `T04-A05` — Generated auth state is still ignored by Git and no token/state file enters the diff.
- `T04-A06` — Discovery commands are green for all active configs and expected projects have non-zero tests.
- `T04-A07` — All 72 live specs remain reachable by at least one config.
- `T04-A08` — Strict POM typecheck and config-only lint are green.

## Validation and verification

Apply the goal's shared loop. Ticket-specific proof: complete discovery matrix, one auth setup and one worker-world test when credentials exist, plus a safe retained-trace check that leaves no tracked artifact.
