---
name: epic-e3-slideover-form
parent: playwright-merchant-360
epic: E3
tasks: [PW-M360-T10, PW-M360-T11]
last_updated: 2026-08-20
---

# Epic E3 — Merchant 360 Slideover + formularz

**Cel produktowy:** klik wiersza otwiera panel bez zmiany route; create zostaje na prawdziwych polach.  
**Cel dydaktyczny:** dialog/focus/Escape/ARIA snapshot; ISTQB DT na Zod.

Wzorzec UI: [EditUserDrawer.vue](../../../apps/frontend/app/components/user/EditUserDrawer.vue), [AuditEntryDrawer.vue](../../../apps/frontend/app/components/audit/AuditEntryDrawer.vue).

---

## Story E3-S1 — Slideover 360

**Task:** `PW-M360-T10` · P0

### Jako / chcę / aby

Jako support agent chcę otworzyć ACME z listy, zobaczyć status, risk, ostatnie płatności i audit, zamknąć Escape i wrócić fokus na wiersz.

### Business case

`BC-M360-20` — CRM 360 bez nowego bounded context: dane z `GET /merchants/{id}`, list płatności (size mały), audit filter `entityId` jeśli API na to pozwala; inaczej link „Open audit” + istniejący audit page.

### Use case

`UC-M360-20` — Click display name / Open `{ref}` → `role=dialog` → sekcje Information, Risk, Payments, History → Escape → dialog hidden.

### Acceptance criteria

- [ ] Route listy zostaje `/admin/merchants?...`.
- [ ] Głęboki link opcjonalny P2: `?merchantId=` otwiera panel (nice).
- [ ] Nested: Suspend w panelu → `ConfirmActionModal`; dismiss nie woła POST.
- [ ] Async: LoadingState w panelu; problem+json ErrorState.
- [ ] HTTP learning: ETag display gdy E4 jeszcze nie — można pokazać `version` z body jeśli mapowane; pełny ETag w E4.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-060 | E2E | Open unique merchant → dialog visible; GET detail 200 `waitForResponse` |
| PW-M360-E2E-061 | E2E | `dialog.toMatchAriaSnapshot` heading + close |
| PW-M360-E2E-062 | E2E | Escape → hidden; focus nie w pułapce |
| PW-M360-E2E-063 | E2E | Confirm suspend dismiss → brak POST `/suspend` |
| PW-M360-API-020 | PW REST | GET detail z panelu = ten sam `merchantId` |

---

## Story E3-S2 — Formularz (bez fikcyjnego NIP)

**Task:** `PW-M360-T11` · P0

### Jako / chcę / aby

Jako platform admin tworzę merchanta z reference, displayName, `tenantReference` (wymagane dla platform).

### Business case

`BC-M360-21` — Analiza chciała VAT/Country/Credit. **Nie w tym epicu** bez Flyway kolumn. Ćwiczenie DT/BVA: istniejące boundy Zod (`merchantReference` 3–64, `displayName` 2–120, regex).

### Use case

`UC-M360-21` — Create w modalu (lub stepper P2): Cancel, Create, duplicate 409, za krótki reference.

Jeśli później ADR doda `legalForm` / VAT — nowy epic, nowa migracja.

### Acceptance criteria

- [ ] `UForm` + Zod; accessible labels (nie sam placeholder).
- [ ] Platform: tenant field `data-testid` już jest.
- [ ] Decision table w `tests-pom/methods/` analog `MerchantReferencePartitions`.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-070 | E2E | unique create → 201 + wiersz (regresja merchants.spec) |
| PW-M360-E2E-071 | E2E | duplicate UI 409 problem |
| PW-M360-E2E-072 | E2E | BVA length 2 vs 3 vs 64 vs 65 |
| RA-M360-030 | RA | te same boundy na Spring (existing + gap jeśli jest) |
| PW-M360-E2E-073 | E2E | create form `toMatchAriaSnapshot` (rozszerzenie aria-snapshots.spec) |

Stepper (`FR-M360-STEP`) jest E7 — tu zostaje modal.
