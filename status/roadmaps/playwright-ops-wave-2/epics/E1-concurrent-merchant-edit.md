---
name: epic-e1-concurrent-merchant-edit
parent: playwright-ops-wave-2
epic: E1
tasks: [PW-OPS-T01, PW-OPS-T02, PW-OPS-T03, PW-OPS-T20]
last_updated: 2026-08-20
---

# Epic E1 — Concurrent Merchant Edit (#13)

**Cel produktowy:** dwóch operatorów nie nadpisuje merchanta po cichu; po 412 widać **Your vs Latest**.  
**Cel dydaktyczny:** dwa `BrowserContext`, ETag, 412/428, conflict workspace (`UTabs` / `UAlert` / `UModal`).

**Gate:** jeśli Merchant 360 T13 już dodał GET/activate If-Match — T02 tylko PATCH contact + conflict DTO. Kolumny contact i tak są Wave 2.

Nuxt UI: `UModal`, `UTabs`, `UAlert`, `UForm`.  
POM: `ConflictDiffComponent`, `fixtures/multi-user.fixture.ts`.

---

## Story E1-S1 — Merchant PATCH + ETag

**Task:** `PW-OPS-T01`, `PW-OPS-T02` · P0

### Jako / chcę / aby

Jako platform admin zapisuję telefon merchanta i dostaję nowy `ETag`, a drugi writer ze starym `If-Match` dostaje 412.

### Business case

`BC-OPS-13` — ERP optimistic lock. `merchants.version` już jest (`@Version`). HTTP jak płatności: `ETag: "v{n}"`, stale **412**, brak **428**. Nie 409.

### Use case

`UC-OPS-13` — GET merchant → PATCH `{ displayName?, contactPhone?, contactAddress? }` z If-Match.

### Domain rules

- PATCH JSON merge; puste stringi vs null: **null czyści pole**, omit = bez zmiany (udokumentować w OpenAPI przy implementacji).
- Bound: `displayName` 1–120 (istniejący), `contactPhone` max 32, `contactAddress` max 200.
- Tenant isolation jak GET merchant.
- Authority: `platform:merchants:update-status` **albo** nowy `platform:merchants:update` — **preferuj reuse update-status** + notes w AC, żeby nie mnożyć ról. Jeśli to za szerokie (activate vs edit), dodać `merchants:update` do PLATFORM_ADMIN i TENANT_ADMIN w T02.
- BFF forward `If-Match` / `ETag` (allowlist już jest).

### SQL

[01-infra B.2](../01-infra-postgres-keycloak-stack.md) V32.

### Acceptance criteria

- [ ] GET `/{id}` zwraca `ETag: "v{n}"` i body z contact fields (nullable).
- [ ] PATCH wymaga If-Match; brak → 428 Problem Details `requiredHeader=If-Match`.
- [ ] Stale version → 412; body **nie** zapisane; GET nadal stara wartość.
- [ ] Sukces → 200 + nowy ETag `v{n+1}` + audit event.
- [ ] REST Assured analog payment version tests.
- [ ] Readonly POST/PATCH → 403.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-OPS-001 | RA/IT | V32 kolumny istnieją; ddl validate |
| RA-OPS-050 | RA | PATCH displayName If-Match v0 → 200 ETag v1 |
| RA-OPS-051 | RA | PATCH bez If-Match → 428 |
| RA-OPS-052 | RA | PATCH If-Match v0 po innym save → 412; DB unchanged |
| RA-OPS-053 | RA | malformed If-Match → 400 (jak płatności) |
| RA-OPS-054 | RA | tenant.admin nie PATCH obcego merchanta → 404/403 (skopiować BOLA płatności) |
| RA-OPS-055 | RA | readonly PATCH → 403 |
| PW-OPS-API-010 | PW REST | BFF cookie forward If-Match / ETag |

---

## Story E1-S2 — Dwa contexty, 412

**Task:** `PW-OPS-T03`, `PW-OPS-T20` · P0

### Jako / chcę / aby

Jako operator B widzę „Record changed by another user”, nie cichy overwrite.

### Use case

`UC-OPS-14` — USER A (`platform.admin`) i USER B (`platform.operator`) ładują ACME v7. A zapisuje phone → v8. B zapisuje address ze starym ETag → 412.

```text
USER A                     USER B
ACME v7                    ACME v7
edit phone                 edit address
save → v8                  save v7
                           ↓
                      412 PRECONDITION FAILED
```

### Acceptance criteria

- [ ] Fixture `multiUser` daje `{ adminPage, operatorPage }` z osobnych `storageState`.
- [ ] Nie używać dwóch kart tego samego usera.
- [ ] Worker-safe: unikalny merchant (nie MERCHANT_ALPHA jeśli równoległy suite go mutuje) — `uniqueMerchantReference`.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-SEC-020 | SEC | A PATCH 200; B PATCH 412; B UI conflict visible |
| PW-OPS-SEC-021 | SEC | B Discard mine → form = server; brak drugiego PATCH ze starym body |
| PW-OPS-SEC-022 | SEC | B Reload latest → GET v8; Your tab znika |

---

## Story E1-S3 — Conflict workspace UI

**Task:** `PW-OPS-T03` · P0

### Use case

`UC-OPS-15` — Modal:

```text
Record changed by another user.
[Your changes] [Latest version]
Phone
YOUR:      +48 111...
SERVER:    +48 222...
[Discard mine] [Reload latest]
```

### Acceptance criteria

- [ ] `UAlert` role=alert.
- [ ] `UTabs` nazwy „Your changes” / „Latest version”.
- [ ] Discard = GET latest, zamknij modal, dirty=false.
- [ ] Reload latest = to samo + toast.
- [ ] Brak auto-retry PATCH.
- [ ] `toMatchAriaSnapshot` fragment dialogu.
- [ ] Focus trap / Escape zamyka **bez** zapisu.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-130 | E2E | tabs Your/Latest widoczne po 412 |
| PW-OPS-E2E-131 | E2E | Escape → modal hidden; GET version unchanged |
| PW-OPS-E2E-132 | E2E | aria snapshot conflict dialog |

### Learning

Interview: multi-user + optimistic locking + HTTP precondition. POM modeluje `ConflictDiff { yours, latest }` jako typy, nie stringi w spec.
