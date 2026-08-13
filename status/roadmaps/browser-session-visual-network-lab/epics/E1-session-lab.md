---
name: epic-e1-session-lab
parent: browser-session-visual-network-lab
epic: E1
tasks: [MRL-T02, MRL-T03, MRL-T04, MRL-T05, MRL-T12, MRL-T13]
last_updated: 2026-08-13
---

# Epic E1 — Browser Session Lab

**Cel produktowy:** lustrzana sesja bankowości / panelu PayU vs publiczny checkout.  
**Cel dydaktyczny:** cookies, `storageState`, idle, logout, CSRF vs Bearer, guest context.

**Połączenia:** [01-infra-keycloak-bff-security.md](../01-infra-keycloak-bff-security.md), `tests-pom/utils/storage-safety.ts`, `playwright.pom.config.ts` guest project.

Lustra rynkowe: idle timeout SCA, SSO logout, „aktywne urządzenia”, cookie HttpOnly na sesji bankowej, hosted PayU bez cookie merchanta.

---

## Story E1-S1 — Cookie inspector  
**Task:** `MRL-T02` · P0 · FR-S01 FR-S02

### Jako / chcę / aby
Jako SDET chcę tabelę cookies widocznych dla automatyzacji vs `document.cookie`, aby zrozumieć HttpOnly i Domain.

### Acceptance criteria
- [ ] UI `/admin/session-lab` (lub karta na hubie) z tabelą: name, httpOnly, secure, sameSite, path, domain, expires.
- [ ] Wiersz `nuxt-session` po zalogowaniu; HttpOnly = true.
- [ ] Panel „JS-visible cookies” (`document.cookie`) **nie** zawiera `nuxt-session`.
- [ ] Sekcja „Keycloak host cookies”: copy że żyją na origin IdP; link/instrukcja `context.cookies()` bez filtrowania do :3000.
- [ ] Sekcja hosted: przycisk otwiera `/psp/checkout/{demo}` w nowej karcie + asercja copy „public page must not require nuxt-session”.
- [ ] `data-testid="session-lab-cookie-table"`.

### Learning
- `CK:` HttpOnly, SameSite, Domain.
- `PW:` `page.context().cookies()` vs `page.evaluate(() => document.cookie)`.
- `KC:` cookies IdP ≠ BFF.

### Połączone z
- CPL PW-E2E-072 (no Keycloak cookie on hosted)
- `expectSessionCookieHttpOnly`

---

## Story E1-S2 — Idle lock  
**Task:** `MRL-T03` · P0 · FR-S03

### Jako / chcę / aby
Jako operator chcę lock screen po bezczynności, jak w bankowości elektronicznej, bez biometrii.

### Acceptance criteria
- [ ] Konfigurowalny TTL (`MRL_IDLE_SECONDS`, default 120 w dev).
- [ ] Overlay lock na `/admin/**` z countdown i `data-testid="session-lab-idle-lock"`.
- [ ] Hosted public **nie** pokazuje idle lock.
- [ ] Unlock → login / OIDC (świat A).
- [ ] Test design: `page.clock.install({ datetime })` + `fastForward`; zakaz sleep.
- [ ] Copy: PSD2/SCA session timeout (edukacja), nie compliance claim.

### Learning
- `PW:` `page.clock`.
- `KC:` re-auth ≠ lab Bearer.
- `HTTP:` 401/302 na BFF po wygaszeniu sesji.

### Połączone z
- INFRA-BFF-02
- Visual Lab lock-screen tile (E2)

---

## Story E1-S3 — Logout + empty storageState  
**Task:** `MRL-T04` · P0 · FR-S04 FR-S07

### Jako / chcę / aby
Jako SDET chcę pełny logout, aby nowy context bez `storageState` nie widział `/admin`.

### Acceptance criteria
- [ ] Logout czyści `nuxt-session` (Set-Cookie max-age=0 lub brak cookie).
- [ ] Redirect na `/login` (i dokumentowany hop Keycloak end-session jeśli już w nuxt-auth-utils).
- [ ] POM: `test.use({ storageState: { cookies: [], origins: [] } })` → `/admin/merchants` pokazuje login, nie dane.
- [ ] Po logout `expectNoTokenInBrowserStorage`.

### Learning
- `PW:` reset storageState per file (Playwright auth docs).
- `KC:` SSO vs application session.

---

## Story E1-S4 — Concurrent sessions  
**Task:** `MRL-T12` · P1 · FR-S05

### Jako / chcę / aby
Jako bank chcę listę sesji i revoke jednej, aby ćwiczyć dwa `BrowserContext`.

### Acceptance criteria
- [ ] `GET/POST` lab devices (INFRA-BFF-04).
- [ ] UI lista + Revoke.
- [ ] AC testowe: dwa contexty, ten sam user; revoke A → A 401, B nadal 200 **albo** (toggle) revoke-all.
- [ ] Brak Keycloak Admin API w wave 1.

### Learning
- `PW:` `browser.newContext({ storageState })` × 2.
- `CK:` dwa pliki state vs jedno konto.

---

## Story E1-S5 — CSRF vs Bearer  
**Task:** `MRL-T13` · P1 · FR-S06

### Jako / chcę / aby
Jako HTTP expert chcę 403 na BFF cookie POST bez CSRF i **brak** CSRF na Bearer API.

### Acceptance criteria
- [ ] `GET /api/session-lab/csrf` → token.
- [ ] `POST /api/session-lab/csrf-demo` bez tokenu → 403 `csrf_failed` problem+json.
- [ ] Z poprawnym tokenem → 204/200.
- [ ] RA: istniejący POST payment-order z JWT **bez** CSRF header → sukces (kontrast).
- [ ] Copy UI: cookie session needs CSRF; API bearer does not.

### Learning
- `HTTP:` 403 vs 401.
- `REST:` SameSite nie zastępuje CSRF na wszystko.
- `PW:` mocked fulfill 403; POM waitForResponse 403.

### Połączone z
- INFRA-BFF-03 — **tylko** lab path, nie cały BFF.

---

## Story E1-S6 — storageState hygiene  
**Task:** `MRL-T05` · P0 · FR-S07 FR-S08

### Jako / chcę / aby
Jako SDET chcę setup Keycloak zapisujący state **bez** JWT w JSON i Web Storage.

### Acceptance criteria
- [ ] `expectNoJwtInStorageStateFile` w setup (już szkic — obowiązkowy w katalogu POM).
- [ ] Copy na Session Lab: Playwright **nie** persistuje sessionStorage; snippet `addInitScript`.
- [ ] Guest project w `playwright.pom.config.ts` pokryty specem `session-guest` (designed).
- [ ] Learner: puste `specs/session-guest.spec.ts` placeholder w wave 2 (E6-S3).

### Learning
- `PW:` setup project, gitignore `.auth`.
- `CK:` cookies + localStorage w pliku; sessionStorage osobno.

### Połączone z
- `apps/frontend/tests-pom/auth/keycloak.setup.ts`
- Playwright Authentication docs
