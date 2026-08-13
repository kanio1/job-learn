---
name: epic-e5-bank-like
parent: browser-session-visual-network-lab
epic: E5
tasks: [MRL-T24, MRL-T25, MRL-T26, MRL-T27, MRL-T28]
last_updated: 2026-08-13
---

# Epic E5 — Bank-like enterprise (desktop)

**Cel produktowy:** lustra internet bankingu / panelu PayU (disputes, reports, dual control) bez bycia bankiem.  
**Cel dydaktyczny:** step-up, download, multipart, dwa `storageState`, consent.

**Połączenia:** payment evidence upload (REST-MULTIPART-01), CSV export, dual-role POM `auth-rbac.spec.ts`.  
Gating: wave 4; INFRA-KC-01 tylko jeśli BFF step-up nie wystarczy.

---

## Story E5-S1 — Step-up + dynamic linking  
**Task:** `MRL-T24` · P2 · FR-B01 FR-B06

### Jako / chcę / aby
Jako operator chcę dodatkowego potwierdzenia refundu powyżej progu, z kwotą i merchantId na ekranie (PSD2 dynamic linking **idea**, bez 3DS/PAN).

### Acceptance criteria
- [ ] Próg lab np. `amountMinor >= 10000`.
- [ ] Poniżej: refund jak dziś.
- [ ] Powyżej: modal `data-testid="step-up-challenge"` z **jawną kwotą + currency + merchant reference**.
- [ ] Potwierdzenie ustawia BFF `stepUpUntil` (INFRA-KC-01: **nie** realm w pierwszym cięciu).
- [ ] Copy: „lab SCA, not a licensed bank”.
- [ ] Timeout step-up → 403 `step_up_required`.

### Learning
- `KC:` ACR later; BFF first (C-07).
- `PW:` modal + waitForResponse 403 then 200.

---

## Story E5-S2 — Statements download  
**Task:** `MRL-T25` · P2 · FR-B02

### Jako / chcę / aby
Jako merchant PayU/bank chcę labowy wyciąg CSV/PDF (nie prawdziwy settlement).

### Acceptance criteria
- [ ] `GET /api/mirror-lab/statements?format=csv|pdf`
- [ ] `Content-Disposition: attachment; filename="statement-lab.{csv|pdf}"`
- [ ] UI button na `/admin/mirror-lab/statements`.
- [ ] PW POM: `waitForEvent('download')`, sprawdź suggested filename.
- [ ] Mocked: `route.fulfill` z CSV + disposition (Error Lab style).
- [ ] Zawartość syntetyczna (lab rows), nie ledger produkcyjny.

### Learning
- `PW:` download event.
- `HTTP:` attachment vs inline.

---

## Story E5-S3 — Disputes + multipart  
**Task:** `MRL-T26` · P2 · FR-B03

### Jako / chcę / aby
Jako merchant w PayU panelu chcę spór OPEN → upload evidence → CLOSED.

### Acceptance criteria
- [ ] Zasób lab `POST /api/mirror-lab/disputes` → 201 + Location.
- [ ] `POST .../evidence` multipart (filename, content-type, size limits) — alignment z REST-MULTIPART-01.
- [ ] 413 oversized, 415 bad type, 400 malformed boundary.
- [ ] UI DropZone + `setInputFiles`.
- [ ] Nie PAN w plikach; sample `tests-pom/data/files/sample-evidence.txt`.

### Learning
- `REST:` multipart, 201 Location.
- `PW:` filechooser / setInputFiles.
- Może zamknąć gate `REST-MULTIPART-01` jeśli `apps/api-tests` dostanie spece (E6).

---

## Story E5-S4 — Maker-checker  
**Task:** `MRL-T27` · P2 · FR-B04

### Jako / chcę / aby
Jako bank chcę, by refund powyżej limitu wymagał drugiej roli.

### Acceptance criteria
- [ ] Maker (`merchant.manager`) tworzy `PENDING_APPROVAL`.
- [ ] Checker (`platform.admin`) Approve; maker nie może self-approve.
- [ ] Jeden test PW: dwa contexty, dwa storageState (Playwright „multiple roles together”).
- [ ] 409 jeśli drugi maker; 403 jeśli checker z innego tenanta.

### Learning
- `PW:` dual storageState fixtures.
- `REST:` stan dual control.

---

## Story E5-S5 — AIS-lite consent  
**Task:** `MRL-T28` · P2 · FR-B05

### Jako / chcę / aby
Jako end-user desktop chcę udzielić i odwołać zgody na read-only listę transakcji lab TPP.

### Acceptance criteria
- [ ] Strona `/consent/mirror-lab` (public + login).
- [ ] Zakres: tylko odczyt listy lab, zero płatności PIS.
- [ ] Grant + revoke; po revoke TPP-lab 403.
- [ ] Nie Berlin Group produkcyjny, nie mobile app2app.
- [ ] Guest vs logged visual optional.

### Learning
- `KC:` consent screen idea; można **lab table** bez zmiany realm.
- `CK:` osobna zgoda ≠ session dashboard.

### Non-goals E5
- Prawdziwy Open Banking, KYC, chargeback network, payouts PayU.
