---
name: epic-e5-import-kanban
parent: playwright-merchant-360
epic: E5
tasks: [PW-M360-T14, PW-M360-T15]
last_updated: 2026-08-20
---

# Epic E5 — Import CSV + Payment Kanban

**Cel produktowy:** onboarding wsadowy merchantów; pipeline płatności jak CRM Kanban.  
**Cel dydaktyczny:** `setInputFiles` na żywym multipart; `dragTo` + keyboard Move; rollback na prawdziwym 4xx.

Gate: E1 (list) + E4 (ETag) dla kanban If-Match.

---

## Story E5-S1 — Import preview

**Task:** `PW-M360-T14` · P0

### Jako / chcę / aby

Jako platform admin wgrywam `merchants.csv` i widzę valid / warning / rejected zanim cokolwiek wstanie w DB.

### Business case

`BC-M360-40` — ERP import: walidacja nagłówka, UTF-8, puste, duplikat `normalized_reference`, zły tenant.

### Use case

`UC-M360-40` — Upload → preview (brak INSERT) → liczby; Download rejected.

### Acceptance criteria

- [ ] `POST /api/merchants/import/preview` multipart; authority create.
- [ ] CSV kolumny: `merchantReference,displayName,tenantReference` (platform). Tenant admin: bez tenant column, wiersze w JWT tenant.
- [ ] Duplikat istniejącego UK → rejected, nie warning.
- [ ] Preview **transakcja read-only** / bez commit.
- [ ] UI: `UFileUpload` jeśli 4.7.1; inaczej input file jak evidence. Label „Upload CSV”.
- [ ] Fixture w `tests-pom/fixtures/import/` (git): `valid.csv`, `duplicate.csv`, `invalid-header.csv`, `empty.csv`, `utf8.csv`, `malformed.csv`. **Nie** 10k w live POM (P2 RA tylko).

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-080 | E2E | `setInputFiles(valid.csv)` → preview counts; GET list jeszcze bez nowych |
| PW-M360-E2E-081 | E2E | invalid-header → rejected/error, 0 commit |
| PW-M360-E2E-082 | E2E | empty.csv → empty/validation |
| PW-M360-E2E-083 | E2E | duplicate.csv vs istniejący unique ref |
| RA-M360-060 | RA | preview 200 body counts; DB count unchanged |
| RA-M360-061 | RA | readonly 403 |
| RA-M360-062 | RA | malformed 400 problem |

---

## Story E5-S2 — Import commit

**Task:** `PW-M360-T14` · P0

### Use case

`UC-M360-41` — Preview OK → Commit → merchanci DRAFT w list + audit events.

### Acceptance criteria

- [ ] `POST .../import/commit` z tokenem preview albo ponowny plik + checksum (wybrać jedno w implementacji; rekomendacja: signed previewId TTL).
- [ ] Jedna transakcja INSERT; partial fail → rollback całości **albo** per-row report (zdecydować: **all-or-nothing** prościej + testowalnie).
- [ ] Idempotentny drugi commit tego samego preview → 409.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-084 | E2E | commit → wiersze w UTable; GET list `q=` |
| PW-M360-E2E-085 | E2E | drugi commit 409 |
| RA-M360-063 | RA | po commit SELECT count; Flyway UK |
| PW-M360-API-030 | PW REST | multipart przez BFF cookie |

---

## Story E5-S3 — Kanban płatności

**Task:** `PW-M360-T15` · P0

### Jako / chcę / aby

Jako merchant manager przeciągam order CREATED → kolumna Authorized, a system woła **istniejący** `POST .../authorize` z If-Match.

### Business case

`BC-M360-41` — To nie jest Deal CRM. Kolumny = statusy `payment_orders`. Nielegalny drop (CAPTURED → CREATED) = ten sam problem co `payments-illegal-transitions`.

### Use case

`UC-M360-42` — Toggle Table/Board na `/admin/merchants/{id}/payments`. Drag card → waitForResponse POST authorize 200 → karta w target → reload → nadal tam. 412 → karta wraca + toast.

### Acceptance criteria

- [ ] Kolumny: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED (puste dozwolone).
- [ ] `dragTo` **oraz** `UDropdownMenu` „Move to AUTHORIZED” (a11y; drag flakowy).
- [ ] Optimistic UI dozwolony tylko z rollback na !2xx.
- [ ] `data-testid="payment-card-{id}"` / `stage-{status}` jeśli role niewystarczające.
- [ ] Worker merchant; unikalny order.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-090 | E2E | menu Move CREATED→authorize; POST 200; GET detail AUTHORIZED |
| PW-M360-E2E-091 | E2E | `dragTo` happy path (może `@flaky` tag jeśli trzeba; menu jest P0) |
| PW-M360-E2E-092 | E2E | reload board — karta w AUTHORIZED |
| PW-M360-E2E-093 | E2E | drugi context capture w międzyczasie → 412 + rollback |
| PW-M360-E2E-094 | E2E | drop nielegalny → 4xx + karta źródłowa |
| PW-M360-API-031 | PW REST | ten sam authorize przez BffClient |
