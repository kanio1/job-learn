# 01 — As-built inventory (product + test analyst)

Pytanie: **co jest w kodzie 2026-08-13** i które przypadki są jeszcze tylko designed.  
To nie jest już mapa „Missing FR” sprzed implementacji — FR z [00-context-requirements.md](../../status/roadmaps/browser-session-visual-network-lab/00-context-requirements.md) są w produkcie (lab, nie bank).

## Werdykt

| Obszar | Produkt | Testy dziś | Luka testowa |
|---|---|---|---|
| Flaga FE `NUXT_PUBLIC_MIRROR_LAB_ENABLED` (default **on**) | Hub, nav, middleware 404, BFF `requireMirrorLab` | hub mocked; Spring IT flag off | FE flag-off project; BFF 404 bez Spring |
| Flaga Spring `app.mirror-lab.enabled` (yml **false**, dev **true**) | Controller `@ConditionalOnProperty` | `MirrorLabEndpointsEnabledIT` / `DisabledIT` | — |
| Session Lab | cookies, idle overlay, Unlock = logout **BFF**, End OIDC = `end_session`, CSRF, devices | POM session-lab + guest | hop OIDC designed; SameSite; cookie 4 KB; BVA TTL-1; CSRF isolation |
| Visual Lab | kafelki + dark + break | mocked `visual-lab.spec.ts` | full-page mask; ARIA snapshot |
| Network Lab | 503 TTL 10s, abort, lie, HAR, CORS cookie | mocked + POM 503 | `setOffline`; strip headers; CORS OPTIONS |
| CPL Wave 3 | GET-body 403, `?lang=`, refund `REFUNDED`, expiry testid, iframe, `trusted_merchant` | RA + mocked widget/idle-absent | lang copy hosted POM; duplicate refund eventId |
| Bank-like | step-up header, statements, disputes, maker-checker, AIS-lite | RA + POM rbac + live PDF | TPP 429; evidence UI 413 |
| Review fixes | idle re-auth, PDF arrayBuffer, BFF gate, TPP header, owner revoke, re-approve 409 | część RA/PW | BFF flag matrix; rate-limit; consent UI header |

**Rekomendacja PM:** nie budować banku ani ACR Keycloak. Dalsze testy = designed w 03–07. `REST-MULTIPART-01` nadal otwarty jako gate dokumentacyjny (evidence jest w RA, nie w `apps/api-tests`).

## Powierzchnie (kod)

```text
Nuxt UI     /admin/mirror-lab, /admin/session-lab, /admin/visual-lab, /admin/network-lab
            /admin/mirror-lab/bank, /admin/checkout-lab/widget, /consent/mirror-lab
            overlay SessionLabIdleLock w layout dashboard; hosted layout: false
Nitro BFF   /api/session-lab/*, /api/network-lab/*, /api/mirror-lab/*
Spring      /api/mirror-lab/*  (@Profile !prod)
            /api/checkout-lab/*  (GET-body filter, refund, oauth trusted_merchant, lang)
Flyway      mirrorlab/V15__create_mirror_lab.sql
            checkoutlab/V16__checkout_session_refunded_status.sql
```

Jedna flaga FE. CSRF **tylko** `POST /api/session-lab/csrf-demo`. TPP GET `permitAll` (lab AIS-lite) + rate limit + header.

## FR status vs kod

### Session

| ID | Produkt | Test |
|---|---|---|
| FR-S01 inspector | tabela cookie policy vs `document.cookie` | existing-pom (partial: nuxt-session HttpOnly). SameSite / live Secure / Keycloak-host cookies: designed |
| FR-S02 trzy origin | hosted `layout: false`; dashboard overlay | existing-pom hosted bez idle; POM guest |
| FR-S03 idle | `useIdleLock` 120s, `page.clock` | existing-pom lock + Unlock → `/login` |
| FR-S04a logout aplikacji | Unlock / Sign out → `auth.logout()` | existing-pom URL `/login` + empty `nuxt-session` + `/admin` → `/login` (`expectSessionCookieCleared`) |
| FR-S04b logout RP OIDC | `session-lab-end-oidc` + POST end-session | existing-pom `session.spec.ts` |
| FR-S05 concurrent | in-memory devices per session cookie | existing-pom dwa contexty revoke (cienko) |
| FR-S06 CSRF lab | cookie `mrl-csrf` non-HttpOnly; header match | existing-pom fail + happy + wrong token + kontrast merchant |
| FR-S07 guest | `session-guest.spec.ts` | existing-pom login redirect |
| FR-S08 no JWT storage | `storage-safety.ts` | existing-pom na session-lab |

### Visual / Network / PayU / Bank

| ID | Produkt | Test |
|---|---|---|
| FR-V01–V05 | Visual Lab tiles + break tag | existing-pom screenshots |
| FR-V04 full+mask | `visual-lab-mask.css` na kafelkach | Partial — brak full-page |
| FR-N01 | Nitro 503→200, TTL 10s | existing-pom live 503→200→503; TTL via GET `/api/network-lab/retry-window` (`remainingMs`). Mocked fulfill: designed (brak `tests/e2e`) |
| FR-N02 abort | `network-lab/slow` | designed (`route.abort` poza live POM); existing-pom offline → `error-state` |
| FR-N03 lie | `lie-fulfillment` | existing-pom body success; designed fulfillment oracle |
| FR-N04 strip | — | designed |
| FR-N05 HAR | fixture `network-lab.har` | designed (brak `tests/e2e`) |
| FR-N06 CORS | `/api/network-lab/cors-cookie` | existing-pom OPTIONS 204 + GET evil Origin |
| FR-P01 GET body | filter 403 `get_with_body` | existing-ra (HttpClient) |
| FR-P02 lang | Location `?lang=pl\|en` | existing-ra create; designed hosted copy |
| FR-P03 refund | status `REFUNDED`, event raz, drugi 409 | existing-ra |
| FR-P04 expiry UI | `psp-link-expired` | existing-pom EXPIRED_LINK |
| FR-P05 iframe | same-origin widget | existing-pom frameLocator Approve |
| FR-P06 grants | `trusted_merchant` + panel hub | existing-ra oauth; designed UI panel |
| FR-B01/B06 | header step-up, copy kwoty | existing-ra 403/200; designed UI modal |
| FR-B02 | CSV/PDF + BFF binary | existing-pom live PDF `%PDF-`; CSV download. Mocked fulfill: designed |
| FR-B03 | multipart 2 MiB / 415 | existing-ra; designed UI file |
| FR-B04 | PENDING → APPROVED, self 403, re-approve 409 | existing-ra + existing-pom fill+click |
| FR-B05 | grant / TPP header / revoke owner | existing-ra; designed UI consent |

## GAP IDs (luki **testowe**, nie produktowe)

| ID | Luka testowa | Warstwa | Prio |
|---|---|---|---|
| GAP-T01 | FE flag off: nav 0 + BFF 404 bez backendu | existing-pom `mirror-lab-flag-off.spec.ts` (:3012; csrf + statements) | P0 |
| GAP-T02 | Idle TTL-1 vs TTL+1; overlay na **każdym** `/admin/**` | existing-pom `session-lab.spec.ts` | P0 |
| GAP-T03 | CSRF happy path + merchant POST bez CSRF nadal 2xx | existing-pom `session-lab` + `admin-bff` | P1 |
| GAP-T04 | PDF magic bytes przez **żywy** BFF (nie fulfill) | existing-pom `mirror-lab.spec.ts` | P1 |
| GAP-T05 | TPP 31. GET → 429; header vs query precedence | RA | P1 |
| GAP-T06 | Evidence UI 413/415; filename `../` | POM + RA | P1 |
| GAP-T07 | CORS OPTIONS credentials | existing-pom `network-lab.spec.ts` | P1 |
| GAP-T08 | 503 counter reset po TTL 10s (izolacja) | existing-pom `network-lab.spec.ts` | P1 |
| GAP-T09 | Consent UI woła header, nie `?token=` | POM / mocked | P1 |
| GAP-T10 | Refund COMPLETED→REFUNDED w UI inspector | POM | P2 |
| GAP-T11 | `stepUpUntil` w JSON gdy amount ≥ 10000 | existing-ra; UI designed | P2 |
| GAP-T12 | Learner specs nadal skip | docs | P2 |

## Non-goals potwierdzone

Mobile, real PayU, Kafka, PAN/3DS, KYC, Berlin Group produkcyjny, Argos, ACR w realm, wiązanie `payment_orders`.
