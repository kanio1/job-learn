---
name: epic-e4-bulk-partial-failure
parent: playwright-ops-wave-2
epic: E4
tasks: [PW-OPS-T08]
last_updated: 2026-08-20
---

# Epic E4 — Bulk Actions with Partial Failure (#15)

**Cel produktowy:** zaznacz wiele spraw → Assign owner; część się uda, część nie; Retry failed.  
**Cel dydaktyczny:** per-item UI state, kontrakt Retry = tylko failed IDs.

**Gate:** E3 cases istnieją.

Nuxt UI: `UCheckbox`, `UProgress`, `UModal`, `UAlert`, `UToast`.

Analiza wspominała `POST /api/payment-orders/bulk-assign`. **Ten lab** robi bulk na **support cases** (właściciel kolejki). Nie mylić z M360 bulk activate merchants.

---

## Story E4-S1 — Partial success API + modal

**Task:** `PW-OPS-T08` · P0

### Jako / chcę / aby

Jako lead support przypisuję 20 spraw do Anny i widzę 17/3 z powodami, nie jeden toast „error”.

### Business case

`BC-OPS-15` — ERP bulk: lock, 403, concurrent modify jako **wiersze**, nie all-or-nothing (all-or-nothing jest w M360 import commit).

### Use case

`UC-OPS-20` — POST

```http
POST /api/support/cases/bulk-assign
{ "caseIds": [ "...", "..." ], "assigneeSubject": "support.agent" }
```

```json
{
  "succeeded": 17,
  "failed": [
    { "caseId": "...", "caseReference": "INC-19", "error": "already_locked" },
    { "caseId": "...", "caseReference": "INC-21", "error": "forbidden" },
    { "caseId": "...", "caseReference": "INC-31", "error": "precondition_failed" }
  ]
}
```

HTTP **200** przy mixed (nie 207, żeby Zod/BFF było proste). HTTP **400** gdy pusta lista albo >100 ids. HTTP **403** gdy caller nie ma operate.

### Domain rules

- Per-item: własna If-Match **nie** wymagana na bulk (użyj aktualnego version w TX albo skip If-Match i polegaj na row lock). **Decyzja:** `SELECT ... FOR UPDATE` per id; jeśli status RESOLVED → fail `already_resolved`; jeśli concurrent update → fail `precondition_failed`.
- Idempotent: ponowne assign tego samego assignee na już assigned → **success** (no-op).
- Tenant: id poza tenantem → failed `not_found` (nie wycieka 403 vs 404 mieszany w jednym body — jeden kod `not_found`).
- Transakcja: **nie** rollback całości. Każdy item commit (autonomous) **albo** jedna TX z savepoints. Rekomendacja lab: jedna TX + savepoint per id (PG) — łatwiejszy test „partial visible after 200”.

### Acceptance criteria

- [ ] UI progress podczas requestu (`UProgress`).
- [ ] Modal: success count, failure rows (`getByRole('row')` count = failed).
- [ ] POWÓD czytelny (nie surowy stack).
- [ ] Checkbox selection zachowana dla failed (Retry).

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-OPS-150 | RA | 4 ids: 1 RESOLVED, 1 obcy tenant, 2 OK → succeeded=2 failed=2 |
| RA-OPS-151 | RA | pusta lista 400 |
| RA-OPS-152 | RA | >100 ids 400 |
| RA-OPS-153 | RA | readonly 403 |
| RA-OPS-154 | RA | no-op same assignee counts succeeded |
| PW-OPS-E2E-150 | E2E | `successCount` text 2; `failureRows` count 2 |
| PW-OPS-E2E-151 | E2E | progress visible then modal |

---

## Story E4-S2 — Retry failed

**Task:** `PW-OPS-T08` · P0

### Use case

`UC-OPS-21` — [Retry failed] → drugi POST **tylko** failed IDs.

### Acceptance criteria

- [ ] `page.waitForRequest` / `waitForBff` body `caseIds.length === 3` (przykład).
- [ ] Claim w spec, nie w page class.
- [ ] Po udanym retry wiersze znikają z failure list.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-API-031 | PW REST | retry payload only failed ids |
| PW-OPS-E2E-152 | E2E | Retry → waitForRequest body ids |
| PW-OPS-E2E-153 | E2E | po retry modal succeeded+=n |

### Learning

UI ↔ REST contract. TypeScript: `BulkAssignResult = { succeeded: number; failed: ReadonlyArray<BulkFailure> }`.
