# 05 — Sesja, cookie, RBAC, IDOR

Warstwa: `chromium-guest` + admin/manager storageState. Tagi `@security`.

CSRF / clock / TLS lab: katalogi Mirror / TLS — tu tylko to, co Wave 2 wstawia w dashboard POM.

---

## A. Cookie i storageState

### PW-W2-SEC-001 — `nuxt-session` HttpOnly

| | |
|---|---|
| Pokrycie | existing-pom `expectSessionCookieHttpOnly` |
| Prio | P0 |
| Uczy | XSS w Vue nie czyta sesji przez `document.cookie`. |

### PW-W2-SEC-002 — Brak JWT w Web Storage

| | |
|---|---|
| Pokrycie | existing-pom `expectNoTokenInBrowserStorage` |
| Prio | P0 |
| Uczy | Access token siedzi po stronie BFF / cookie, nie `localStorage`. |

### PW-W2-SEC-003 — Plik storageState: JWT nie w `origins`

| | |
|---|---|
| Pokrycie | existing-pom `expectNoJwtInStorageStateFile` |
| Prio | P0 |
| Uczy | Sealed cookie **może** zawierać `eyJ` — nie skanować blobu ciasteczek. |

### PW-W2-SEC-004 — Odpowiedź Error Lab bez `Authorization` w body

| | |
|---|---|
| Pokrycie | existing-pom `expectNoAuthorizationInNetworkResponse` |
| Prio | P0 |

---

## B. Guest vs authenticated

### PW-W2-SEC-010 — Guest project bez `api` fixture

| | |
|---|---|
| Pokrycie | existing-pom (konwencja README + `requireApi`) |
| Prio | P0 |
| Uczy | Puste cookies → BFF 401; fixture `api` jest `undefined`. |

### PW-W2-SEC-011 — `redirectTo` wraca po login (happy)

| | |
|---|---|
| Pokrycie | designed (setup Keycloak loguje bez asercji `redirectTo`) |
| Prio | P1 |

### PW-W2-SEC-012 — Idle lock 121s → Unlock → `/login`

| | |
|---|---|
| Pokrycie | existing-pom E2E-012 `session-lab.spec.ts` |
| Prio | P0 |
| Uwaga | brak asercji ponownego `/admin/merchants` (MRL E2E-022 designed) |

---

## C. RBAC / IDOR

### PW-W2-SEC-020 — Manager Support Beta → problem, brak tabeli

| | |
|---|---|
| Pokrycie | existing-pom W2-11 |
| Prio | P0 |
| Uczy | Deep-link jest testem autoryzacji, nie UX hide. |

### PW-W2-SEC-021 — Manager `/admin/users` forbidden + Beta payments deny

| | |
|---|---|
| Pokrycie | existing-pom E2E-100 `auth-rbac.spec.ts` |
| Prio | P0 |
| Asercje | Users/Audit nav false; Beta alert; brak `payment-orders-table`; `users.expectForbidden()` |

### PW-W2-SEC-022 — Admin vs manager: Alpha OK, Beta deny (payments)

| | |
|---|---|
| Pokrycie | existing-pom payments isolation |
| Prio | P0 |

### PW-W2-SEC-023 — Notes/risk 403 = drift realm vs UI

| | |
|---|---|
| Pokrycie | existing-pom gałęzie 403 |
| Prio | P1 |
| Uczy | Nie oznaczać PASS bez asercji statusu. |

### PW-W2-SEC-024 — Dwa `BffClient` w jednym teście (admin + manager)

| | |
|---|---|
| Pokrycie | existing-pom notes |
| Prio | P0 |
| Uczy | Precondition innej roli bez wylogowania UI. |

---

## D. Designed security (odblokowane)

### PW-W2-SEC-030 — Guest POST `/api/merchants` 401

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Warstwa | `APIRequestContext` bez cookie (nie guest UI) |

### PW-W2-SEC-031 — CSRF fail path Session Lab

| | |
|---|---|
| Pokrycie | existing-pom E2E-121 (fail); happy + kontrast merchant POST **designed** (MRL) |
| Prio | P1 |

### PW-W2-SEC-032 — RLS Lab dual JWT

| | |
|---|---|
| Pokrycie | existing-pom `rls-lab.spec.ts` — katalog RFC |
| Prio | P0 |
| Uczy | Nie dublować TC. Oracle: [rls-filters-composition-lab](../rls-filters-composition-lab/03-playwright-e2e-catalog.md) E2E-002–005; compare JSON `restrictedWithoutTenantGuc`; merchant API 403 ≠ `rls_forbidden`. |
