---
name: epic-e7-calendar-timeline-editor
parent: playwright-merchant-360
epic: E7
tasks: [PW-M360-T19, PW-M360-T20]
last_updated: 2026-08-20
---

# Epic E7 — Calendar, Timeline, Stepper, Editor, editable grid

**Cel produktowy:** operacyjne daty, historia 360, wizard create-payment, inline edit nazwy.  
**Cel dydaktyczny:** `page.clock`; contenteditable tylko jeśli 4.7.1 ma Editor.

Gate komponentów: sprawdzić eksport `@nuxt/ui@4.7.1` (T19 start). Fallback w [02-research](../02-versioned-research.md).

---

## Story E7-S1 — Calendar `expiresAt`

**Task:** `PW-M360-T19` · P2

### Business case

`BC-M360-60` — CRM kalendarz = terminy autoryzacji (7 dni seed), nie spotkania sales.

### Use case

`UC-M360-60` — Widok miesiąca; dzień z `expiresAt`; click → lista orderów. `page.clock` ustawia „dziś”.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-130 | E2E | `clock.install` + `getByRole('button', { name: /20 / })` jeśli UCalendar; inaczej date input |
| PW-M360-E2E-131 | E2E | disabled past P2 |
| RA-M360-080 | RA | list `fromDate`/`toDate` UTC bounds (regresja RLS wave) |

---

## Story E7-S2 — Timeline w 360

**Task:** `PW-M360-T19` · P1

### Business case

`BC-M360-61` — Akcja UI (activate) → audit event → timeline merchanta.

### Use case

`UC-M360-61` — W slideover: chronologia `createdAt` asc/desc ustalona; support widzi audit; manager **nie** otwiera 360 registry.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-132 | E2E | activate → timeline zawiera ACTIVE (albo audit page filter) |
| PW-M360-E2E-133 | E2E | payment history tab vs timeline — te same przejścia lifecycle |
| RA-M360-081 | RA | audit list po activate (istniejący audit API) |

---

## Story E7-S3 — Stepper create payment

**Task:** `PW-M360-T19` · P2

### Business case

`BC-M360-62` — Maszyna: DRAFT form → AMOUNT_SET → CURRENCY_SET → REFERENCE_SET → SUBMITTED. Back/Next/validation. Double submit: Idempotency-Key (już jest).

### Use case

`UC-M360-62` — Wizard na `/payments/new`; review pokazuje payload; Create → 201; browser Back nie gubi draft (sessionStorage OK, nie localStorage token).

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-134 | E2E | Next bez amount → field error |
| PW-M360-E2E-135 | E2E | pełny wizard → POST create 201 `waitForResponse` |
| PW-M360-E2E-136 | E2E | double click Create: jeden 201 / replay 200 (istniejąca semantyka) |

---

## Story E7-S4 — Editor notes (conditional)

**Task:** `PW-M360-T19` · P2

Jeśli brak `UEditor` w 4.7.1: **SKIP** story, zostaje `InternalNotes` textarea. Nie dopinać TipTap.

### Test IDs (tylko gdy Editor w pinie)

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-140 | E2E | type w contenteditable; POST notes 201 |
| PW-M360-E2E-141 | E2E | XSS string zapisany escaped w GET |

---

## Story E7-S5 — Editable grid displayName

**Task:** `PW-M360-T20` · P1 · wymaga E4 ETag

### Jako / chcę / aby

Jako tenant admin edytuję nazwę w wierszu: Edit → spin/textbox → Save → PATCH + If-Match → 200.

### Business case

`BC-M360-63` — Inline ERP. Nowy `PATCH /api/merchants/{id}` displayName only (nie status). If-Match 412.

### Use case

`UC-M360-63` — Row filter ref → Edit name → fill → Save; `waitForResponse` PATCH 200; GET name.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-150 | E2E | inline save 200 + tekst wiersza |
| PW-M360-E2E-151 | E2E | stale If-Match 412 + stara nazwa |
| RA-M360-090 | RA | PATCH displayName validation 2–120 |
| RA-M360-091 | RA | PATCH bez If-Match 428 |
| PW-M360-API-060 | PW REST | PATCH BFF |

Nie PATCH-ować `normalized_reference` (immutable).
