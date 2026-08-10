---
name: epic-e4-hosted-checkout-return
parent: checkout-protocol-lab
epic: E4
tasks: [CPL-T14, CPL-T15, CPL-T16, CPL-T22]
last_updated: 2026-08-09
---

# Epic E4 — Hosted Checkout & Return (F-D2 upgrade)

**Cel produktowy:** prawdziwy hop przeglądarki + dowód „redirect/return is a lie”.  
**Cel dydaktyczny:** multi-tab PSP; untrusted continueUrl; notify bez powrotu.

**Połączenia:**

```text
dziś:  /psp-redirect-simulator  (F-D2) + error-lab trigger
docelowo: /psp/checkout/{sessionId}  LUB  upgrade istniejącej strony z query ?sessionId=
         + /checkout-lab/return (continueUrl target)
```

Istniejące: `apps/frontend/app/pages/psp-redirect-simulator.vue`,  
`apps/frontend/tests/e2e/psp-redirect-simulator.spec.ts`,  
middleware wyłącza auth dla `/psp-redirect-simulator`.

---

## Story E4-S1 — Bind F-D2 to session  
**Task:** `CPL-T14` · P0 · FR-03

### Jako / chcę / aby
Jako płatnik chcę hosted page z kwotą i Approve/Decline/Pending powiązaną z session.

### Acceptance criteria
- [ ] Wejście z `Location` / `redirectUri` z E1-S2.
- [ ] Pokazuje amountMinor + currency z session (API lab lub BFF).
- [ ] Approve → stub emituje `checkout.session.completed` (E2-S1).
- [ ] Decline → `canceled` event.
- [ ] Bez pól PAN/CVV (SAQ-A mindset).
- [ ] Middleware: path publiczny (jak dziś F-D2) — **bez logowania Keycloak**.
- [ ] `data-testid` stabilne (upgrade istniejących).

### Szkic FE

```ts
// route: /psp/checkout/[sessionId]  lub query na istniejącej stronie
const session = await $fetch(`/api/checkout-lab/sessions/${sessionId}`)
// Approve:
await $fetch(`/api/checkout-lab/sessions/${sessionId}/simulate`, {
  method: 'POST',
  body: { outcome: 'COMPLETED' } // lab-only, flag-gated
})
```

(Exact API simulate — decyzja implementacyjna; może być wewnętrzne wywołanie stubu bez publicznego simulate, jeśli Preferujesz tylko UI→stub bean.)

### Learning
- `PW:` multi-tab / popup już ćwiczone w F-D2 — teraz z **danymi**.
- `KC:` hosted page publiczna = realistyczne.
- `HTTP:` browser GET na hosted URL po 302.

### Połączone z
- E1-S2 Location
- E2-S1 notifier
- istniejący `psp-redirect-simulator.spec.ts`

---

## Story E4-S2 — Untrusted continueUrl page  
**Task:** `CPL-T15` · P0 · FR-04

### Jako / chcę / aby
Jako learner chcę thank-you z hintem, który **nie** jest oracle.

### Acceptance criteria
- [ ] Strona return czyta query `status=success` (hint) **oraz** poll/GET fulfillment.
- [ ] UI: „oczekiwanie na potwierdzenie” dopóki fulfillment ≠ `CONFIRMED`.
- [ ] PW: hint=success + **brak** event → **nie** pokazuje finalnego CONFIRMED / nie traktuje jako paid.
- [ ] Copy edukacyjny: „Return URL is not proof of payment”.

### Learning
- `HTTP:` GET return URL = UX only.
- `REST:` oracle = fulfillment/session API.
- `PW:` asercja na stan biznesowy, nie na zielony banner z query.

### Połączone z
- E3-S3
- E7-S2 scenario lie return

---

## Story E4-S4 — Close-tab before return  
**Task:** `CPL-T16` · P0

### Jako / chcę / aby
Jako QA chcę dowód „notify bez return”.

### Acceptance criteria
- [ ] PW: Approve → zamknij tab hosted → **bez** wizyty continueUrl.
- [ ] Fulfillment i tak → `CONFIRMED` (poll API / Inspector).
- [ ] Stabilne, bez `waitForTimeout` — wait na API/locator fulfillment.

### Learning
- `HTTP:` notify path niezależny od browser return.
- `PW:` multi-context / page.close wzorzec.
- Big-tech: webhook = truth.

### Połączone z
- E2 + E3
- istniejący multi-tab test F-D2

---

## Story E4-S3 — Payment link expiry  
**Task:** `CPL-T22` · P1 · FR-10

### Jako / chcę / aby
Jako merchant chcę `validityUntil` na linku.

### Acceptance criteria
- [ ] Po expiry: UI expired; Approve zablokowane.
- [ ] Session → `EXPIRED` / fulfillment `EXPIRED` wg reguł.
- [ ] Test z `Clock` / stub time (nie sleep 15 min).

### Learning
- `HTTP:` late pay attempt → biznesowy reject.
- `SQL:` `validity_until TIMESTAMPTZ`.
- `PW:` clock fixtures / backend time control.

### Połączone z
- E1 create `validitySeconds`
- E6 scenario `expired_link`
