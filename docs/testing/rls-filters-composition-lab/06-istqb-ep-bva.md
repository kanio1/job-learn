# 06 — Equivalence partitioning + BVA

## Daty listy (fromDate/toDate)

| ID | Partycja | Przykład | Oczekiwanie | Pokrycie |
|---|---|---|---|---|
| EP-RFC-010 | obie puste | — | wszystkie | existing-ra unfiltered |
| EP-RFC-011 | zakres zawiera createdAt | UTC today–today | match | existing-ra + POM (`toISOString().slice(0, 10)`) |
| EP-RFC-012 | zakres poza | 2099 | pusta lista 200 | existing-ra |
| BVA-RFC-010 | fromDate = toDate = dzień created | UTC today | match | existing-ra |
| EP-RFC-013 | from > to | — | 400 validation | istniejący `validateDateRange` (RA analog min>max) |

## Amount (minor units)

| ID | Partycja | Przykład | Oczekiwanie | Pokrycie |
|---|---|---|---|---|
| EP-RFC-020 | min≤amount≤max | 2000–4000 | tylko w zakresie | existing-ra (seed 1000*i) |
| EP-RFC-021 | min>max | 5000>1000 | 400 | existing-ra |
| BVA-RFC-020 | amount = min | 2000 | włącznie | existing-ra allMatch ≥ |
| BVA-RFC-021 | amount = max | 4000 | włącznie | existing-ra |
| EP-RFC-022 | UI min/max + status CREATED | 5000–10000 | URL query | existing-pw |

## Paginacja

| ID | Partycja | Oczekiwanie | Pokrycie |
|---|---|---|---|
| EP-RFC-030 | page 0 default | metadata | existing-ra first page |
| BVA-RFC-030 | widget page 2 | query page=1 | existing-pw |
| BVA-RFC-031 | Apply przy page=1 | page znika / 0 | existing-pw + POM |
| EP-RFC-031 | size 10, 25 elementów | 3 strony, last=5 | existing-ra |

## RLS visibility

| ID | Partycja | Oczekiwanie | Pokrycie |
|---|---|---|---|
| EP-RFC-040 | TENANT_ALPHA JWT | 1 wiersz a1 | existing-ra `alphaJwtListsOnlyAlphaRow` |
| EP-RFC-041 | id a2 jako Alpha | 404 `not_found` | existing-ra `alphaJwtCannotReadOtherTenantItem` |
| EP-RFC-042 | brak GUC (`rls_lab_app`) | count 0 | existing-ra JDBC |
| EP-RFC-043 | platform bypass role | `bypassRoleCount=2`, `restrictedWithoutTenantGuc=0` | existing-ra `platformCompareShowsLeakContrast` + POM E2E-004 |
| EP-RFC-044 | merchant compare, brak `platform:payments:read` | 403 (nie `rls_forbidden`) | existing-ra RA-014 |
| EP-RFC-045 | `platform:payments:read` + `TENANT_ALPHA` | 403 `rls_forbidden` | existing-ra RA-018 |
| EP-RFC-046 | support.agent + `PLATFORM_TENANT` | compare 200 | existing-ra RA-019 |
