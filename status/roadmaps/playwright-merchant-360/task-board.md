---
name: playwright-merchant-360-tasks
origin: POST_KIRO_WORK
status: DONE
last_updated: 2026-08-20
---

# Task board — kolejność implementacji

Statusy: `OPEN` / `DONE`. Implementacja od T01.

| ID | Fala | Epic | Treść | Testy (projektowane) | Status |
|---|---|---|---|---|---|
| PW-M360-T00 | 0 | E0 | Ten katalog + research + catalog BC/UC/BF/EP/AT/POM plan | — | DONE (docs) |
| PW-M360-T01 | 1 | E1 | Flyway V23 merchant list indexes | RA-M360-001 (IT schema) | DONE |
| PW-M360-T02 | 1 | E1 | `GET /api/merchants` Page DTO + query whitelist + tenant isolation | RA-M360-010…018 | DONE |
| PW-M360-T03 | 1 | E1 | Payment list: pełny `status` enum + `sort=amountMinor` + V24 index | RA-M360-020…026 | DONE |
| PW-M360-T04 | 1 | E1 | Nitro BFF query forward + Zod `merchantList*` breaking change | PW-M360-API-001…004 | DONE |
| PW-M360-T05 | 1 | E1 | Overview / `BffClient.listMerchants` na `content` | PW-M360-E2E-001 (overview count) | DONE |
| PW-M360-T06 | 2 | E2 | `MerchantTable` server sort/filter/page URL | PW-M360-E2E-020…032 | DONE |
| PW-M360-T07 | 2 | E2 | Row selection + bulk activate per-row error | PW-M360-E2E-040…042 | DONE |
| PW-M360-T08 | 2 | E2 | Empty / loading / 403 list | PW-M360-E2E-050…052 | DONE |
| PW-M360-T09 | 2 | E2 | Payment `UTable` sort amount + status AUTHORIZED… | PW-M360-E2E-025…028 | DONE |
| PW-M360-T10 | 3 | E3 | `USlideover` 360 + Escape/focus/ARIA | PW-M360-E2E-060…063 | DONE |
| PW-M360-T11 | 3 | E3 | Form DT/BVA na istniejących polach | PW-M360-E2E-070, RA-M360-030 | DONE |
| PW-M360-T12 | 4 | E4 | RBAC kolumny vs 403 API | PW-M360-SEC-010…014, RA-M360-040 | DONE |
| PW-M360-T13 | 4 | E4 | Merchant ETag / If-Match 412/428 | RA-M360-050…055, PW-M360-SEC-020 | DONE |
| PW-M360-T14 | 5 | E5 | CSV import preview+commit | PW-M360-E2E-080…085, RA-M360-060 | DONE |
| PW-M360-T15 | 5 | E5 | Payment Kanban drop + Move menu + rollback | PW-M360-E2E-090…094 | DONE |
| PW-M360-T16 | 6 | E6 | Org `UTree` lazy | PW-M360-E2E-100…104 | DONE |
| PW-M360-T17 | 6 | E6 | Command palette entity search (live last-response) | PW-M360-E2E-110…112 | DONE |
| PW-M360-T18 | 6 | E6 | Charts z summary | PW-M360-E2E-120, PW-M360-API-050 | DONE |
| PW-M360-T19 | 7 | E7 | Calendar / Timeline / Stepper / Editor | PW-M360-E2E-130…141 | DONE |
| PW-M360-T20 | 7 | E7 | Editable grid displayName + If-Match | PW-M360-E2E-150, RA-M360-090 | DONE |

## Zależności

```text
T00
 └─ T01 → T02 → T04 → T05 → T06 → T07 → T08
              └─ T03 → T09
                         └─ T10 → T11
                         └─ T12 (może równolegle po T06)
                         └─ T13 (po T02; UI po T10)
                                └─ T14, T20
                         └─ T09 → T15
                         └─ T16, T17, T18 (po T02/T04)
                                └─ T19
```

Nie startować T06 zanim T02+T04 nie przejdą RA na Testcontainers **i** smoke na `--app`.
