---
name: epic-e5-step-up-pin
parent: playwright-ops-wave-2
epic: E5
tasks: [PW-OPS-T09]
last_updated: 2026-08-20
---

# Epic E5 — Step-up Approval PIN (#17)

**Cel produktowy:** refund powyżej €1,000 wymaga 6-cyfrowego kodu z TTL, nie tylko dual-control approve.  
**Cel dydaktyczny:** keyboard PinInput, paste, expiry `page.clock`, 429 lockout.

**Gate:** dual-control V21 już jest. **Nie** Mirror Lab `X-Lab-Step-Up`. **Nie** Keycloak OTP.

Nuxt UI: `UPinInput` `:length="6"` — **bez** `separator` (4.9+). `UProgress` na timer. Event `complete`.  
POM: `PinChallengeComponent`.

Próg lab: `amountMinor > 100000` (1000.00 w skali 2). Waluta orderu ignorowana (uproszczenie — zapisać w AC).

---

## Story E5-S1 — Challenge REST

**Task:** `PW-OPS-T09` · P0

### Jako / chcę / aby

Jako checker platformy przy high-value refund wpisuję kod z challenge, nie zatwierdzam w ciemno.

### Business case

`BC-OPS-17` — Step-up na kwotę. Dual-control zostaje (inny subject). PIN jest **dodatkowym** czynnikiem na approval PENDING gdy kwota > próg.

### Use case

`UC-OPS-22`

```text
Refund <= 1000.00  → istniejący confirm / dual-control bez PIN
Refund >  1000.00  → POST /api/merchants/{mid}/payment-orders/{pid}/refund-challenges
                   → POST .../refund-challenges/{id}/verify { "pin": "482193" }
```

PIN generowany serwerowo, zwracany **raz** w 201 body `pin` **tylko w profilu `dev`/`test`** (lab). W `prod` profile pin tylko e-mail/log — ten lab nie ma maila: **dev/test zwracają pin w JSON** + UI pokazuje go w debug panel **masked** opcjonalnie. POM czyta pin z `BffClient.createChallenge().pin` (REST precondition), nie z DOM jeśli uznamy wyciek. **Decyzja implementacyjna (rekomendacja):** testy RA/PW REST dostają pin w 201; E2E wpisuje pin z API fixture, **nie** scrapuje UI. UI ma pole do wpisania, nie wyświetla plaintext (operator lab dostaje pin z ApiDebugPanel w `dev` only — mask w prod).

### SQL

[01-infra B.3](../01-infra-postgres-keycloak-stack.md) V33. Hash only.

### Domain rules

- Challenge tylko gdy approval PENDING i amount > próg. Inaczej 400 `pin_not_required`.
- TTL 90s (lab; `page.clock`).
- 5 złych verify → `locked_until` now()+5min, dalsze verify **429** `rate_limited`.
- Sukces: `verified_at` set; drugi verify → 409 `already_verified`.
- Expired → 400 `expired` (nie 401).
- Maker nie verify (dual-control self-approve 409 zostaje na approve; PIN verify authority = checker `platform:payments:lifecycle`).
- Approve refund **wymaga** verified challenge gdy kwota > próg; bez tego 412/400 `step_up_required`.

### Acceptance criteria

- [ ] BFF proxy + Zod.
- [ ] Problem Details jak reszta laby.
- [ ] Audit na create/verify/lock.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-OPS-170 | RA | amount ≤ 100000: create challenge 400 pin_not_required |
| RA-OPS-171 | RA | amount 100001: 201 challenge; hash ≠ pin |
| RA-OPS-172 | RA | verify correct → 200; approval nadal PENDING aż approve |
| RA-OPS-173 | RA | verify wrong ×4 still 401/400; 5th 429 |
| RA-OPS-174 | RA | 429 body error=rate_limited; locked_until |
| RA-OPS-175 | RA | verify po TTL 400 expired |
| RA-OPS-176 | RA | reuse verified 409 |
| RA-OPS-177 | RA | maker verify 409/403 (self) |
| RA-OPS-178 | RA | approve high-value bez verify → 400 step_up_required |
| RA-OPS-179 | RA | readonly 403 |

Uściślenie 173: pierwsze złe = 400 `invalid_pin` (nie 401 — jesteś authenticated). 429 dopiero po 5.

---

## Story E5-S2 — PinInput keyboard / paste

**Task:** `PW-OPS-T09` · P0

### Use case

`UC-OPS-23` — Modal „Confirm high-value refund” + timer „Expires in mm:ss”.

```ts
await pinInput.pressSequentially('482193');
```

Oraz paste całego PIN (otp autocomplete / clipboard). Backspace, strzałki.

### Acceptance criteria

- [ ] `length=6`, `type=number` albo text digits-only.
- [ ] `complete` submit auto **albo** przycisk Verify — **jedno** (rekomendacja: complete auto-verify + przycisk disabled aż 6).
- [ ] Accessible name na grupie.
- [ ] Paste: `locator.fill` / clipboard API lab już ma evidence clipboard — reuse.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-170 | E2E | pressSequentially correct → refund path continues |
| PW-OPS-E2E-171 | E2E | wrong pin → inline error; still 6 slots |
| PW-OPS-E2E-172 | E2E | paste 6 digits |
| PW-OPS-E2E-173 | E2E | Backspace clears last |
| PW-OPS-E2E-174 | E2E | arrow navigation między slotami |

---

## Story E5-S3 — Expiry clock + 429 UI

**Task:** `PW-OPS-T09` · P0

### Acceptance criteria

- [ ] `page.clock` install; fast-forward > TTL → message expired; Verify 400.
- [ ] Po 5 fail: UI „Too many attempts”; HTTP 429 widoczne w debug panel.
- [ ] Low-value refund: **brak** PinInput (istniejący dual-control).

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-175 | E2E | clock expire |
| PW-OPS-E2E-176 | E2E | 5 fail → 429 alert |
| PW-OPS-API-040 | PW REST | create+verify przez BFF cookie |

### Learning

Keyboard + rate limit + clock. Discriminated union `ChallengeState = open | expired | locked | verified`.
