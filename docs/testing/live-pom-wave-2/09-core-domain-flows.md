# 09 — Przepływy domeny operatora (BA + test architect)

Szczegółowe **business cases**, **use cases** i **symulacje** (pozytyw / negatyw) od najprostszego hopu.  
Techniki: ISTQB FL (EP, BVA, DT, ST, use-case testing, error guessing). **As-built** — bez PSP HTTP, bez Kafki.

Normatywne HTTP: Spring `:8080` z JWT. UI: Nuxt `:3000` (cookie `nuxt-session` → BFF dokłada Bearer).  
**Druga iteracja (Caddy / TLS / vhosty / X-Forwarded-*):** [10-full-stack-edge-flows.md](10-full-stack-edge-flows.md).  
Bruno: [bruno-postman-api.md](../../setup/bruno-postman-api.md). Indeks Wave 2: [07](07-istqb-decision-state-usecase.md). CPL (inny świat): [CPL 09](../checkout-protocol-lab/09-protocol-flow-simulations.md).

Konwencja pokrycia: `existing-ra` | `existing-pom` | `designed`.

---

## Słownik i seed (`dev,seed`)

| Symbol | Wartość |
|---|---|
| `ALPHA_001` | merchant UUID `00000000-0000-0000-0000-0000000000b1`, reference `MERCHANT_ALPHA_001`, tenant `TENANT_ALPHA` (`…a2`) |
| `ALPHA_002` | `…b2` / `MERCHANT_ALPHA_002`, ten sam tenant co 001, **inny** merchant |
| `BETA_001` | `…b3` / `MERCHANT_BETA_001`, tenant `PLATFORM_TENANT` (`…a1`) |
| `SUSPENDED_DEMO` | `33333333-3333-3333-3333-333333333333`, tenant `PLACEHOLDER_TENANT_ID` **SUSPENDED** |
| ETag | dokładnie `"v{n}"` (cudzysłowy w wartości nagłówka) |
| Problem | `Content-Type: application/problem+json`, pole `error` (snake), `status` liczbowe |

**Nie** wysyłaj `X-Tenant-*`. Tenant i merchant biorą się z JWT (`tenant_id`, opcjonalnie `merchant_id`).

JWT (ROPC, public client):

```http
POST http://localhost:8081/realms/payment-quality/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=payment-quality-dashboard&username={persona}&password={persona}
```

Hasło labowe = username. Do Springa: `Authorization: Bearer {access_token}` (nie `id_token`).

---

## Persony (DT aktorów)

| Persona | JWT `tenant_id` | JWT `merchant_id` | Tworzy merchant | Tworzy order | Lifecycle order | Direct refund |
|---|---|---|---|---|---|---|
| `platform.admin` | `PLATFORM_TENANT` | — | tak (wymaga `tenantReference`) | **403** | tak (`platform:payments:lifecycle`) | **200** override |
| `tenant.admin` | `TENANT_ALPHA` | — | tak (własny tenant) | nie | nie | nie |
| `merchant.manager` | `TENANT_ALPHA` | `MERCHANT_ALPHA_001` | nie | tak (tylko `ALPHA_001`) | tak (własny) | **409** `dual_control_required` |
| `support.agent` | `PLATFORM_TENANT` | — | nie | nie | nie | nie |
| `readonly.user` | `TENANT_ALPHA` | — | nie | nie | nie | nie |
| `merchant.denied` | claim tenant, **0 ról** | — | 403 | 403 | 403 | 403 |
| gość (brak tokenu) | — | — | UI `/login`; API **401** | 401 | 401 | 401 |

UI matrix (`rbacMatrix`) może pokazać notes/risk mimo braku leaf w realmie → **403** (GAP-W2-02). Oracle testu: 201\|200 **albo** 403, nie silent skip.

---

## BC-OP-00 — Ping publiczny

**Cel biznesowy:** monitoring / health bez tożsamości.

### UC-OP-00 Happy — GET status

| | REST | UI |
|---|---|---|
| Method / path | `GET /api/status` | nie ma ekranu; canary stack |
| Headers | — | — |
| Body | — | — |
| Status | **200** `{ name, phase, status }` | — |
| Pokrycie | existing-ra | docs |

### EXC-OP-00a — Chroniony zasób bez tokenu

```http
GET /api/merchants
```

| Warstwa | Wynik |
|---|---|
| Spring | **401** `application/problem+json` |
| BFF bez cookie | **401** (SEC-030) |
| Browser `/admin/merchants` | **302/navigate** → `/login?redirectTo=/admin/merchants` (UC-W2-01, E2E-001) |

---

## BC-OP-01 — Wejście operatora (sesja)

**Cel:** oddzielić ciasteczko aplikacji od SSO Keycloak.

### UC-W2-01 / UC-OP-01a Gość → login

- **Pre:** `chromium-guest`, puste cookies.
- **Kroki UI:** `goto /admin/merchants`.
- **Oracle:** URL zawiera `/login` i `redirectTo=`; brak tabeli merchantów.
- **Negatyw myślowy:** 200 z pustą tabelą = **fail** (fail-open).
- **TC:** E2E-001; warianty E2E-002 session-lab, E2E-003 users/checkout-lab.

### UC-W2-02 / UC-OP-01b Sign out aplikacji

- **Pre:** zalogowany `platform.admin`.
- **Kroki UI:** UserMenu Sign out → ponownie `/admin/merchants`.
- **Oracle:** znowu login. Sesja Keycloak **może** żyć.
- **Nie mylić** z End OIDC (`session-lab-end-oidc`) — UC-W2-18 **designed**.

### EXC-OP-01c Cookie vs JWT w Bruno

Wklejenie `nuxt-session` do Postmana na `:8080` **nie** autoryzuje Springa. Tylko Bearer.

---

## BC-OP-02 — Rejestr merchantów (happy od zera)

**Cel:** platforma zakłada merchant w tenancie, potem aktywuje.

Stan: `DRAFT --activate→ ACTIVE --suspend→ SUSPENDED`. Inne krawędzie → **409** `invalid_transition`.

### UC-OP-02 MS — Platform admin tworzy w TENANT_ALPHA

```http
POST /api/merchants
Authorization: Bearer {platform.admin}
Content-Type: application/json
X-Correlation-ID: bc-op-02

{
  "merchantReference": "LAB-FLOW-001",
  "displayName": "Lab Flow Merchant",
  "tenantReference": "TENANT_ALPHA"
}
```

| Pole | Oczekiwanie |
|---|---|
| Status | **201** |
| Body | `merchantId` UUID, `merchantReference` znormalizowany, `status: DRAFT`, `riskFlagged` |
| Oracle persist | `GET /api/merchants/{merchantId}` → **200** ten sam reference |
| UI analog | formularz `merchantReference` + `displayName`; **Wave 2 UI nie wysyła tenanta** (GAP-W2-01) → użyj `BffClient.createMerchant` (E2E-020/022) |
| Pokrycie | existing-pom API-001; existing-ra |

`POST` bez `tenantReference` jako platform:

```http
POST /api/merchants
Authorization: Bearer {platform.admin}
Content-Type: application/json

{ "merchantReference": "NO-TENANT", "displayName": "No Tenant" }
```

→ **400** `MissingTenantReferenceException` (API-003, GAP-W2-01).

### UC-OP-02 ALT — Activate potem suspend

```http
POST /api/merchants/{id}/activate
Authorization: Bearer {platform.admin}

POST /api/merchants/{id}/suspend
Authorization: Bearer {platform.admin}
```

Oba **200**, status `ACTIVE` potem `SUSPENDED`. UI: E2E-021.  
Ponowne `activate` na `SUSPENDED` → **409** `invalid_transition`.

### EXC-OP-02a Duplikat reference

Drugi POST z tym samym `merchantReference` (po normalizacji) → **409** `duplicate_merchant_reference` (E2E-026).

### EXC-OP-02b Walidacja (EP/BVA)

| Klasa | Body | HTTP |
|---|---|---|
| Pusty displayName | `{ "merchantReference": "X", "displayName": "" }` | **400** `validation` |
| UI pusty form | Zod, **0** POST | E2E-023 |
| Zły UUID path | `GET /api/merchants/not-a-uuid` | **400** |
| Nieistniejący UUID | `GET …/00000000-0000-0000-0000-000000000000` | **404** (API-004) |

### EXC-OP-02c AuthZ

| Aktor | `POST /api/merchants` | `GET /api/merchants` |
|---|---|---|
| `merchant.manager` | **403** | **403** (brak `platform:merchants:*`) |
| `merchant.denied` | **403** | **403** |
| `support.agent` | **403** create | **200** list (read) |
| `readonly.user` | **403** | **200** (tenant-scoped list) |
| brak Bearer | **401** | **401** |

Pokrycie: existing-ra `MerchantSecurityTest`; POM: API-011 analog dla payments.

---

## BC-OP-03 — Tenant × merchant (izolacja)

**Cel:** tenant.admin widzi tylko swój tenant; GET obcego merchanta **nie** zdradza istnienia (404); mutate obcego **403**.

### UC-OP-03 / UC-W2-20 — Tenant admin happy (własny tenant)

```http
POST /api/merchants
Authorization: Bearer {tenant.admin}
Content-Type: application/json

{
  "merchantReference": "ALPHA-TA-001",
  "displayName": "Tenant Admin Merchant"
}
```

- **Bez** `tenantReference` w body: Spring bierze tenant z JWT → **201**, `tenantId` = Alpha.
- `GET /api/merchants` → lista **tylko** Alpha (w tym seed `ALPHA_001` / `ALPHA_002`).
- `GET /api/merchants/{ALPHA_001}` → **200**.
- Pokrycie: existing-ra `TenantIsolationIT`; **brak** live POM (designed UI).

### EXC-OP-03a — Tenant admin czyta Beta (maskowanie)

```http
GET /api/merchants/{BETA_001}
Authorization: Bearer {tenant.admin}
```

→ **404** problem+json, **bez** nazwy tenanta Beta w `detail`.  
Activate/suspend tego id → **403** (boundary), nie 404 — `TenantIsolationIT`.

### EXC-OP-03b — Platform vs tenant list filter

```http
GET /api/merchants?tenantId={TENANT_ALPHA uuid}
Authorization: Bearer {platform.admin}
```

Platform: filtr opcjonalny, może widzieć Beta. Tenant.admin: query `tenantId` obcy **nie** otwiera Beta.

### EXC-OP-03c — Suspended tenant JWT

Token z `PLACEHOLDER_TENANT_ID` (status SUSPENDED) na `GET /api/merchants` → **403**. Seed merchant `SUSPENDED_DEMO` nie jest backdoorem.

### EXC-OP-03d — JWT bez `tenant_id`

→ **403** `forbidden` (resolution fail), nie 401.

### UC-OP-03e / UC-W2-21 — Ten sam tenant, dwa merchanci (BOLA)

Aktor: `merchant.manager` (`merchant_id` = `MERCHANT_ALPHA_001`).

```http
POST /api/merchants/{ALPHA_002}/payment-orders
Authorization: Bearer {merchant.manager}
Idempotency-Key: 11111111-1111-1111-1111-111111111111
Content-Type: application/json

{ "amountMinor": 1000, "currency": "PLN", "clientOrderReference": "BOLA-002" }
```

→ **403** (create: scope mismatch).

```http
GET /api/merchants/{ALPHA_002}/payment-orders/{anyId}
Authorization: Bearer {merchant.manager}
```

→ **404** (read BOLA-safe; nie 403 z „to nie twój merchant”).

```http
GET /api/merchants/{BETA_001}/payment-orders
Authorization: Bearer {merchant.manager}
```

→ **403** list / **404** GET id (zależnie od matchera) — UI Support: problem, 0 rows (E2E-070).  
Platform `support.agent` / admin: GET Beta **200** (`platform:payments:read`).

Pokrycie: existing-ra security; POM E2E-070/100; CRUD 002 **designed** w Wave 2.

---

## BC-OP-04 — Utworzenie payment order (pieniądze operatora)

**Cel:** manager Alpha księguje zlecenie; admin **nie** tworzy.

Merchant musi być **ACTIVE**. DRAFT/SUSPENDED → **409** `merchant_not_payment_eligible`.

### UC-W2-08 / UC-OP-04 MS — Create 201

```http
POST /api/merchants/00000000-0000-0000-0000-0000000000b1/payment-orders
Authorization: Bearer {merchant.manager}
Content-Type: application/json
Idempotency-Key: 22222222-2222-2222-2222-222222222222
X-Correlation-ID: bc-op-04

{
  "amountMinor": 1999,
  "currency": "PLN",
  "clientOrderReference": "PO-FLOW-001"
}
```

| | |
|---|---|
| Status | **201** |
| Headers out | `Location: /api/merchants/{ALPHA_001}/payment-orders/{id}`; `ETag: "v0"`; `Idempotency-Replayed: false` |
| Body | `status: CREATED`, `amountMinor: 1999`, `currency: PLN` |
| UI | `/admin/merchants/{id}/payments` → `idempotency-key-input` + amount/currency/reference → landuje na detail UUID |
| TC | E2E-090, API-010 |

**Nie** mylić z CPL `POST /api/checkout-lab/sessions` (302 + `continueUrl`).

### ALT-OP-04 Replay 200

Ten sam `Idempotency-Key` + **identyczny** body → **200**, to samo `id`, `Idempotency-Replayed: true` (E2E-091).

### EXC-OP-04a Conflict 409

Ten sam key, `amountMinor: 2000` → **409** `idempotency_conflict`.

### EXC-OP-04b Brak Idempotency-Key

Bez nagłówka → **400** `validation` („Idempotency-Key header is required…”).

### EXC-OP-04c Admin create

Ten sam URL, token `platform.admin` → **403** (brak `merchant:payments:create`) — API-011, UC-W2-17.

### EXC-OP-04d Walidacja (EP)

| Body | HTTP `error` |
|---|---|
| `amountMinor: 0` | **400** |
| `amountMinor: 100000001` | **400** (max 1e8) |
| `currency: "PL"` | **400** |
| `currency: "XXX"` | **400** (nie ISO lab) |
| extra field `"foo": 1` | **400** unknown field |
| `Content-Type: text/plain` | **415** |

### EXC-OP-04e Denied / gość

`merchant.denied` → **403**. Brak Bearer → **401**.

---

## BC-OP-05 — Authorize + capture (ETag)

**Cel:** przejście CREATED → AUTHORIZED → CAPTURED bez zgubienia współbieżności.

### UC-W2-09 / UC-OP-05 MS

Po create zapamiętaj `ETag`.

```http
POST /api/merchants/{ALPHA_001}/payment-orders/{id}/authorize
Authorization: Bearer {merchant.manager}
Content-Type: application/json
Idempotency-Key: 33333333-3333-3333-3333-333333333333
If-Match: "v0"

{ "reason": "lab-authorize" }
```

→ **200**, `status: AUTHORIZED`, nowy `ETag` np. `"v1"`.

```http
POST /api/merchants/{ALPHA_001}/payment-orders/{id}/capture
Authorization: Bearer {merchant.manager}
Content-Type: application/json
Idempotency-Key: 44444444-4444-4444-4444-444444444444
If-Match: "v1"

{ "amountMinor": 1999, "reason": "lab-capture" }
```

→ **200**, `status: CAPTURED`, `ETag: "v2"`.  
UI: E2E-092 (drawer + If-Match z GET).

Opcjonalnie `HEAD` tego samego URL (bez `/authorize`) → **200** nagłówki, `ETag`, bez body.

### EXC-OP-05a Stale If-Match (pozytywna ochrona)

`If-Match: "v99"` na CREATED → **412** `payment_order_version_mismatch` (lub concurrency), GET nadal `CREATED` (E2E-093).

### EXC-OP-05b Brak If-Match

→ **428** `precondition_required`, header wskazuje `If-Match`.

### EXC-OP-05c Malformed ETag

`If-Match: "stale-etag"` → **400** `malformed_if_match` (nie 412). Error guessing EG-W2-09.

### EXC-OP-05d Zła krawędź ST

Capture na `CREATED` (bez authorize) → **422** `invalid_transition`.  
Capture `amountMinor` > authorized → **422** `capture_amount_exceeds_authorized`.  
Ponowny authorize na `AUTHORIZED` → **422**.

### EXC-OP-05e Platform lifecycle na cudzym merchancie

`platform.admin` **może** authorize/capture Alpha (override). `support.agent` **nie** (brak lifecycle) → **403**.

---

## BC-OP-06 — Cancel (UI confirm vs HTTP)

### UC-W2-10 MS — Cancel z CREATED

```http
POST /api/merchants/{ALPHA_001}/payment-orders/{id}/cancel
Authorization: Bearer {merchant.manager}
Content-Type: application/json
Idempotency-Key: 55555555-5555-5555-5555-555555555555
If-Match: "v0"

{ "reason": "customer-abort" }
```

→ **200** `CANCELLED`. UI: `confirm-action-confirm` (E2E-094).

### ALT-OP-06 Dismiss modal

Klik `confirm-action-dismiss` → **brak** POST `/cancel`, GET nadal CREATED (E2E-096).

### EXC-OP-06 Cancel na CAPTURED

→ **422** `invalid_transition` (terminal capture idzie w refund, nie cancel).

---

## BC-OP-07 — Refund dual-control

**Cel:** merchant nie zwraca sam; checker ≠ maker.

### UC-W2-22 / UC-OP-07 MS — Maker + checker

Pre: order **CAPTURED**, ETag świeży.

**Krok 1 — merchant próbuje direct refund (negatyw biznesowy, oczekiwany):**

```http
POST /api/merchants/{ALPHA_001}/payment-orders/{id}/refund
Authorization: Bearer {merchant.manager}
Idempotency-Key: 66666666-6666-6666-6666-666666666666
If-Match: "v2"
Content-Type: application/json

{ "amountMinor": 1999, "reason": "full-refund" }
```

→ **409** `dual_control_required`. Status order **nie** REFUNDED.

**Krok 2 — wniosek (maker):**

```http
POST /api/merchants/{ALPHA_001}/payment-orders/{id}/refund-approvals
Authorization: Bearer {merchant.manager}
Content-Type: application/json

{ "amountMinor": 1999, "reason": "full-refund" }
```

→ **201**, `Location: …/refund-approvals/{approvalId}`, body `status: PENDING`.

**Krok 3 — self-approve (negatyw):**

```http
POST /api/merchants/{ALPHA_001}/payment-orders/{id}/refund-approvals/{approvalId}/approve
Authorization: Bearer {merchant.manager}
Idempotency-Key: 77777777-7777-7777-7777-777777777777
If-Match: "v2"
```

→ **409** `dual_control_self_approve`.

**Krok 4 — checker** (inny `sub`: np. drugi user z `merchant:payments:lifecycle` **albo** `platform.admin`):

```http
POST …/refund-approvals/{approvalId}/approve
Authorization: Bearer {platform.admin}
Idempotency-Key: 88888888-8888-8888-8888-888888888888
If-Match: "v2"
```

→ **200** lifecycle, `status: REFUNDED`, nowy ETag.

**ALT override:** `platform.admin` `POST …/refund` bezpośrednio → **200** (omija kolejkę).

**UI:** `payments-refund-dual-control.spec.ts` (real-stack learning). Wave 2 07 **nie** miał UC — teraz UC-W2-22.

### EXC-OP-07 Refund > captured

→ **422** `refund_amount_exceeds_captured`.

---

## BC-OP-08 — Users, audit, settings (platform / tenant)

### UC-OP-08a List users (platform)

```http
GET /api/users?page=0&size=20
Authorization: Bearer {platform.admin}
```

→ **200**. `merchant.manager` → **403**. `tenant.admin` → **200** scoped Alpha.

### UC-OP-08b Create user (szkielet)

```http
POST /api/users
Authorization: Bearer {platform.admin}
Content-Type: application/json

{
  "username": "lab.flow.user",
  "email": "lab.flow.user@example.test",
  "temporaryPassword": "lab.flow.user",
  "tenantId": "TENANT_ALPHA",
  "merchantId": "MERCHANT_ALPHA_001",
  "roles": ["merchant:payments:read"]
}
```

→ **201**. Zbyt krótkie hasło / zły email → **400**. Manager → **403**.  
POM: E2E-110 existing (admin users page), nie ten dokładny body.

### UC-OP-08c Audit export bez wycieku JWT

```http
GET /api/audit/export.json
Authorization: Bearer {platform.admin}
```

→ **200** attachment; treść **bez** `eyJ`. UI E2E-111. Manager → **403**.

### UC-OP-08d Tenant settings ETag

```http
GET /api/tenants/current/settings
Authorization: Bearer {platform.admin}
```

→ **200** + `ETag`.

```http
PATCH /api/tenants/current/settings
Authorization: Bearer {platform.admin}
If-Match: {etag}
Content-Type: application/json

{ "timezone": "Europe/Warsaw" }
```

→ **200**. Brak If-Match → **428**. Stale → **412**.  
**Uwaga realm:** leaf `platform:tenant:settings:*` może **nie** być w composite `PLATFORM_ADMIN` → live **403** (dokumentuj drift, nie „naprawiaj” realm w teście). UI E2E-112.

---

## BC-OP-09 — Notes, risk, Error Lab (skrót)

| Przypadek | HTTP | UI | Pokrycie |
|---|---|---|---|
| Notes POST | `POST …/payment-orders/{id}/notes` `{ "body": "…" }` **201** lub **403** | E2E-040 | GAP-W2-02 |
| Risk | `PATCH /api/merchants/{id}/risk-flag` `{ "riskFlagged": true }` | E2E-050 | GAP-W2-02 |
| Error Lab 412 | BFF trigger → authorize `If-Match: "v99"` | E2E-082 | existing-pom |
| Error Lab 428 | authorize bez If-Match | E2E-083 | existing-pom |

Szczegóły triggerów: [04](04-playwright-api-http.md) C.

---

## Macierz kombinacji (minimal covering — nie kartezjusz)

| # | Tenant | Merchant path | User | Akcja | Oracle |
|---|---|---|---|---|---|
| 1 | — | any | gość | GET merchants UI | login |
| 2 | PLATFORM | ALPHA_001 | platform.admin | POST order | **403** |
| 3 | ALPHA | ALPHA_001 | merchant.manager | POST order | **201** |
| 4 | ALPHA | ALPHA_002 | merchant.manager | POST order | **403** |
| 5 | PLATFORM | BETA_001 | merchant.manager | GET order | **404**/deny UI |
| 6 | ALPHA | ALPHA_001 | tenant.admin | GET merchant | **200** |
| 7 | ALPHA | BETA_001 | tenant.admin | GET merchant | **404** |
| 8 | PLATFORM | ALPHA_001 | support.agent | GET order | **200** |
| 9 | ALPHA | ALPHA_001 | merchant.denied | GET merchants | **403** |
| 10 | ALPHA | ALPHA_001 | merchant.manager | POST refund | **409** dual_control |
| 11 | PLATFORM | ALPHA_001 | platform.admin | POST refund | **200** |

Pairwise currency×amount: [06](06-istqb-ep-bva.md). Filtry listy: RFC / E2E-095.

---

## Mapowanie na UC-W2 / TC

| BC | UC | Główne TC |
|---|---|---|
| 00 | — | status RA |
| 01 | UC-W2-01, 02, 18 | E2E-001…013 |
| 02 | UC-W2-03 | E2E-020…026, API-001…004 |
| 03 | UC-W2-11, **20**, **21** | E2E-070, 100; TenantIsolationIT |
| 04 | UC-W2-08, 17 | E2E-090/091, API-010/011 |
| 05 | UC-W2-09 | E2E-092/093 |
| 06 | UC-W2-10 | E2E-094/096 |
| 07 | **UC-W2-22** | RA PlaywrightLearningStack; dual-control spec |
| 08 | UC-W2-14, 15 | E2E-110…112 |
| 09 | UC-W2-04, 07, 12 | E2E-040, 050, 080… |

Checkout CASH/lie/decline zostaje UC-W2-05 — hops HTTP w [CPL 09](../checkout-protocol-lab/09-protocol-flow-simulations.md).
