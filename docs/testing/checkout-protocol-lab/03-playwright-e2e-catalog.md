# 03 — Katalog Playwright E2E (UI)

Warstwa: przeglądarka Chromium. Domyślnie obecny spec **mockuje** BFF (`context.route`). Kolumna Pokrycie: `existing-pw` | `designed`.

**Stałe selektory:** patrz tabele.  
**SSR:** `/error-lab` tylko przez sidebar po SPA `/admin/*` — nigdy `page.goto('/error-lab')` przy mocku sesji.  
**Oracle pieniędzy:** `data-testid="fulfillment-status"` na booking/return **albo** odpowiedź BFF fulfillment — nie sam `return-hint`.  
**F-D2:** poza tym katalogiem. Te same `psp-approve` / `psp-decline` na `/psp-redirect-simulator` — asertuj URL `/psp/checkout/`.

Legenda kolumn TC: Priorytet P0–P2; Auth = Keycloak mock / public / none.

---

## A. Wejścia i nawigacja

### PW-E2E-001 — Hub uczy identity worlds i otwiera booking

| | |
|---|---|
| Pokrycie | existing-pw |
| Prio | P0 |
| Auth | Keycloak mock |
| Preconditions | `mockAuthenticatedSession`; `mockCheckoutLab` |
| Kroki | `goto /admin/checkout-lab` → widać „Hosted capability” i „Three identity worlds” → click `checkout-lab-open-booking` |
| Asercje | `checkout-booking-submit` visible |
| Learning | Cztery światy tożsamości na jednej stronie |

### PW-E2E-002 — Sidebar otwiera hub

| | |
|---|---|
| Pokrycie | existing-pw |
| Prio | P0 |
| Kroki | `goto /admin/merchants` → `nav-link-checkout-lab` |
| Asercje | tekst „Three identity worlds” |

### PW-E2E-003 — Error Lab karta → hub

| | |
|---|---|
| Pokrycie | existing-pw |
| Prio | P1 |
| Kroki | `goto /admin/merchants` → `nav-link-error-lab` → `checkout-lab-from-error-lab` |
| Asercje | „Hosted capability”; heading Error Lab przed click |
| Uwaga | Nie `goto /error-lab` (SSR) |

### PW-E2E-004 — Hub → Inspector

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Kroki | hub → `checkout-lab-open-inspector` |
| Asercje | URL `/admin/checkout-lab/inspector`; `inspector-load` visible |

### PW-E2E-005 — Overview karta Checkout Lab

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Kroki | `goto /` (index) jako zalogowany → `overview-checkout-lab` |
| Asercje | hub visible |

### PW-E2E-006 — Command palette → Error Lab / Checkout Lab

| | |
|---|---|
| Pokrycie | existing-pom `command-palette.spec.ts` (Error Lab + ARIA). Checkout Lab z palety: **designed** |
| Prio | P2 |
| Kroki | Ctrl+K → wpisz „Checkout Lab” → Enter |
| Asercje | URL `/admin/checkout-lab` |
| Selectory | `search-link-checkout-lab` jeśli widoczny w sidebar search |

### PW-E2E-007 — Unauth dashboard → login

| | |
|---|---|
| Pokrycie | existing-pom `session-guest.spec.ts` (E2E-003: `/admin/checkout-lab` + merchants/session-lab) |
| Asercje | URL `/login`; **nie** hub |
| Uwaga | storageState chromium ma platform-operator — użyj `browser.newContext()` bez stanu + bez mocka |

### PW-E2E-008 — Flaga `checkoutLabEnabled=false` ukrywa nav

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Preconditions | webServer env `NUXT_PUBLIC_CHECKOUT_LAB_ENABLED=false` (osobny project) **lub** skip jeśli nie da się przełączyć bez restartu |
| Asercje | `nav-link-checkout-lab` count 0; Error Lab karta CPL count 0 |

---

## B. Booking Lab

Form: `checkout-booking-form`. Pola: `checkout-booking-ext-order` (min 3), `checkout-booking-amount` (1…1e8), `checkout-booking-currency` (PLN/EUR/USD), `checkout-booking-mode` (ONLINE/CASH), submit `checkout-booking-submit`.

Learning copy (asertuj tekst alertu): **POST /bookings = 200 JSON `redirectUri`**, nie 302. Hop 302 jest na `POST /sessions`.

### PW-E2E-010 — ONLINE booking → AWAITING_PAYMENT + Open hosted

| | |
|---|---|
| Pokrycie | existing-pw (część multi-tab) |
| Prio | P0 |
| Kroki | booking → submit (default ONLINE) |
| Asercje | `fulfillment-status` = `AWAITING_PAYMENT`; `checkout-open-hosted` visible; debug panel nie pokazuje pustego `Location` jako sukcesu 302 |
| Network | `waitForResponse` POST `/api/checkout-lab/bookings` status 200; body `mode=ONLINE`, `sessionId` uuid, `redirectUri` zawiera `/psp/checkout/` |

### PW-E2E-011 — CASH przez **select mode** (nie prefix CASH-)

| | |
|---|---|
| Pokrycie | existing-pw (mock `CASH-*`) **+ existing-pom** `checkout-lab.spec.ts` · `cash booking confirms fulfillment without hosted checkout` (`chooseMode('CASH')`) |
| Prio | P0 |
| Kroki | ustaw `checkout-booking-mode` = CASH → submit |
| Asercje | `fulfillment-status` = `CONFIRMED`; `checkout-open-hosted` count 0; body `sessionId` null, `redirectUri` null, `validityUntil` null |
| Mock | honoruj `body.mode === 'CASH'`, nie tylko prefix |

### PW-E2E-012 — CASH istniejący prefix (regresja mocka)

| | |
|---|---|
| Pokrycie | existing-pw |
| Prio | P1 |
| Kroki | `extOrderId` `CASH-{ts}` → submit |
| Asercje | CONFIRMED, brak hosted button |

### PW-E2E-013 — Countdown `validityUntil` (ONLINE)

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Asercje | po 200 widać ExpirationCountdown (tekst „Link validity” / remaining); brak countdown dla CASH |

### PW-E2E-014 — Walidacja Zod: extOrderId za krótki

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Kroki | wypełnij 2 znaki → submit |
| Asercje | **brak** POST bookings (albo 0 requestów); komunikat walidacji przy polu; fulfillment-status nie pojawia się |

### PW-E2E-015 — Walidacja amount 0 / powyżej 1e8

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Kroki | amount `0` oraz `100000001` (dwa przebiegi) |
| Asercje | brak udanego POST; analogicznie do BVA-002 |

### PW-E2E-016 — Currency EUR i USD (happy UI)

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Kroki | select EUR → ONLINE submit; powtórz USD |
| Asercje | 200; hosted DTO pokazuje tę samą walutę po otwarciu (jeśli live/mock zwraca pole) |

### PW-E2E-017 — Booking API 400 problem+json

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Mock | POST bookings → 400 `application/problem+json` `{error: validation, status: 400}` |
| Asercje | `ProblemDetailsCard` visible; toast/error; brak `checkout-open-hosted` |

### PW-E2E-018 — Booking 401 (BFF bez lab token — symulacja)

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Mock | 401 empty |
| Asercje | ErrorState / problem; użytkownik nie widzi CONFIRMED |

### PW-E2E-019 — Copy 200 vs 302

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Asercje | alert zawiera „200 JSON” i „302 Location”; **nie** twierdzi, że booking zwraca 302 |

---

## C. Hosted PSP (`/psp/checkout/{sessionId}`)

Selectory: `psp-hosted-checkout`, `psp-approve`, `psp-decline`, `psp-outcome`. Layout `false` — bez dashboard chrome.

### PW-E2E-020 — Hosted public bez sesji dashboard

| | |
|---|---|
| Pokrycie | existing-pw |
| Prio | P0 |
| Auth | `browser.newContext()` bez storage |
| Kroki | `goto /psp/checkout/{SESSION_ID}` |
| Asercje | `psp-hosted-checkout` + `psp-approve` visible; **nie** login |

### PW-E2E-021 — Approve wysyła `Lab-Simulate-Token`

| | |
|---|---|
| Pokrycie | existing-pw (assert header w mocku multi-tab) |
| Prio | P0 |
| Network | POST `**/simulate` header `lab-simulate-token` = token z GET |
| Asercje | 200; `psp-outcome` visible; Approve/Decline **ukryte** (`!outcome`) |

### PW-E2E-022 — Decline → outcome CANCELED + return failure

| | |
|---|---|
| Pokrycie | existing-pom `hosted decline leaves fulfillment cancelled` (hint zawiera `failure`; fulfillment `CANCELLED`) |
| Asercje | simulate body `{outcome: CANCELED}`; return URL `status=failure`; fulfillment (live lub mock) → `CANCELLED` nie `CANCELED` |
| Learning | Session `CANCELED` vs fulfillment `CANCELLED` |

### PW-E2E-023 — Przyciski znikają po outcome

| | |
|---|---|
| Pokrycie | designed (częściowo w 021) |
| Prio | P1 |
| Asercje | po Approve: `psp-approve` i `psp-decline` count 0 |

### PW-E2E-024 — Expired link blokuje Approve

| | |
|---|---|
| Pokrycie | existing-pom `mirror-lab.spec.ts` · `psp-link-expired` visible. Blokada Approve / 409 `expired_link` w UI: **designed** |
| Asercje | alert „Payment link expired”; `psp-approve` count 0 |

### PW-E2E-025 — Simulate 403 missing/invalid token — UI ErrorState

| | |
|---|---|
| Pokrycie | designed |
| Prio | P0 |
| Mock POST | 403 problem `error: missing_simulate_token` (lub `invalid_simulate_token`) |
| Asercje | ErrorState / ProblemDetails; **nie** ekran Keycloak; nie mylić z 401 |

### PW-E2E-026 — GET hosted 404

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Kroki | nieznany UUID v4 |
| Mock | 404 `not_found` |
| Asercje | ErrorState; brak Approve |

### PW-E2E-027 — ApiDebugPanel GET i simulate

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Asercje | panel pokazuje GET path hosted oraz po click POST simulate (status 200/403) |

### PW-E2E-028 — Countdown na hosted z `validityUntil`

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Asercje | tekst Validity + countdown; po EXPIRED znika para Approve/Decline |

### PW-E2E-029 — Public DTO nie wycieka notifyUrl w UI

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Asercje | strona **nie** renderuje `notifyUrl`; debug body (jeśli pokazuje JSON) nie zawiera klucza `notifyUrl` |

### PW-E2E-030 — Double-click Approve

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Kroki | dwa szybkie click Approve |
| Asercje | drugi request: 200 noop (terminal) **lub** UI blokuje drugi click; fulfillment nie CONFIRMED dwukrotnie (jedno eventId) — przy live backend |

---

## D. Return (`/checkout-lab/return`) — public

Selectory: `checkout-return`, `return-hint`, `fulfillment-status`.

### PW-E2E-040 — Lie return: hint success, fulfillment AWAITING_PAYMENT

| | |
|---|---|
| Pokrycie | existing-pw **+ existing-pom** `lie return keeps fulfillment unconfirmed` |
| Prio | P0 |
| Auth | public context / admin POM po bookingu |
| Kroki | `goto /checkout-lab/return?sessionId={id}&status=success` bez simulate |
| Asercje | `return-hint` = `success`; `fulfillment-status` nie CONFIRMED (`AWAITING_PAYMENT\|UNKNOWN`) |

### PW-E2E-041 — Happy return po Approve → CONFIRMED

| | |
|---|---|
| Pokrycie | existing-pw **+ existing-pom** `hub opens booking; online pay uses hosted tab and fulfillment oracle` |
| Prio | P0 |
| Asercje | CONFIRMED; hint może być success — oracle i tak fulfillment |

### PW-E2E-042 — Return po Decline → CANCELLED

| | |
|---|---|
| Pokrycie | existing-pom `hosted decline leaves fulfillment cancelled` |
| Asercje | hint `failure`; fulfillment `CANCELLED` |

### PW-E2E-043 — PAY_NO_RETURN: Approve, nie odwiedzaj return, poll fulfillment

| | |
|---|---|
| Pokrycie | designed (GAP-02) |
| Prio | P0 |
| Kroki | Approve na hosted → **zamknij tab** → z booking/inspector lub `request.get` hosted fulfillment |
| Asercje | fulfillment `CONFIRMED` bez wizyty `/checkout-lab/return` |
| Oracle | API, nie UI return |

### PW-E2E-044 — Return bez `sessionId`

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Kroki | `goto /checkout-lab/return?status=success` |
| Asercje | hint widoczny; fulfillment `UNKNOWN` lub brak poll; **nie** crash |

### PW-E2E-045 — Return public bez Keycloak

| | |
|---|---|
| Pokrycie | designed (implied by 040) |
| Prio | P0 |
| Auth | newContext |
| Asercje | nie redirect `/login` |

### PW-E2E-046 — Return EXPIRED

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Mock fulfillment | `EXPIRED` |
| Asercje | `fulfillment-status` = `EXPIRED`; hint może kłamać success |

---

## E. Multi-tab (F-D2 pattern + binding)

### PW-E2E-050 — Open hosted w nowej karcie, Approve, return oracle

| | |
|---|---|
| Pokrycie | existing-pw |
| Prio | P0 |
| Kroki | `waitForEvent('page')` **przed** click `checkout-open-hosted` → Approve → goto return |
| Asercje | CONFIRMED na return |

### PW-E2E-051 — Return to merchant button (nie `hosted.goto`)

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Kroki | po Approve click „Return to merchant” |
| Asercje | URL zawiera `sessionId` i `status=success`; potem CONFIRMED |

### PW-E2E-052 — Booking tab nadal AWAITING aż worker; potem odśwież

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Uwaga | UI booking nie polluje sam — asercja na return/inspector; dokumentuj że booking snapshot jest statyczny |

---

## F. Inspector

Selectory: `inspector-session-id`, `inspector-load`, `inspector-process-status`, `inspector-signature-panel`.

### PW-E2E-060 — Load events: processStatus + Lab-Signature

| | |
|---|---|
| Pokrycie | existing-pw |
| Prio | P0 |
| Mock | events array z `signatureHeader`, `processStatus: DONE` |
| Asercje | `inspector-process-status` = DONE; tekst Lab-Signature |

### PW-E2E-061 — Deliveries renderowane

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Mock | deliveries `[{attempt:1, responseStatus:503},{attempt:2, responseStatus:202}]` |
| Asercje | UI pokazuje 503 i 202 (learning retry) |

### PW-E2E-062 — Anomalies dla sesji

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Mock | GET anomalies z `kind: session_completed_fulfillment_pending` i matching sessionId |
| Asercje | wiersz anomaly visible; obcy sessionId odfiltrowany |

### PW-E2E-063 — ErrorState gdy events problem

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |
| Mock | events 404/400 problem |
| Asercje | ErrorState / ProblemDetails; **nie** pusta lista udająca sukces |

### PW-E2E-064 — EmptyState brak eventów

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Mock | `[]` |
| Asercje | EmptyState; brak fałszywego DONE |

### PW-E2E-065 — `lastError` widoczny

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Mock | event `lastError: "hmac mismatch"` / FAILED |
| Asercje | tekst błędu w UI |

### PW-E2E-066 — Copy CANCELED vs CANCELLED

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Asercje | alert inspector wyjaśnia różnicę (jeśli jest w copy); dane nie mieszają enumów |

### PW-E2E-067 — Deliveries/anomalies failure jest miękki

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Mock | events 200, deliveries 500 |
| Asercje | events nadal widoczne; brak pełnego crash strony |

### PW-E2E-068 — Niepoprawny sessionId (nie UUID)

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Kroki | wpisz `not-a-uuid` → Load |
| Asercje | problem walidacji / 400; Zod uuid na BFF |

---

## G. Learning copy i auth hosted vs dashboard

### PW-E2E-070 — Hub: HMAC 400/503/202 w copy

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Asercje | karta HMAC zawiera 400, 503, 202 |

### PW-E2E-071 — Hub: simulate token copy

| | |
|---|---|
| Pokrycie | existing-pw (karta Hosted capability) |
| Prio | P2 |
| Asercje | tekst o Lab-Simulate-Token / public bez Keycloak |

### PW-E2E-072 — Hosted copy: no Keycloak cookie

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Asercje | tekst „No Keycloak cookie” na hosted |

---

## H. Macierz przepływów E2E (use-case overlay)

| UC | Sekwencja stron | TC |
|---|---|---|
| Happy online | Hub → Booking ONLINE → Hosted Approve → Return | 001, 010, 050, 041 |
| Cash | Booking mode CASH | 011, 012 |
| Lie | Return bez pay | 040 |
| Cancel | Hosted Decline → Return | 022, 042 |
| Pay no return | Approve, skip return | 043 |
| Expiry | Hosted EXPIRED | 024, 046 |
| Inspect | Inspector load | 060–065 |
| Identity | unauth dashboard / public hosted | 007, 020, 045 |

Szacowana liczba TC w tym pliku: **~45** (001–008, 010–019, 020–030, 040–046, 050–052, 060–068, 070–072).
