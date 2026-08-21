# 03 — Katalog Playwright E2E (live POM)

Warstwa: Chromium `tests-pom`, **zero fulfill, zero routeWebSocket**. Stos: `scripts/dev-stack.sh --app`.  
Pokrycie: **designed**.

Page objects: rozszerzyć `SupportPage`, `PaymentDetailPage`; dodać component objects z [README roadmap](../../status/roadmaps/playwright-ops-wave-2/README.md). Locatory: `getByRole` → `getByLabel` → `getByTestId`. Import `test`/`expect` z `tests-pom/fixtures/index.ts`.  
Curriculum: [value-and-learning](../m360-ops-wave-2-value-and-learning.md) · flow: [00](00-business-flows.md).

### Czego uczy ta warstwa (Ops E2E)

Klasy: dwa `storageState`, dirty guard, Kanban spraw, bulk partial, PIN, żywy WS (`page.on('websocket')` nie mock), saved views bez JWT, slider ARIA, `locale` project, carousel. **Nie** uczy payment-status Kanban (M360 E2E-090) ani pełnej macierzy 400 (RA-OPS).

---

## A. Concurrent edit / security

### PW-OPS-SEC-020 — Dwa contexty, 412

| | |
|---|---|
| Pokrycie | designed |
| UC | UC-OPS-14 |
| Spec | `merchants-concurrent.spec.ts` (nowy) |
| Kroki | `multiUser`: admin PATCH phone 200; operator PATCH address stale ETag |
| Asercje | operator response 412; conflict dialog visible |

### PW-OPS-SEC-021 / 022 — Discard / Reload

| | |
|---|---|
| Asercje | Discard → form = server, no second stale PATCH; Reload → GET v8 |

### PW-OPS-E2E-130…132 — Conflict tabs

| | |
|---|---|
| Kroki | po 412: tabs Your / Latest; Escape |
| Asercje | `toMatchAriaSnapshot`; Escape bez zapisu |

### PW-OPS-SEC-040 / 041 — Search RBAC

| | |
|---|---|
| UC | UC-OPS-33 |
| Asercje | manager: brak grupy Merchants; readonly: brak Create merchant |

---

## B. Unsaved guard

### PW-OPS-E2E-160 — Back + Stay

| | |
|---|---|
| UC | UC-OPS-16 |
| Kroki | dirty phone; `page.goBack()`; Stay |
| Asercje | URL nadal edit; dialog hidden |

### PW-OPS-E2E-161 — NuxtLink Discard

| | |
|---|---|
| Asercje | list URL; GET without local phone |

### PW-OPS-E2E-162 — czysty form bez dialogu

### PW-OPS-E2E-163 — beforeunload

| | |
|---|---|
| Kroki | `page.on('dialog')`; `close({ runBeforeUnload: true })`; dismiss |
| Asercje | `dialog.type() === 'beforeunload'` |

### PW-OPS-E2E-164 — Stay nie wysyła PATCH

| | |
|---|---|
| Asercje | request buffer PATCH merchant length 0 |

---

## C. Support Kanban

### PW-OPS-E2E-110 — Menu Move + reload

| | |
|---|---|
| UC | UC-OPS-18 |
| Spec | `support-kanban.spec.ts` |
| Kroki | factory case NEW; menu Move IN_PROGRESS; `waitForResponse` PATCH 200; reload |
| Asercje | karta w kolumnie IN_PROGRESS |

### PW-OPS-E2E-111 — dragTo (P1, `@flaky` OK)

### PW-OPS-E2E-112 — 412 rollback + toast

| | |
|---|---|
| UC | UC-OPS-19 |
| Kroki | drugi context przesuwa; drag; 412 |
| Asercje | karta wraca; toast |

### PW-OPS-E2E-113 — aria snapshot board

### PW-OPS-E2E-114 — illegal drop 409

---

## D. Bulk partial

### PW-OPS-E2E-150 — counts per item

| | |
|---|---|
| UC | UC-OPS-20 |
| Asercje | `successCount` text; `failureRows` count |

### PW-OPS-E2E-151 — progress then modal

### PW-OPS-E2E-152 / 153 — Retry failed ids only

---

## E. Step-up PIN

### PW-OPS-E2E-170 — pressSequentially

| | |
|---|---|
| UC | UC-OPS-23 |
| POM | `PinChallengeComponent` |
| Kroki | refund > 100000 minor; pin z `BffClient` (nie DOM) |
| Asercje | verify 200 path |

### PW-OPS-E2E-171…174 — wrong / paste / Backspace / arrows

### PW-OPS-E2E-175 — `page.clock` expire

### PW-OPS-E2E-176 — 5 fail → 429 UI

---

## F. Live feed

### PW-OPS-E2E-120 — capture → framereceived → row

| | |
|---|---|
| UC | UC-OPS-25 |
| Kroki | `page.on('websocket')`; BffClient capture; `waitForFrameReceived` |
| Asercje | row PO ref + CAPTURED; **nie** `routeWebSocket` |

### PW-OPS-E2E-121 — duplicate eventId jeden wiersz

### PW-OPS-E2E-122 — out-of-order sort occurredAt

### PW-OPS-E2E-123 — malformed toast, still connected

### PW-OPS-E2E-124 — `setOffline` chip

### PW-OPS-E2E-125 — reconnect no dup from recent

---

## G. Notifications

### PW-OPS-E2E-190…194 — badge 3→4→3, popover, persist, read-all

Producer: inject/assign case (actionable), **nie** każdy CAPTURE.

---

## H. Saved views

### PW-OPS-E2E-140 / 141 — localStorage restore + storage-safety

### PW-OPS-E2E-142 / 143 — login restore / other user absent

### PW-OPS-E2E-144…147 — URL sync, Back, default star, column hide

---

## I. Global search

### PW-OPS-E2E-200 / 201 — Ctrl+K keyboard

### PW-OPS-E2E-202 — last waitForResponse wins

### PW-OPS-E2E-203 — denied empty no crash

---

## J. Rule configurator

### PW-OPS-E2E-180…184 — disabled, required, BVA 0/100, Home/End, radios

Oracle slider: `aria-valuenow`, nie piksele.

---

## K. Locale

### PW-OPS-E2E-210…213 — pl/sv/en format + persist

Spec w `specs/locale-workspace.spec.ts`; `test.use({ locale, timezoneId })`. Nie 3 pełne projekty suite.

---

## L. Evidence gallery

### PW-OPS-E2E-220…224 — next, keyboard, download, invalid id, BOLA 404
