---
name: epic-e4-payu-mirrors
parent: browser-session-visual-network-lab
epic: E4
tasks: [MRL-T18, MRL-T19, MRL-T20, MRL-T21, MRL-T22, MRL-T23]
last_updated: 2026-08-13
---

# Epic E4 — PayU protocol mirrors (CPL extensions)

**Cel produktowy:** lustra z [PayU auth-and-order](https://developers.payu.com/europe/docs/payment-flows/auth-and-order/) **na checkoutlab**, nie nowy PSP.  
**Cel dydaktyczny:** GET-without-body, locale redirect, refund IPN, expiry UI, iframe widget, grant contrast.

**Połączenia:** CPL E1–E4; `MockPspClient` **nietknięty**; hosted new-tab zostaje.

---

## Story E4-S1 — GET with body → 403  
**Task:** `MRL-T18` · P1 · FR-P01

### Jako / chcę / aby
Jako REST client chcę 403 gdy GET ma body, jak PayU cytując RFC 9110.

### Acceptance criteria
- [ ] `GET /api/checkout-lab/sessions/{id}` (lub lab twin) z body → 403 problem `get_with_body` / PayU-like `ERROR_VALUE_INVALID`.
- [ ] GET bez body → 200 jak dziś.
- [ ] RA: RestAssured `.body(...)` na GET.
- [ ] Copy Error/Mirror Lab: „disable empty body on GET”.

### Learning
- `HTTP:` RFC 9110 §9.3.1.
- `REST:` klienci którzy zawsze serializują `{}`.

---

## Story E4-S2 — lang on redirectUri  
**Task:** `MRL-T19` · P1 · FR-P02

### Jako / chcę / aby
Jako płatnik chcę hosted page w `pl`/`en` z query `lang` (PayU `redirectUri&lang=`).

### Acceptance criteria
- [ ] Create session honoruje `buyer.language` **lub** query na Location.
- [ ] Hosted UI przełącza copy CTA (dwa locale, nie i18n framework-wide required).
- [ ] Nie mobile RTL jako AC.
- [ ] Visual tile hosted CTA może mieć `lang=en` variant (E2 opcjonalnie).

### Learning
- `HTTP:` query na Location po 302.
- CPL non-goal locale — tutaj **świadomie** odblokowane jako lab.

---

## Story E4-S3 — Refund notify same URL  
**Task:** `MRL-T20` · P1 · FR-P03

### Jako / chcę / aby
Jako merchant chcę IPN refundu na **ten sam** `notifyUrl` co order (PayU docs).

### Acceptance criteria
- [ ] Po lab refund (CPL session COMPLETED → refund command) stub emituje event `REFUND_*` na ten sam receiver.
- [ ] HMAC + eventId dedup jak order notify.
- [ ] Inspector pokazuje typ REFUND vs ORDER.
- [ ] 202 ACK; fulfillment/refund oracle w DB, nie UI thank-you.

### Learning
- `HTTP:` ten sam path, inny payload.
- `KC:` nadal **bez** JWT na notify.

---

## Story E4-S4 — Payment link expiry UI  
**Task:** `MRL-T21` · P1 · FR-P04

### Jako / chcę / aby
Jako płatnik widzę „link expired” (CPL FR-10 dociągnięcie UI + visual + clock).

### Acceptance criteria
- [ ] Hosted po `validityUntil`: kafelek expired, simulate 409 `expired_link`.
- [ ] PW: clock / `POST clock` (istniejący CPL ops), nie sleep.
- [ ] Visual optional tile expired (stabilny, nie żywy timer w PNG).

### Learning
- `PW:` clock + 409.
- CPL T22 może już mieć API — ta story = UI/visual/PW luka.

---

## Story E4-S5 — Same-origin iframe widget  
**Task:** `MRL-T22` · P1 · FR-P05

### Jako / chcę / aby
Jako merchant PayU white-label chcę widget w iframe na desktopie (`frameLocator`), obok new-tab hosted.

### Acceptance criteria
- [ ] Strona `/admin/checkout-lab/widget` z `<iframe src="/psp/checkout/widget/{id}">` **same origin**.
- [ ] Approve w iframe zmienia fulfillment (oracle API).
- [ ] New-tab hosted **nie** usunięty.
- [ ] Brak cross-origin bank iframe (to zabiłoby lab bez specjalnego CSP).
- [ ] `data-testid` na frame + przyciski wewnątrz.

### Learning
- `PW:` `page.frameLocator('#mrl-psp-frame')`.
- Nie mylić z F-D2 multi-tab.

### Stop
- Nie mobile WebView.
- Nie 3DS challenge iframe.

---

## Story E4-S6 — OAuth grant contrast  
**Task:** `MRL-T23` · P1 · FR-P06

### Jako / chcę / aby
Jako architekt chcę panel: `client_credentials` vs OIDC PKCE vs lab `trusted_merchant` (`extCustomerId`).

### Acceptance criteria
- [ ] UI trzy kolumny: kto woła, Content-Type, gdzie token, czego **nie** robić.
- [ ] `trusted_merchant` stub: form fields email + extCustomerId → lab Bearer scoped; **nie** Keycloak user.
- [ ] 401 bez `application/x-www-form-urlencoded` (jak PayU).
- [ ] Zakaz użycia KC JWT na `/notify`.

### Learning
- `KC:` trzy granty, trzy światy.
- `HTTP:` form vs JSON.
