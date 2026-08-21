# Ops Wave 2 — slice overlay

Mutable. Agents update this after each `PW-OPS-Txx`. Do not put this content in chat prompts.

| Field | Value |
|---|---|
| Milestone | `playwright-ops-wave-2` |
| Status | COMPLETE |
| Last closed | `PW-OPS-T19` |
| Next | — |
| Blocked | — |
| Last verify | Fale 7–9 T16–T19: RA-OPS-180…185 BUILD SUCCESS (V36 JSONB on `tenants`). Live POM E2E-180…184 (rule configurator), E2E-210…213 (locale), E2E-220…224 + API-070 (evidence gallery). `@nuxt/ui` 4.7.1. No V37. No auto-capture worker. No new Keycloak composites. T20–T22 already DONE. |

Roadmap: `status/roadmaps/playwright-ops-wave-2/`  
Catalog: `docs/testing/ops-wave-2-interaction-lab/`  
Skill: `ops-wave-2-implement`

Locked: Flyway **V31 = merchant contact**; **V32 = support_cases**; **V33 = refund_challenges**; **V34 = ops notifications**; **V35 = user_saved_views** (iam); **V36 = tenants.payment_policy JSONB**. V26–V30 unused. 412 not 409 for stale If-Match. No `page.route` / `routeWebSocket`. G3 websocket granted (SimpleBroker). AUTOMATIC vs PIN: `.codex/ops-wave-2-payment-policy.md`. Wave 2 complete — do not open T20+.
