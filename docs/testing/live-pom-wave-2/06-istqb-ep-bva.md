# 06 — Equivalence partitioning + BVA

Każda partycja mapuje na `PW-W2-E2E-*` / `PW-W2-API-*` / `PW-W2-SEC-*`.

## Sesja

| ID | Partycja | Przykład | Oczekiwanie | Pokrycie |
|---|---|---|---|---|
| EP-W2-010 | brak cookie | guest | `/login?redirectTo=` | existing-pom E2E-001 |
| EP-W2-011 | ważna sesja admin | storageState | dashboard 200 UI | existing-pom |
| EP-W2-012 | po logout aplikacji | sign out | login + blokada `/admin`; **brak** `end_session` | existing-pom E2E-010 |
| EP-W2-013 | idle 121s Session Lab | `clock.fastForward(121_000)` | lock + Unlock → `/login` | existing-pom E2E-012 |
| EP-W2-014 | wygasła sesja mid-journey | clear cookies na `/admin` | login | designed (nie E2E-012) |
| EP-W2-015 | logout RP OIDC | `session-lab-end-oidc` | `end_session` + `client_id`, bez `id_token_hint` | designed E2E-013 |

## Merchant reference / name (UI Zod)

| ID | Partycja | Przykład | Oczekiwanie | Pokrycie |
|---|---|---|---|---|
| BVA-W2-020 | reference 0–2 znaki | `""` / `"AB"` | field error, brak POST | existing-pom E2E-023 (pusty) |
| BVA-W2-021 | reference 3 znaki | unique ≥3 | API 201 | existing-pom API-001 |
| EP-W2-022 | name &lt; 2 | `""` | field error | existing-pom |
| EP-W2-023 | duplikat reference | drugi POST | 409 | existing-pom E2E-026 |
| EP-W2-024 | brak tenant (admin UI) | form bez pola | 400 | blocked GAP-W2-01 / existing-pom API-003 |

## Merchant status

| ID | Partycja | Oczekiwanie | Pokrycie |
|---|---|---|---|
| EP-W2-030 | DRAFT po create | badge Draft | existing-pom E2E-021 |
| EP-W2-031 | ACTIVE po activate | Active | existing-pom |
| EP-W2-032 | SUSPENDED po suspend | Suspended | existing-pom |

## HTTP Error Lab

| ID | Partycja | Status | Pokrycie |
|---|---|---|---|
| EP-W2-040 | validation | 400 | existing-pom |
| EP-W2-041 | unauthorized | 401 | existing-pom |
| EP-W2-042 | stale precondition | 412 | existing-pom |
| EP-W2-043 | rate limit | 429 | **poza** POM (mock BFF) |
| EP-W2-044 | 403/404/406/415 admin; 409/428/304 manager | BFF `page.request` + canary UI 401 | existing-pom E2E-083 |
| EP-W2-045 | idempotency replay / conflict | 200 / 409 | existing-pom E2E-091 |

## Checkout mode × outcome

| ID | Partycja | Oczekiwanie | Pokrycie |
|---|---|---|---|
| EP-W2-050 | ONLINE + Approve | CONFIRMED | existing-pom E2E-060 |
| EP-W2-051 | ONLINE + lie `status=success` | ≠ CONFIRMED | existing-pom E2E-061 |
| EP-W2-052 | CASH | CONFIRMED, brak hosted | existing-pom E2E-062 |
| EP-W2-053 | ONLINE + Decline | fulfillment CANCELLED | existing-pom E2E-063 |
| EP-W2-054 | lab flag off | skip / brak nav | existing-pom E2E-064 |
| EP-W2-055 | EXPIRED_LINK | `psp-link-expired` | existing-pom E2E-065 |

## Role × zasób

| ID | Partycja | Oczekiwanie | Pokrycie |
|---|---|---|---|
| EP-W2-060 | guest `/admin` | login | existing-pom |
| EP-W2-061 | manager Support Beta | problem, 0 rows | existing-pom |
| EP-W2-062 | admin notes na orderze Alpha | 201 lub 403 drift | existing-pom |
| EP-W2-063 | manager create order Alpha | 201 | existing-pom API-010 |
| EP-W2-064 | admin create order | 403 | existing-pom API-011 |
| EP-W2-065 | manager Beta payments | alert, brak tabeli | existing-pom E2E-100 |
| EP-W2-066 | CSRF bez tokenu | 403 `csrf_failed` | existing-pom E2E-121 |
