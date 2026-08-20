---
name: epic-e8-saved-views
parent: playwright-ops-wave-2
epic: E8
tasks: [PW-OPS-T13, PW-OPS-T14]
last_updated: 2026-08-20
---

# Epic E8 — Saved Views & Column Profiles (#14)

**Cel produktowy:** operator zapisuje filtr+kolumny płatności i wraca do nich po loginie.  
**Cel dydaktyczny:** persistence localStorage → API; URL ↔ view ↔ controls; Page Object + component objects.

Nuxt UI: `UPopover`, `UCheckboxGroup`, `UDropdownMenu`, `UInput`.  
POM: `PaymentFiltersComponent`, `SavedViewsComponent`.

---

## Story E8-S1 — localStorage first

**Task:** `PW-OPS-T13` · P0 (część a)

### Jako / chcę / aby

Jako merchant manager zapisuję „Large EUR captured” i po reload widzę ten sam filtr **zanim** API views istnieje.

### Business case

`BC-OPS-14` — ERP saved views. Lekcja: storage ≠ security (JWT nadal zakazany w storage — [storage-safety.ts](../../../apps/frontend/tests-pom/utils/storage-safety.ts)).

### Use case

`UC-OPS-28` — ustaw Status=CAPTURED, Currency=EUR, Amount>10000, sort newest, kolumny; Save view name.

### Rules

- Key `pq.payment-views.{subject}` — subject z **sanitized auth store** (user id), nie token.
- Quota np. 20 views; nadmiar 400 w API, w localStorage overwrite oldest.
- Logout: localStorage per origin zostaje — **OK dla fazy a**. Faza b API jest źródłem po loginie (merge: server wins).

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-140 | E2E | save → reload → filters restored |
| PW-OPS-E2E-141 | E2E | storage-safety: brak access_token w localStorage |

---

## Story E8-S2 — API persistence + inny user

**Task:** `PW-OPS-T13` · P0 (część b)

### Use case

`UC-OPS-29`

```text
POST /api/users/me/payment-views
GET  /api/users/me/payment-views
PUT  /api/users/me/payment-views/{id}
DELETE ...
POST .../{id}/default
```

### SQL

[01-infra B.5](../01-infra-postgres-keycloak-stack.md) V35. Owner = JWT sub. IAM module.

### Acceptance criteria

- [ ] Logout/login same user → view present (API).
- [ ] `platform.operator` nie widzi views admina.
- [ ] Default unique per resource (partial unique index).
- [ ] Zod filters whitelist = istniejące query params payment list (status, currency, amountMin, sort, page size nie w view).

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-OPS-140 | RA | POST view 201; GET list 1 |
| RA-OPS-141 | RA | drugi subject GET empty |
| RA-OPS-142 | RA | second default flips first is_default false |
| RA-OPS-143 | RA | unknown filter key 400 |
| PW-OPS-E2E-142 | E2E | save API → logout login restore |
| PW-OPS-E2E-143 | E2E | other user view absent |
| PW-OPS-API-050 | PW REST | CRUD przez BFF cookie |

---

## Story E8-S3 — URL ↔ view ↔ controls

**Task:** `PW-OPS-T14` · P0

### Use case

`UC-OPS-30` — Apply view ustawia query string; Back przywraca; zmiana checkbox kolumn nie psuje filtra.

### Acceptance criteria

- [ ] Otwórz view → URL zawiera status/currency itd. (jak istniejąca sync filtrów).
- [ ] Column profile: `UCheckboxGroup`; ukrycie kolumny UI-only (nie security — RBAC kolumn to M360 E4).
- [ ] Clear filters ≠ delete view.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-144 | E2E | apply view → URL query match |
| PW-OPS-E2E-145 | E2E | Back restores view |
| PW-OPS-E2E-146 | E2E | set default star |
| PW-OPS-E2E-147 | E2E | uncheck column Created by → header absent; data still in API |

### Learning

Component objects `apply / clear / saveAs / open / setDefault`. TypeScript view DTO shared Vue+Nitro w `shared/types` jeśli BFF i UI tego potrzebują.
