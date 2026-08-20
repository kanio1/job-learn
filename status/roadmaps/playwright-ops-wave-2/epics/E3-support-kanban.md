---
name: epic-e3-support-kanban
parent: playwright-ops-wave-2
epic: E3
tasks: [PW-OPS-T05, PW-OPS-T06, PW-OPS-T07, PW-OPS-T21]
last_updated: 2026-08-20
---

# Epic E3 — Support Kanban Queue (#11)

**Cel produktowy:** drugi tab Support: Work Queue z kolumnami NEW / IN_PROGRESS / WAITING / RESOLVED.  
**Cel dydaktyczny:** `dragTo` + menu Move, optimistic UI, rollback, `waitForResponse` PATCH.

**To nie jest** Merchant 360 payment Kanban (statusy `payment_orders` + POST authorize). Tutaj encja `support_cases`.

Nuxt UI: `UCard`, `UUser`, `UBadge`, `UTabs` (Search | Work Queue).  
POM: `KanbanBoardComponent`, `SupportCaseCard`.

---

## Story E3-S1 — Moduł support + REST

**Task:** `PW-OPS-T05`, `PW-OPS-T06` · P0

### Jako / chcę / aby

Jako support agent tworzę sprawę INC-* powiązaną z payment order i przesuwam status legalną ścieżką.

### Business case

`BC-OPS-11` — Support dziś to wyszukiwarka payments. Kolejka pracy to osobny lifecycle.

### Use case

`UC-OPS-11` — POST case → PATCH status z If-Match.

### State machine

```text
NEW → IN_PROGRESS → WAITING → RESOLVED
              ↑_________|
```

- WAITING → IN_PROGRESS dozwolone (wraca z oczekiwania).
- RESOLVED terminal. Cofnięcie → 409 `illegal_transition`.
- Skip (NEW→RESOLVED) → 409.
- Stale version → **412**.

### SQL

[01-infra B.1](../01-infra-postgres-keycloak-stack.md) V31. Dodać `support` do flyway locations. `SupportModuleTest`.

### REST (projekt)

| Method | Path | Auth |
|---|---|---|
| POST | `/api/support/cases` | operate |
| GET | `/api/support/cases?status=&assignee=` | read |
| GET | `/api/support/cases/{id}` | read |
| PATCH | `/api/support/cases/{id}` | operate + If-Match; body `{ status?, assigneeSubject? }` |

Create wymaga `merchantId`; `paymentOrderId` opcjonalny ale **rekomendowany** (lab: POM zawsze linkuje order). `caseReference` serwer generuje `INC-{compact}` albo client unique — **serwer** (uniknij collission w parallel).

Tenant: JWT tenant admin tylko swój `tenant_id`. Support agent platform-scoped jak `platform:payments:read`.

### Acceptance criteria

- [ ] Public package `lab.paymentquality.support` + internal layout.
- [ ] CHECK constraints + unique `case_reference`.
- [ ] ETag na GET/PATCH jak payments `"v{n}"`.
- [ ] Audit `AuditableActionOccurred` na create/move/assign.
- [ ] Seed nie wymagany; factory w RA/POM.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-OPS-110 | RA | POST 201 Location + ETag v0 status NEW |
| RA-OPS-111 | RA | PATCH NEW→IN_PROGRESS If-Match 200 |
| RA-OPS-112 | RA | PATCH NEW→RESOLVED 409 illegal_transition |
| RA-OPS-113 | RA | PATCH stale 412 |
| RA-OPS-114 | RA | PATCH bez If-Match 428 |
| RA-OPS-115 | RA | tenant isolation 404 |
| RA-OPS-116 | RA | readonly POST 403 |
| RA-OPS-117 | RA | manager POST 403 |
| RA-OPS-118 | RA | duplicate case_reference 409 (jeśli client-supplied) |
| RA-OPS-119 | RA | WAITING→IN_PROGRESS 200 |
| RA-OPS-120 | RA | GET list filter status IN_PROGRESS |
| RA-OPS-121 | RA | RESOLVED→IN_PROGRESS 409 |
| RA-OPS-122 | RA | Modulith verify + SupportModuleTest |

---

## Story E3-S2 — UI tab Work Queue

**Task:** `PW-OPS-T07` · P0

### Use case

`UC-OPS-12` — `/admin/support` tabs Search (istniejący) | Work Queue.

Kolumny z `role=region` / `data-testid="kanban-column-NEW"` itd. Karty `getByRole('article', { name: /INC-/ })` albo testid `support-card-{id}`.

### Acceptance criteria

- [ ] Puste kolumny dozwolone (`UEmpty` w kolumnie).
- [ ] Karta: reference, payment order ref, priority badge, assignee `UUser`.
- [ ] Search tab nie psuje się (istniejący support-rbac.spec nadal zielony).

---

## Story E3-S3 — Move + drag (optimistic)

**Task:** `PW-OPS-T07`, `PW-OPS-T21` · P0/P1

### Use case

`UC-OPS-18`

```text
drag card
  ↓
PATCH /api/support/cases/{id}
  ↓
optimistic movement
  ↓
API 200
  ↓
reload
  ↓
still IN_PROGRESS
```

P0: `UDropdownMenu` „Move to IN_PROGRESS” (a11y; drag flakowy).  
P1: `await caseCard.dragTo(inProgressColumn)`.

### Acceptance criteria

- [ ] `waitForResponse` PATCH **przed** asercją kolumny (promise before click).
- [ ] Reload board: karta w target.
- [ ] `data-testid` jeśli role niewystarczające.
- [ ] Keyboard: focus karty + menu (nie tylko pointer).

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-110 | E2E | menu Move NEW→IN_PROGRESS; PATCH 200; reload |
| PW-OPS-E2E-111 | E2E | `dragTo` happy path (`@flaky` OK; menu jest P0) |
| PW-OPS-E2E-114 | E2E | nielegalny drop RESOLVED← — 409 + karta źródłowa |

---

## Story E3-S4 — Rollback na 412

**Task:** `PW-OPS-T07` · P0

### Use case

`UC-OPS-19`

```text
drag
 ↓
PATCH → 412
 ↓
card initially moves
 ↓
rollback
 ↓
error toast
```

Drugi context: operator przesuwa tę samą kartę w międzyczasie.

### Acceptance criteria

- [ ] Optimistic dozwolony **tylko** z rollback na !2xx.
- [ ] Toast problem+json.
- [ ] Po rollback GET list zgadza się z UI.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-112 | E2E | drugi context PATCH w międzyczasie → 412 + rollback + toast |
| PW-OPS-E2E-113 | E2E | `toMatchAriaSnapshot` board fragment |
| PW-OPS-API-030 | PW REST | ten sam PATCH przez BffClient |

### Learning

`dragTo` + network coordination + discriminated union status. Component object, nie 200 locatorów w spec.
