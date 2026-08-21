---
name: playwright-ops-wave-2-learning-map
parent: playwright-ops-wave-2
last_updated: 2026-08-20
---

# Mapa lekcja TypeScript / Playwright → story

Curriculum senior SDET na **żywym** stosie. Świadomie **bez** lekcji `page.route` i `routeWebSocket` (zakaz laby).

| Lekcja | Feature | Story | Test IDs |
|---|---|---|---|
| Dwa `browser.newContext` + dwa `storageState` | Concurrent merchant | E1-S2 | PW-OPS-SEC-020 |
| 412 vs 409 vs 428 | If-Match merchant | E1-S1 | RA-OPS-050…055 |
| `UTabs` Your vs Latest + `toMatchAriaSnapshot` | Conflict workspace | E1-S3 | PW-OPS-E2E-130 |
| `page.goBack` + `getByRole('dialog')` Stay | Dirty NuxtLink/Back | E2-S1 | PW-OPS-E2E-160 |
| `page.on('dialog')` + `close({ runBeforeUnload: true })` | Native beforeunload | E2-S2 | PW-OPS-E2E-163 |
| Guard nie wysyła PATCH (`waitForResponse` timeout / count 0) | Dirty no-write | E2-S3 | PW-OPS-E2E-164 |
| Union `SupportCaseStatus` + legal transitions | Case machine | E3-S1 | RA-OPS-110 |
| Menu Move P0; `dragTo` P1 | Kanban | E3-S3 | PW-OPS-E2E-110, 111 |
| Optimistic UI + rollback na 412 | Kanban conflict | E3-S4 | PW-OPS-E2E-112 |
| Per-item result, nie jeden toast | Bulk partial | E4-S1 | PW-OPS-E2E-150 |
| Retry body **tylko** failed IDs | Bulk contract | E4-S2 | PW-OPS-API-031 |
| `UPinInput` `pressSequentially` / paste / Backspace | Step-up PIN | E5-S2 | PW-OPS-E2E-170…174 |
| `page.clock` TTL | PIN expiry | E5-S3 | PW-OPS-E2E-175 |
| HTTP 429 lockout | Rate limit | E5-S4 | RA-OPS-174 |
| `page.on('websocket')` + `framereceived` | Live feed | E6-S2 | PW-OPS-E2E-120 |
| Dedup `eventId` / sort `occurredAt` | Out-of-order WS | E6-S3 | PW-OPS-E2E-121…123 |
| `setOffline` reconnect chip | Disconnect | E6-S4 | PW-OPS-E2E-124 |
| Badge count 3→4→3 persist | Notifications | E7-S1 | PW-OPS-E2E-190 |
| localStorage then API; inny user absent | Saved views | E8-S1/S2 | PW-OPS-E2E-140…146 |
| Ctrl+K Fuse + ArrowDown/Enter | Global search | E9-S1 | PW-OPS-E2E-200 |
| Last `waitForResponse` wins | Search race | E9-S2 | PW-OPS-E2E-202 |
| Role A widzi, B nie | Search RBAC | E9-S3 | PW-OPS-SEC-040 |
| BVA 0/1/99/100 + disabled when OFF | Rule configurator | E10-S2 | PW-OPS-E2E-180 |
| Slider Home/End ARIA | Keyboard form | E10-S3 | PW-OPS-E2E-183 |
| `test.use({ locale })` + role locators | i18n | E11-S1 | PW-OPS-E2E-210 |
| `Intl` amount/date EN/PL/SV | Formatting | E11-S2 | PW-OPS-E2E-211 |
| Carousel keyboard / broken media | Evidence gallery | E12-S1 | PW-OPS-E2E-220 |
| Discriminated unions / `satisfies` / Zod | TS 6 w schema + events | E1–E7 | typecheck + oxlint |
| Component objects vs page | POM model | E3, E7, E8 | tests-pom/pages/components |

Co para z M360 daje operatorowi i czemu RA ≠ E2E: [value-and-learning](../../../docs/testing/m360-ops-wave-2-value-and-learning.md). Flow: [00-business-flows](../../../docs/testing/ops-wave-2-interaction-lab/00-business-flows.md).

Poza mapą (non-goal): GraphQL, `routeWebSocket` mock, WebAuthn, Keycloak OTP, visual pixel chart jako jedyna asercja, trzy pełne projekty PW na cały suite.
