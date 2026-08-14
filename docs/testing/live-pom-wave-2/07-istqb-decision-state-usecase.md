# 07 — Decision tables, state, use case, pairwise, error guessing

Implementacja = konkretny `test('…')` z **03**. `designed` tylko gdy spec nie istnieje.

---

## DT-W2-01 — Kto widzi `/admin`

| Sesja | Path | Wynik | TC |
|---|---|---|---|
| brak | `/admin/merchants` | `/login?redirectTo=` | E2E-001 |
| brak | `/admin/session-lab` | login | E2E-002 |
| brak | `/admin/users` itd. | login | E2E-003 designed |
| admin | `/admin/merchants` | lista | E2E-020 |
| manager | `/admin/users` | forbidden | E2E-100 |
| manager | `/admin/support` + Beta UUID | problem, 0 rows | E2E-070 |
| admin | `/admin/support` | wyniki | E2E-071 designed |

---

## DT-W2-02 — Notes / risk capability

| UI control | JWT fine-grained | HTTP | UI | TC |
|---|---|---|---|---|
| notes form admin | tak | 201 | note + `listNotes` | E2E-040 |
| notes form admin | nie | 403 | alert / error-state | E2E-040 |
| risk toggle | tak | PATCH 200 | badge listy | E2E-050 |
| risk toggle | nie | 403 | unflagged | E2E-050 |
| notes manager | — | brak POST | designed | E2E-041 |

---

## DT-W2-03 — Idempotency create order

| Ten sam key | Ten sam body | HTTP | TC |
|---|---|---|---|
| pierwszy | — | 201 + nowe id | E2E-090/091 |
| tak | tak | 200 to samo id | E2E-091 |
| tak | nie | 409 `idempotency_conflict` | E2E-091 |

---

## ST-W2-01 — Merchant

`DRAFT --activate→ ACTIVE --suspend→ SUSPENDED`.  
TC: E2E-021 `activates a DRAFT merchant then suspends it`.

---

## ST-W2-02 — Payment order (dashboard)

```text
CREATED --authorize(If-Match fresh)→ AUTHORIZED --capture(If-Match)→ CAPTURED
CREATED --authorize(stale)→ 412, zostaje CREATED
CREATED --cancel + ConfirmModal submit→ CANCELLED
CREATED --ConfirmModal dismiss→ CREATED (brak POST /cancel)
```

TC: E2E-092, 093, 094, 096.

---

## ST-W2-03 — Checkout fulfillment (live POM)

```text
booking ONLINE → AWAITING_PAYMENT
  --Approve+return→ CONFIRMED
  --lie return→ AWAITING_PAYMENT|UNKNOWN
  --Decline+return→ CANCELLED
booking CASH → CONFIRMED (bez hosted)
booking EXPIRED_LINK → hosted `psp-link-expired`
```

TC: E2E-060…065. PSP session `CANCELED` ≠ fulfillment `CANCELLED`.

---

## Use cases (konkretne, as-built)

Format: aktor · precondition · kroki · oracle · spec.

### UC-W2-01 — Gość próbuje registry — P0

- **Aktor:** anonim (`chromium-guest`, puste cookies).
- **Kroki:** otwiera `/admin/merchants`.
- **Oracle:** `/login?redirectTo=`; ekran logowania, nie pusta tabela.
- **TC:** E2E-001. Wariant session-lab: E2E-002.

### UC-W2-02 — Operator wylogowuje shared browser — P0

- **Aktor:** platform admin.
- **Kroki:** registry loaded → Sign out → wpisuje `/admin/merchants`.
- **Oracle:** oba razy `/login`.
- **TC:** E2E-010.

### UC-W2-03 — Rejestracja merchantu — P0

- **Aktor:** platform admin + `BffClient`.
- **Happy:** unique POST 201 → detail pokazuje reference (E2E-020).
- **Persist:** GET 200 po create (E2E-022).
- **Walidacja:** pusty form, 0 POST (E2E-023).
- **Unikalność:** drugi POST 409 (E2E-026).
- **Lifecycle:** Draft → Active → Suspended (E2E-021).
- **Blocked:** UI POST z tenantem (E2E-024).

### UC-W2-04 — Notatka na żywej płatności — P0

- **Aktorzy:** merchant manager (tworzy order Alpha), platform admin (UI notes).
- **Precondition:** admin JWT **nie** ma `merchant:payments:create` — drugi storageState.
- **Kroki:** manager POST order 201 → admin detail → `addNote` → POST `/notes`.
- **Oracle:** 201 + GET list zawiera body **albo** 403 + alert (GAP-W2-02).
- **TC:** E2E-040.

### UC-W2-05 — Booking gotówka vs karta vs odmowa vs kłamstwo — P0

- **CASH:** select mode → CONFIRMED, brak nowej karty (E2E-062).
- **ONLINE approve:** nowa karta → hint zawiera success → fulfillment CONFIRMED (E2E-060).
- **Lie:** return `status=success` bez Approve → nie CONFIRMED (E2E-061).
- **Decline:** fulfillment CANCELLED; hint zawiera failure (E2E-063, GAP-W2-03).
- **Expired:** scenario EXPIRED_LINK → `psp-link-expired` (E2E-065).

### UC-W2-06 — Support IDOR — P0

- **Aktor:** merchant manager (tenant Alpha).
- **Kroki:** nav Support ukryty → deep-link `/admin/support` → search `merchantBetaId`.
- **Oracle:** problem+json; tabela wyników count 0 (brak wycieku Beta).
- **TC:** E2E-070.

### UC-W2-07 — Problem+json z Error Lab — P0

- **Aktor:** admin na `/error-lab`.
- **Kroki:** trigger 400 / 401 / 412; 429 widoczny ale nie wołany.
- **Oracle:** żywy status; karta problemu; brak Authorization w body.
- **TC:** E2E-080…082.

### UC-W2-08 — Utworzenie płatności z idempotencją — P0

- **Aktor:** merchant manager, merchant Alpha.
- **Kroki:** formularz amount/currency/reference + Idempotency-Key → submit.
- **Oracle:** request niesie ten sam klucz; landuje na detail UUID.
- **Replay:** ten sam key+body → 200 to samo id; inny body → 409 `idempotency_conflict`.
- **TC:** E2E-090, 091.

### UC-W2-09 — Autoryzacja i capture z ETag — P0

- **Aktor:** manager na detail CREATED.
- **Kroki:** odczyt ETag z GET → Authorize (If-Match) → Capture z If-Match z drawera.
- **Negatyw:** If-Match `"stale-etag"` → 412, GET nadal CREATED.
- **TC:** E2E-092, 093.

### UC-W2-10 — Anulowanie z potwierdzeniem — P0

- **Submit:** ConfirmModal → status Cancelled (E2E-094).
- **Dismiss:** `confirm-action-dismiss` → brak POST `/cancel`, GET CREATED (E2E-096).

### UC-W2-11 — Izolacja tenant/merchant w UI — P0

- **Aktorzy:** admin i manager, dwa contexty.
- **Kroki:** oba lista Alpha → manager Users/Audit ukryte → manager Beta → forbidden → manager `/admin/users` forbidden.
- **TC:** E2E-100.

### UC-W2-12 — Risk flag na nowym merchancie — P1

- **Kroki:** unique merchant → activate → toggle risk → filtr listy.
- **Oracle:** 200 + badge **albo** 403 (GAP-W2-02). Nie seed Alpha.
- **TC:** E2E-050.

### UC-W2-13 — Command palette do Error Lab — P1

- **Kroki:** Ctrl+K → ARIA snapshot dialogu → opcja Error Lab.
- **TC:** E2E-030.

### UC-W2-14 — Audit export i evidence bez wycieku tokenu — P1

- **Audit:** GET `/api/audit/export.json` + plik JSON bez `eyJ` (E2E-111).
- **CSV płatności:** GET export + download `.csv` bez Bearer (E2E-097).
- **Evidence:** upload `sample-evidence.txt` → `evidence-file-name`.

### UC-W2-15 — Tenant settings optimistic concurrency — P1

- **Kroki:** GET settings ETag → PATCH z `If-Match`.
- **Cleanup:** `afterEach` przywraca snapshot.
- **TC:** E2E-112.

### UC-W2-16 — CSRF fail path (Session Lab) — P0

- **Kroki:** `session-lab-csrf-fail` bez headera.
- **Oracle:** 403 `csrf_failed`.
- **TC:** E2E-121. Happy CSRF = designed (MRL).

---

## PWISE-W2-01 — Checkout (zredukowane, as-built)

| Para | TC |
|---|---|
| CASH × — | E2E-062 |
| ONLINE × approve | E2E-060 |
| ONLINE × decline | E2E-063 |
| ONLINE × lie | E2E-061 |
| ONLINE × EXPIRED_LINK | E2E-065 |

Nie kartezjan currency × amount × mode.

## PWISE-W2-02 — Filtry (RFC)

CREATED × PLN vs EUR; date+status+reference; amount min/max; stale `page=1` → Apply.  
TC: E2E-095 / RFC E2E-020–023.

---

## Error guessing

| ID | Hipoteza | Mitigacja | Pokrycie |
|---|---|---|---|
| EG-W2-01 | Overlay Vite przejmuje click | `addLocatorHandler` + zwykły `.click()` | fixtures |
| EG-W2-02 | `localhost` → `::1` ECONNREFUSED | `BffClient` `127.0.0.1`; browser `localhost` | API |
| EG-W2-03 | Sealed cookie zawiera `eyJ` | skan tylko `origins` | E2E-011 |
| EG-W2-04 | `payments-internal-notes.spec.ts` wpadnie w manager project | plik `internal-notes.spec.ts` | E2E-040 |
| EG-W2-05 | Double `status` na continueUrl | `toContainText('failure')` nie exact | E2E-063 |
| EG-W2-06 | `page.selectOption` na USelect | `getByRole('option')` | E2E-096 |
| EG-W2-07 | Confirm „Cancel” = zły przycisk | `confirm-action-dismiss` | E2E-096 |
| EG-W2-08 | Globalny `status-badge` łapie dashboard | badge per `clientOrderReference` | E2E-096 |
