# Ops Wave 2 — versioned stack research (Firecrawl + Context7)

Status: DESIGNED_NOT_STARTED  
Date: 2026-08-20  
Context: second milestone-PW after Merchant 360. Live stack only.

## Answer

Pinned lab versions remain: Playwright **1.61.0**, `@nuxt/ui` **4.7.1**, Nuxt **4.4.6**, TypeScript **6.0.3**, PostgreSQL **18**, Spring Boot **4.0.6** / Modulith **2.0.6**, Java **25**.

Live POM forbids `page.route` / `route.fulfill` **and** `page.routeWebSocket` / `browserContext.routeWebSocket`. Playwright 1.61 `WebSocketRoute` **does not connect to the server** unless `connectToServer()` is called — that is a mock. Live oracle is `page.on('websocket')` + `waitForFrameReceived` against a real same-origin BFF WebSocket.

Optimistic lock in this lab is HTTP **412** (stale `If-Match`), not 409. 409 stays duplicate / idempotency / dual-control self-approve. 428 = missing `If-Match`.

Merchant 360 reserved Flyway **V23–V30**. This milestone starts at **V31**.

`UPinInput` exists in 4.7.1 (`length`, `complete` emit, OTP/paste). Prop `separator` is **4.9+** — do not use. `UDashboardSearch` wraps CommandPalette with Fuse.js; `searchDelay` landed in **4.7.0**. `ULocaleSelect` is meant to be used with `@nuxtjs/i18n` (new dependency — explicit approval in E11). Confirm `UCarousel` export in `node_modules/@nuxt/ui@4.7.1` at implementation time.

## Why it matters here

The Wave 2 analysis treated `routeWebSocket` as the way to inject duplicate / out-of-order / malformed frames. In this lab that would violate the live-stack rule the same way `page.route` does. Duplicate/out-of-order/malformed must come from a real `POST /api/ops/feed/inject` (platform authority) or from two real lifecycle actions.

The analysis also mixed 412 and 409 for concurrent merchant edit, assumed a second Kanban on payment statuses (already planned in Merchant 360 E5), and cited live Nuxt UI docs that include 4.9/4.10 APIs.

## Project impact

- WebSocket is same-origin through Nitro (`/api/ops/feed`); sealed session attaches JWT. Never put the access token in the browser or in a WS query string.
- New Modulith modules `support` and `ops` (not OPEN). Add Flyway folders and `spring.flyway.locations`.
- New backend dependency (approval required at implementation): `spring-boot-starter-websocket` on Boot 4.0.6. In-memory SimpleBroker; no Kafka / outbox.
- New frontend dependency **only** for E11: `@nuxtjs/i18n`. Do not bump `@nuxt/ui`.
- Keycloak: no new composite roles. Add realm roles `support:*`, `ops:*`, `notifications:read` into existing composites. Second writer for two `BrowserContext`s is existing `platform.operator`.
- PIN step-up is **not** Keycloak OTP. It extends `payment_refund_approvals` with a hashed challenge row.

## Test impact

| Claim | Test implication |
|---|---|
| `routeWebSocket` = mock | Forbidden in `tests-pom`. Learner copies may demo it; product suite must not. |
| Inspect live WS | `page.on('websocket')`, `ws.waitForEvent('framereceived')` / `waitForFrameReceived` |
| Duplicate / malformed | Inject HTTP 200 then assert UI; do not rewrite frames in Playwright |
| Disconnect | `browserContext.setOffline(true/false)` or server-side close inject — not `route.abort` |
| Two users | `browser.newContext` + distinct `storageState` (`platform.admin` vs `platform.operator`) |
| `beforeunload` | `page.on('dialog')` + `page.close({ runBeforeUnload: true })`; in-app leave uses `UModal`, not native dialog |
| Locale | One Playwright project `locale` + `test.use({ locale, timezoneId })` — do not triple the whole suite |
| PinInput | `pressSequentially`, paste, Backspace; `complete` event; length 6; no `separator` |
| Drag | P0 = keyboard/menu Move; `dragTo` is P1 and may be tagged flaky |
| 412 not 409 | Concurrent merchant save asserts 412 + conflict tabs |

## Sources

- [Playwright WebSocketRoute](https://playwright.dev/docs/api/class-websocketroute) — default is mock; `connectToServer()` required to reach the real server. Since v1.48; lab is 1.61.0.
- [Playwright WebSocket](https://playwright.dev/docs/api/class-websocket) — `framereceived` / `framesent` / `close` for inspection without routing.
- [Playwright Network — WebSockets](https://playwright.dev/docs/network#websockets) — `page.on('websocket')` for live frames; mock APIs are a separate section.
- [Playwright Browser contexts](https://playwright.dev/docs/browser-contexts) — isolated multi-user in one test.
- [Playwright Dialogs](https://playwright.dev/docs/dialogs) — `beforeunload` + `page.close({ runBeforeUnload: true })`.
- [Playwright Emulation — locale / timezone](https://playwright.dev/docs/emulation#locale--timezone) — `locale` / `timezoneId` on config, `test.use`, or `newContext`.
- [Nuxt UI PinInput](https://ui.nuxt.com/docs/components/pin-input) — `length`, `otp`, `complete`; `separator` marked **4.9+**.
- [Nuxt UI DashboardSearch](https://ui.nuxt.com/docs/components/dashboard-search) — CommandPalette + Fuse; shortcut `meta_k`; `searchDelay` in 4.7.0.
- [Nuxt UI LocaleSelect](https://ui.nuxt.com/docs/components/locale-select) — designed to pair with `@nuxtjs/i18n` + `@nuxt/ui/locale`.
- [Nuxt UI Carousel](https://ui.nuxt.com/docs/components/carousel) — Embla; `select` emit. Confirm 4.7.1 export at implement time.
- [PostgreSQL 18 CREATE INDEX](https://www.postgresql.org/docs/18/sql-createindex.html) — btree default; `IF NOT EXISTS`; `CONCURRENTLY` not in a transaction (Flyway).
- Repo: `apps/frontend/playwright.pom.config.ts` already documents no `page.route`. Dual-control refund lives in payment V21, not Mirror Lab.

## Uncertainty / follow-up

- Nitro 4.4.6 WebSocket proxy (`defineWebSocketHandler`) vs a ticketed Spring WS behind BFF HTTP upgrade — pick at E6 implement; token must stay server-side.
- `UCarousel` / `UTimeline` / `UChip` export list in **4.7.1** (live docs may be 4.10). Fallback: dialog + next/prev buttons; `ol` timeline; `UBadge` for chip.
- Spring Boot 4.0.6 WebSocket starter API vs Framework 7 package names — verify against Boot 4.0.6 javadoc at implement; do not copy Boot 3 samples blindly.
- Whether M360 T13 (merchant ETag) lands first: E1 of this milestone **includes** the HTTP ETag foundation if M360 E4 is still DESIGNED. Implement once.
- Whether M360 T17 (entity search) lands first: E9 skips the basic live GET and only adds RBAC groups + last-wins if T17 already exists.
