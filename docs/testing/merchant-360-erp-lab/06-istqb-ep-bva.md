# 06 — Equivalence partitioning + BVA

Każda partycja → `PW-M360-E2E-*` / `API-*` / `RA-M360-*`.  
1 happy E2E + reszta granic na **BFF/RA** (analog `CreateUcEpRest`). Nie 15 E2E na 400.

Klasy docelowe: [09-agent-tests-pom-plan.md](09-agent-tests-pom-plan.md). Istniejące: `MerchantReferencePartitions`, `AmountPartitions`.

Pokrycie: **designed**.

---

## List query — merchants

| ID | Partycja | Przykład | Oczekiwanie | TC | Warstwa |
|---|---|---|---|---|---|
| EP-M360-010 | brak filtra | `page=0&size=20` | `content≤20`, `totalElements` | RA-010, API-001 | REST |
| EP-M360-011 | status DRAFT | `status=DRAFT` | same DRAFT | RA-011, E2E-022 | RA + 1 E2E |
| EP-M360-012 | status ACTIVE | | same ACTIVE | RA-011 | REST |
| EP-M360-013 | status SUSPENDED | | same SUSPENDED | RA-011 | REST |
| EP-M360-014 | status nielegalny | `status=CLOSED` | 400 | analog RA-015 | REST |
| EP-M360-015 | q hit | unique ref | 1 wiersz | E2E-021, RA-012 | hybryda |
| EP-M360-016 | q miss | `q=zz-no-hit-{uniq}` | empty UI, `content=[]` | E2E-050 | E2E |
| EP-M360-017 | tenant JWT | tenant.admin | brak Beta | RA-016 | REST |
| EP-M360-018 | platform tenantId | Alpha UUID | tylko Alpha | RA-017, E2E-032 | hybryda |
| BVA-M360-020 | size 0 | `size=0` | 400 | analog RA-014 | REST |
| BVA-M360-021 | size 1 | | 1 wiersz max | RA | REST |
| BVA-M360-022 | size 100 | | ≤100 | RA | REST |
| BVA-M360-023 | size 101 | | 400 | RA-014 | REST |
| EP-M360-019 | sort whitelist | `updatedAt,desc` | 200 monotonic | RA-013, E2E-020 | hybryda |
| EP-M360-020 | sort nielegalny | `revenue,desc` | 400 | API-002, RA-015 | REST |

---

## Payment list

| ID | Partycja | Oczekiwanie | TC |
|---|---|---|---|
| EP-M360-030 | status CREATED (regresja) | 200 | RA-024, E2E-027 |
| EP-M360-031 | AUTHORIZED…REFUNDED | 200, wiersze = status | RA-020, E2E-026 |
| EP-M360-032 | status NOPE | 400 | RA-021 |
| EP-M360-033 | sort createdAt | regresja | existing list RA |
| EP-M360-034 | sort amountMinor | monotonic | RA-022, E2E-025 |
| BVA-M360-035 | minAmount = maxAmount | 200, równość | RA-025 |
| BVA-M360-036 | minAmount > maxAmount | 400 | existing validate() |

---

## Create merchant (istniejące boundy Zod — nie zmyślać NIP)

Reuse [BVA-W2-020…](../live-pom-wave-2/06-istqb-ep-bva.md). Nowe ID tylko gdy kontrakt list zmienia overview.

| ID | Partycja | Przykład | Oczekiwanie | TC |
|---|---|---|---|---|
| BVA-M360-040 | reference 2 | `"AB"` | field error, **brak POST** | E2E-072 |
| BVA-M360-041 | reference 3 | unique | 201 | E2E-070, RA-030 |
| BVA-M360-042 | reference 64 | | 201 | E2E-072 |
| BVA-M360-043 | reference 65 | | field error / 400 | E2E-072, RA-030 |
| EP-M360-044 | displayName 1 vs 2 | | error vs 201 | RA + 0–1 E2E |
| EP-M360-045 | duplikat | drugi POST | 409 | E2E-071 |
| EP-M360-046 | platform bez tenant | body | 400 | existing API-003 / MER-05 |

---

## Import CSV

| ID | Partycja | Oczekiwanie | TC |
|---|---|---|---|
| EP-M360-050 | valid.csv | preview counts >0, DB unchanged | E2E-080, RA-060 |
| EP-M360-051 | invalid-header | error, 0 commit | E2E-081 |
| EP-M360-052 | empty.csv | validation | E2E-082 |
| EP-M360-053 | duplicate.csv vs UK | rejected | E2E-083, RA-062 |
| EP-M360-054 | utf8.csv | names z diakrytykami | E2E-080 wariant |
| EP-M360-055 | malformed row | rejected row, not 500 | RA-062 |
| BVA-M360-056 | 0 data rows | empty | E2E-082 |
| EP-M360-057 | drugi commit | 409 | E2E-085 |

10k rows: **RA only P2**, nie live POM.

---

## PATCH displayName / If-Match (E7)

| ID | Partycja | Oczekiwanie | TC |
|---|---|---|---|
| BVA-M360-060 | name 1 vs 2 vs 120 vs 121 | 400 / 200 | RA-090 |
| EP-M360-061 | brak If-Match | 428 | RA-091 |
| EP-M360-062 | stale `"v99"` | 412 | E2E-151, RA-052 analog |
| EP-M360-063 | świeży ETag | 200 | E2E-150 |

---

## Pagination EP

| ID | Partycja | Oczekiwanie | TC |
|---|---|---|---|
| EP-M360-070 | page 0 | first page | RA-010 |
| EP-M360-071 | page last | `content` może być krótszy | RA |
| EP-M360-072 | page ≥ totalPages | pusta `content` lub 400 (ustalić w E1 — **jedna** semantyka) | RA |
| EP-M360-073 | Apply z `?page=1` | reset page 0 | E2E-028, E2E-030 |
