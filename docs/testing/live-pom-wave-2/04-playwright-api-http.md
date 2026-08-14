# 04 — HTTP / REST (BFF `APIRequestContext` + `waitForResponse`)

Warstwa: Nuxt `:3000` z ciasteczkiem sesji. **Nie** nowy suite REST Assured. Czysty kontrakt Spring zostaje w `*Test.java` (`existing-ra`).

`BffClient` baseURL: `http://127.0.0.1:3000`. Browser: `http://localhost:3000`.

---

## A. Merchants BFF

### PW-W2-API-001 — POST merchant 201 + GET 200

| | |
|---|---|
| Pokrycie | existing-pom `createMerchant` + `assertPersistedMerchant` |
| Method / path | `POST /api/merchants` → `GET /api/merchants/{id}` |
| Body | `reference`, `displayName`, `tenantReference=TENANT_ALPHA` |
| Status | 201 potem 200 |
| Uczy | Platform create **wymaga** tenanta (GAP-W2-01). |

### PW-W2-API-002 — POST duplikat reference 409

| | |
|---|---|
| Pokrycie | existing-pom `merchants.spec.ts` |
| Status | 409 |
| Uczy | Unikalność jest serwerowa; UI Zod tego nie łapie. |

### PW-W2-API-003 — POST bez tenantReference 400

| | |
|---|---|
| Pokrycie | designed (obserwowane: `MissingTenantReferenceException`) |
| Prio | P0 |
| Status | 400 problem+json |
| Uczy | Wyjaśnia, czemu Wave 2 nie robi UI POST jako admin. |

### PW-W2-API-004 — GET nieistniejący merchant 404

| | |
|---|---|
| Pokrycie | designed PW / `existing-ra` list/get |
| Prio | P1 |

---

## B. Payment orders i notes

### PW-W2-API-010 — Manager POST payment-order Alpha 201

| | |
|---|---|
| Pokrycie | existing-pom precondition `internal-notes.spec.ts` |
| Headers | `Idempotency-Key` |
| Uczy | Admin storageState **nie** ma `merchant:payments:create`. |

### PW-W2-API-011 — Admin POST payment-order 403

| | |
|---|---|
| Pokrycie | designed (omijane: `internal-notes` używa `BffClient` managera; E2E-100 pokazuje deny Beta, nie create 403) |
| Prio | P1 |

### PW-W2-API-012 — POST notes 201 i GET list zawiera body

| | |
|---|---|
| Pokrycie | existing-pom gałąź 201 |
| Path | `POST/GET .../payment-orders/{id}/notes` |
| Alternatywa | 403 jeśli realm bez `platform:payments:notes:*` |

### PW-W2-API-013 — PATCH risk-flag 200 vs 403

| | |
|---|---|
| Pokrycie | existing-pom `waitForResponse` `/risk-flag` |
| Uczy | Capability UI ≠ authority JWT. |

---

## C. Error Lab (żywy backend)

Nie wołać `trigger-429` (BFF mock).

### PW-W2-API-020 — GET/POST trigger-400 → 400 problem+json

| | |
|---|---|
| Pokrycie | existing-pom `waitForResponse` `/api/error-lab/trigger-400` |
| Asercje | status 400; `expectNoAuthorizationInNetworkResponse` |

### PW-W2-API-021 — trigger-401 → 401

| | |
|---|---|
| Pokrycie | existing-pom |

### PW-W2-API-022 — trigger-412 → 412

| | |
|---|---|
| Pokrycie | existing-pom W2-12 |

### PW-W2-API-023 — trigger 403 / 404 / 428

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |

---

## D. Nagłówki z UI (`waitForRequest`) — odblokowane, nie Wave 2 core

Wzorzec już w `payments-*.spec.ts` (manager). Wave 2 nie dodaje nowych asercji Idempotency/ETag, ale ten sam `page` + live BFF.

### PW-W2-API-030 — Idempotency-Key na create order (UI request)

| | |
|---|---|
| Pokrycie | existing-pom `payments-create.spec.ts` · E2E-090 |
| Prio | P0 |
| Asercja | `requestHeader(posted, 'Idempotency-Key')` = wartość z pola |

### PW-W2-API-031 — If-Match authorize/capture + 412 stale

| | |
|---|---|
| Pokrycie | existing-pom `payments-lifecycle.spec.ts` · E2E-092/093; Error Lab E2E-082 |
| Prio | P0 |
| HTTP | GET ETag → POST authorize/capture; stale `"stale-etag"` → 412, status CREATED |

### PW-W2-API-032 — Replay ten sam klucz → 200; mismatch 409

| | |
|---|---|
| Pokrycie | existing-pom E2E-091 `idempotency_conflict` |
| Prio | P0 |

### PW-W2-API-033 — Export CSV / audit JSON bez Bearer w ciele i pliku

| | |
|---|---|
| Pokrycie | existing-pom E2E-097, E2E-111 |
| Prio | P1 |
| HTTP | GET `.../payment-orders/export`; GET `/api/audit/export.json` |

### PW-W2-API-034 — Tenant PATCH If-Match = GET ETag

| | |
|---|---|
| Pokrycie | existing-pom E2E-112 |
| Path | `GET/PATCH /api/tenants/current/settings` |

### PW-W2-API-035 — CSRF demo 403 `csrf_failed`

| | |
|---|---|
| Pokrycie | existing-pom E2E-121 |
| Path | POST `/api/session-lab/csrf-demo` |

### PW-W2-API-036 — Network 503 potem 200 (żywy retry, nie fulfill)

| | |
|---|---|
| Pokrycie | existing-pom E2E-123 |
| Path | POST `/api/network-lab/trigger-503-retry` |

---

## E. REST Assured (nie implementować w PW)

| ID | Temat | Pokrycie |
|---|---|---|
| RA-W2-REF-01 | Merchant create/list/activate | `existing-ra` merchant tests |
| RA-W2-REF-02 | Payment create/idempotency/ETag | PHASE_7* |
| RA-W2-REF-03 | Tenant isolation | PHASE_8C |
| RA-W2-REF-04 | Checkout protocol | CPL RA — nie tu |

Playwright Wave 2 sprawdza, że **dashboard naprawdę wysyła** i **BFF zwraca** te statusy, nie że JDBC zgadza się 1:1.
