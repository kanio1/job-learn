---
name: epic-e3-network-lab
parent: browser-session-visual-network-lab
epic: E3
tasks: [MRL-T08, MRL-T09, MRL-T15, MRL-T16]
last_updated: 2026-08-13
---

# Epic E3 — Network Fault Lab

**Cel produktowy:** Error Lab 2.0 — przyciski, które generują ruch do przechwycenia (PayU 5xx retry, lie status, CORS).  
**Cel dydaktyczny:** `page.route` fulfill/abort/continue/`route.fetch`, HAR, `waitForResponse`, split mock vs POM.

**Połączenia:** [error-lab.vue](../../../../apps/frontend/app/pages/error-lab.vue), `tests/e2e/ui/error-lab-network.spec.ts`, Playwright [Network](https://playwright.dev/docs/network).  
CPL: 400 HMAC no-retry vs 503 retry — E3 pokazuje **przeglądarkową** stronę tego samego.

---

## Story E3-S1 — 503 then 200  
**Task:** `MRL-T08` · P0 · FR-N01 FR-N07

### Jako / chcę / aby
Jako SDET chcę przycisk, który za pierwszym razem dostaje 503 `Retry-After`, a retry 200.

### Acceptance criteria
- [ ] BFF lub Spring lab: stateful counter per correlation/session.
- [ ] UI Error/Network Lab: `data-testid="network-lab-trigger-503-retry"`.
- [ ] Problem+json pierwsze trafienie; drugie JSON ok.
- [ ] Mocked PW: stateful `route.fulfill` (count===0 → 503 else 200) — nauka bez backendu.
- [ ] POM: **zakaz fulfill**; `waitForResponse` dwukrotnie przeciwko żywemu BFF.
- [ ] Copy: CPL HMAC 400 **nie** retry; 503 **tak**.

### Learning
- `PW:` sequential route mocks.
- `HTTP:` Retry-After, 503 vs 400.

---

## Story E3-S2 — Abort, offline, lie body  
**Task:** `MRL-T09` · P0 · FR-N02 FR-N03 FR-N04

### Jako / chcę / aby
Jako integrator PayU chcę zobaczyć UI gdy PSP „leży” i gdy body kłamie jak `continueUrl`.

### Acceptance criteria
- [ ] Scenariusz offline: UI ErrorState gdy `context.setOffline(true)` (tylko test; przycisk „simulate abort” woła endpoint który zwraca connection-close **albo** dokumentuje że abort jest tylko w PW `route.abort()`).
- [ ] Preferencja produktowa: przycisk **Abort demo** woła `/api/network-lab/slow` (delay) + UI timeout copy; prawdziwy `route.abort()` zostaje w mocked spec.
- [ ] Lie: GET lab fulfillment `{ "status": "success" }` przy DB/oracle `AWAITING_PAYMENT` — karta „Untrusted body” (jak CPL return).
- [ ] Mocked: `route.continue({ headers })` usuwa `Idempotency-Key` → 428/400 problem.
- [ ] Oracle pieniędzy **nigdy** z samego JSON `status=success`.

### Learning
- `PW:` abort, setOffline, continue headers, fetch+override body.
- `HTTP:` lie response = analog continueUrl.

---

## Story E3-S3 — HAR record / replay  
**Task:** `MRL-T15` · P1 · FR-N05

### Jako / chcę / aby
Jako SDET chcę nagrać booking/session flow do HAR i odtworzyć go w mocked teście.

### Acceptance criteria
- [ ] Dokument: `recordHar: { path, urlFilter: '**/api/**' }`.
- [ ] Przykładowy HAR **nie** zawiera tokenów (redact Authorization/Cookie) — albo HAR gitignored + fixture zsanitizowany.
- [ ] Spec replay: `page.routeFromHAR(...)`.
- [ ] Zakaz commitowania raw live HAR z `nuxt-session`.

### Learning
- `PW:` HAR, service worker caveat (block SW jeśli events znikają).

---

## Story E3-S4 — CORS credentials  
**Task:** `MRL-T16` · P1 · FR-N06

### Jako / chcę / aby
Jako HTTP expert chcę zobaczyć preflight i `Access-Control-Allow-Credentials`.

### Acceptance criteria
- [ ] Lab OPTIONS na `/api/network-lab/cors-cookie`.
- [ ] Credentials origin allowlist localhost:3000.
- [ ] Copy: hosted public **bez** credentials; dashboard **z**.
- [ ] RA/PW-API: OPTIONS 200 + Allow-Headers.
- [ ] Nie zmieniać globalnego CORS payment API bez potrzeby — lab matcher.

### Learning
- `HTTP:` ACAO nie może być `*` przy credentials.
- `CK:` cookie cross-origin nie poleci bez tej pary.
