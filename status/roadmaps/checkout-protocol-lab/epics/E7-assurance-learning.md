---
name: epic-e7-assurance-learning
parent: checkout-protocol-lab
epic: E7
tasks: [CPL-T17, CPL-T18, CPL-T19, CPL-T30, CPL-T24]
last_updated: 2026-08-09
---

# Epic E7 — Assurance & Learning Evidence

**Cel produktowy:** domknięcie jakości + gate Wave 2B `REST-REDIRECT-01`.  
**Cel dydaktyczny:** czarna skrzynka RA + przeglądarkowe hop’y PW; dokumentacja hopów.

**Połączenia:** cały MVP; `status/index.md` Active work Wave 2B; `apps/api-tests`; Playwright chromium.

---

## Story E7-S1 — api-tests / REST Assured suite  
**Task:** `CPL-T17` · P0

### Jako / chcę / aby
Jako SDET chcę czarną skrzynkę protokołu.

### Minimalny zestaw testów

| Case | Assert |
|---|---|
| OAuth happy | 200 + token |
| OAuth bad secret | 401 |
| Create follow=false | **302** + Location |
| Create no auth | 401 |
| Notify bad signature | **400**, brak CONFIRMED |
| Notify happy | **202**, event RECEIVED |
| Duplicate event | **200** duplicate, fulfillment ×1 |
| GET session | 200 status fields |

### Acceptance criteria
- [ ] Testy w `apps/api-tests` **lub** backend IT `checkoutlab` — jedna kanoniczna suite.
- [ ] Nie używają Keycloak JWT do notify.
- [ ] Oracle: headers + DB / retrieve.
- [ ] Wchodzą w filtered verify **albo** osobny Failsafe module — udokumentuj w tasku przy implementacji.

### Learning
- `HTTP:` `redirects().follow(false)` — krytyczna umiejętność.
- `REST:` macierz 400/202/302/401.
- `SQL:` asercje po INSERT inbox / fulfillment.
- `KC:` suite pokazuje, że IPN nie potrzebuje realm usera.

### Połączone z
- skill `rest-api-test-design`
- istniejące RA patterns w `apps/backend/.../rest` i `apps/api-tests`

---

## Story E7-S2 — Playwright journey pack  
**Task:** `CPL-T18` · P0

### Jako / chcę / aby
Jako SDET FE chcę 3–5 stabilnych E2E.

### Minimalny pack

1. **Multi-tab pay** — create → hosted Approve → fulfillment CONFIRMED.  
2. **Lie return** — hint success, brak event → nie CONFIRMED.  
3. **pay_no_return** — Approve + close tab → CONFIRMED bez return visit.

Opcjonalnie: cancel; expired (P1).

### Acceptance criteria
- [ ] Locators: role / `data-testid`; zero `waitForTimeout`.
- [ ] Oracle: API fulfillment lub Inspector, nie tylko zielony tekst.
- [ ] Flaga lab włączona w env webServer / project.
- [ ] Nie psują istniejących 82 chromium (F-D2 może zostać lub zostać zmigrowany — decyzja w CPL-T14).

### Learning
- `PW:` browser hops + API oracle.
- `HTTP:` wizualizacja 302 w DevTools / trace.
- `KC:` hosted bez logowania; Booking (jeśli w packu) z storage state.

### Połączone z
- `psp-redirect-simulator.spec.ts`
- skill `playwright-sdet-review`
- `playwright.config.ts` / live config tylko jeśli potrzeba real KC dla chronionych stron

---

## Story E7-S4 — Wave 2B gate closure  
**Task:** `CPL-T19` · P0

### Jako / chcę / aby
Jako PM chcę zamknąć `REST-REDIRECT-01` przez CPL jako approved test-only redirect server.

### Acceptance criteria
- [ ] Wpis w `status/index.md` / evidence: gate fulfilled by CPL (SUPERSEDED / DONE_VERIFIED).
- [ ] Jasne: **brak** inventowania prod 3xx poza labem.
- [ ] Link do tego katalogu roadmapy.

### Learning
- Proces: training feature zamyka assurance gate bez scope creep.

### Połączone z
- `status/index.md` → REST-ADVANCED / REST-REDIRECT-01
- E1-S2 dowód 302

---

## Story E7-S3 — Learning evidence note  
**Task:** `CPL-T30` · P1

### Jako / chcę / aby
Jako instructional designer chcę mapę hopów w `docs/`.

### Acceptance criteria
- [ ] Doc: tabela signature / dedup / retry / ACK.
- [ ] Provider matrix (PayU-like vs lab headers).
- [ ] Non-goals + „continueUrl is a lie”.
- [ ] Link do `status/roadmaps/checkout-protocol-lab/`.

### Learning
- Domknięcie pętli: kod → test → lekcja pisana.

### Połączone z
- `learning-map.md` (ten katalog)
- skill `implementation-learning-loop`

---

## Task CPL-T24 — Reset checkout_* (INFRA-PG-03) · P1

### Acceptance criteria
- [ ] Gdy lab enabled, test reset czyści tabele CPL.
- [ ] Nie rusza deterministic seed merchant/payment poza istniejącym kontraktem.
- [ ] Udokumentowane w `test-data-isolation` notes / FE support jeśli dotyczy.

### Learning
- `SQL:` izolacja testów przy nowym module.
- `KC:` bez wpływu na realm users.
