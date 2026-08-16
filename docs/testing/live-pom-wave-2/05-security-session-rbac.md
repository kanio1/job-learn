# 05 — Sesja, cookie, RBAC, IDOR

Warstwa: `chromium-guest` + admin/manager storageState. Tagi `@security`.

CSRF / clock: Mirror Lab. TLS HTTPS (E2E-050–056, SEC-RFC-001): [09](../rls-filters-composition-lab/09-wave-b-stack-tls-catalog.md) i [wave-3](../wave-3-compose-tls-pom/). Tu dashboard HTTP POM.

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

### PW-W2-SEC-005 — Cookie &lt; 4 KB; brak `id_token` w sesji — **designed**

| | |
|---|---|
| Pokrycie | designed · UC-SESS-04 |
| Prio | P0 |
| Uczy | Drugi JWT w `nuxt-session` zrzuca ciasteczko (UA ~4096 B). Produkt: tylko `accessToken`. |

### PW-W2-SEC-006 — SameSite + Secure z `context.cookies()` — **designed**

| | |
|---|---|
| Pokrycie | designed (HttpOnly = SEC-001 existing). TLS Secure = RFC E2E-056 existing |
| Prio | P1 |
| Uczy | JSON `/api/session-lab/cookie-policy` ma zahardkodowane `secure: false` — nie jest oraclem HTTPS. |

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
| Pokrycie | existing-pom `session-guest.spec.ts` · `login with redirectTo returns to the intended admin path` |
| Prio | P1 |
| Kroki | `goto('/admin/users')` → Keycloak → callback |
| Asercje | URL `/admin/users`; heading `Users` |

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

## D. Guest BFF i security labs

### PW-W2-SEC-030 — Guest GET/POST `/api/merchants` 401

| | |
|---|---|
| Pokrycie | existing-pom `session-guest.spec.ts` · `guest BFF merchants GET and POST return 401` |
| Prio | P1 |
| Warstwa | `page.request` bez cookie (nie destructure `api`) |

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
| Uczy | Nie dublować TC. Oracle HTTP: [RFC 03](../rls-filters-composition-lab/03-playwright-e2e-catalog.md) E2E-002–005. HTTPS: E2E-054 `tls-lab.spec.ts`. |
