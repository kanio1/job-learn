# Bruno / Postman — API i przypadki biznesowe

Poradnik ręcznego testowania REST na żywym stosie. **Nie wymaga** instalacji Bruno/Postmana w repozytorium ani commitowania kolekcji.

Kanon kontraktu zostaje REST Assured (`apps/api-tests`, `apps/backend` RA). Bruno/Postman = eksploracja i te same oracles co use case’y w katalogach.

Powiązane: [run-stack-and-pom.md](run-stack-and-pom.md), [keycloak-local-auth.md](keycloak-local-auth.md), [session-bff-oidc-contract.md](../testing/session-bff-oidc-contract.md), [UC live POM](../testing/live-pom-wave-2/07-istqb-decision-state-usecase.md).

---

## Krótka odpowiedź

Na tym labie **Bruno/Postman biją Spring na `:8080` z JWT Keycloak** (`Authorization: Bearer …`). Dashboard (`:3000`) używa **ciasteczka `nuxt-session`**, nie ręcznego JWT w UI.

| Pytanie | Odpowiedź |
|---|---|
| Podajemy JWT? | **Tak** — `access_token` z Keycloak (ROPC), nie `id_token`. |
| Cookie BFF? | Nie do kontraktu API. Sealed `nuxt-session` nie służy do edycji ról. |
| Client secret? | **Nie** — publiczny klient `payment-quality-dashboard`. |
| CSRF? | Nie na Spring `/api/merchants/**`. Tylko lab `POST /api/session-lab/csrf-demo` na BFF. |

---

## Dwa światy (nie mieszaj)

| Cel | Host | Auth | Narzędzie |
|---|---|---|---|
| Kontrakt REST, RBAC, lifecycle | `http://127.0.0.1:8080` | **JWT** z Keycloak | Bruno / Postman / curl |
| Sesja operatora, cookie, CSRF lab | `http://localhost:3000` | **HttpOnly `nuxt-session`** (BFF dokłada Bearer do Spring) | przeglądarka / Playwright |
| Publiczne | `GET /api/status` | **nic** | cokolwiek |

Spring jest **stateless JWT resource server**. Nie logujesz się do Postmana loginem Nuxt. Bierzesz **access token** z Keycloak.

Checkout Lab (`/api/checkout-lab/**`) to **osobny** stub OAuth/HMAC, nie JWT dashboardu. Nie ucz „KC JWT = PayU token”.

Mapa hops / `continueUrl` / luki designed: [checkout-protocol-lab README](../testing/checkout-protocol-lab/README.md) (UC-01, UC-03, UC-05). Skrypty z headers/body: [CPL 09](../testing/checkout-protocol-lab/09-protocol-flow-simulations.md). Idempotencja **orderów** dashboardu: [live-pom 09](../testing/live-pom-wave-2/09-core-domain-flows.md) (BC-OP-04…07), nie replay sesji CPL.

**HTTPS / Caddy:** token i API muszą być z **tego samego trybu**. `--full` → token `POST https://auth.payment-quality.local:8443/realms/payment-quality/protocol/openid-connect/token`, API `https://api.payment-quality.local:8443`. Token z `:8081` na `api.` HTTPS → **401**. Location create jest względny mimo `X-Forwarded-*`. Szczegóły: [10](../testing/live-pom-wave-2/10-full-stack-edge-flows.md).

Stos: [run-stack-and-pom.md](run-stack-and-pom.md) (`scripts/dev-stack.sh` albo `--app`). Issuer Keycloak:

```text
http://localhost:8081/realms/payment-quality
```

`iss` w JWT musi być tym samym URI, którego używa Spring. Token z innego hosta (`127.0.0.1` vs `localhost`) pada na walidacji.

---

## Skąd JWT (to samo co `api-tests`)

Resource Owner Password Grant (ROPC) na publicznym kliencie dashboardu. W realmie: `directAccessGrantsEnabled: true`, **bez** `client_secret`. Implementacja: `KeycloakTokenFactory` (`grant_type=password`, `client_id=payment-quality-dashboard`).

**Token URL**

```http
POST http://localhost:8081/realms/payment-quality/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
```

**Body**

```text
grant_type=password
&client_id=payment-quality-dashboard
&username=platform.admin
&password=platform.admin
```

Odpowiedź: `access_token` (JWT), `expires_in` (zwykle ~300 s). Do Springa idzie **tylko access token**.

Hasła labowe = username (import `infra/keycloak/realms/payment-quality-realm.json`):

| Persona | Username / hasło | Do czego |
|---|---|---|
| Platform admin | `platform.admin` | merchanci, IAM, ops, dual-control override na refund |
| Merchant manager | `merchant.manager` | płatności Alpha (`merchant_id` w JWT) |
| Tenant admin | `tenant.admin` | merchanci w TENANT_ALPHA |
| Support | `support.agent` | odczyt cross-tenant (granice IDOR) |
| Denied | `merchant.denied` | ważny JWT, **403** na chronionych ścieżkach |
| Platform operator | `platform.operator` | historyczny merchant-only pack |

JWT niesie `realm_access.roles` (liście, np. `merchant:payments:create`) oraz claimy `tenant_id` / `merchant_id`. Spring mapuje role na `platform:*` / `merchant:*`. **Nie wklejasz ról ręcznie** — zmieniasz użytkownika i bierzesz nowy token.

Seed merchant Alpha (profil `dev,seed`):

```text
merchantId = 00000000-0000-0000-0000-0000000000b1
```

(`Seeds.MERCHANT_ALPHA_001_ID`)

---

## How-to: Postman

1. Collection → **Authorization** → Type **OAuth 2.0**.
2. Grant type: **Password Credentials** (Resource Owner).
3. Access Token URL: `http://localhost:8081/realms/payment-quality/protocol/openid-connect/token`
4. Client ID: `payment-quality-dashboard`. Client Secret: **puste**.
5. Username / Password: np. `merchant.manager` / `merchant.manager`.
6. Scope: `openid` (opcjonalnie).
7. **Get New Access Token** → Use Token.
8. Requesty: Inherit auth from parent.

Environment:

```text
baseUrl     = http://127.0.0.1:8080
merchantId  = 00000000-0000-0000-0000-0000000000b1
```

Przykład: `{{baseUrl}}/api/merchants/{{merchantId}}/payment-orders`

**OAuth 2.0 Authorization Code + PKCE** odtwarza login przeglądarki (`redirect_uri` na allow-liście `http://localhost:3000/*`). Do API **niepotrzebne** — ROPC jest tym, czego używa REST Assured.

Osobne środowiska / foldery per persona. Nie jeden token na admina i managera.

---

## How-to: Bruno

1. Collection Auth → **OAuth 2.0** → grant **password**, ten sam token URL i `client_id`.
2. Albo folder **Auth**: request `Get token`, body `form-urlencoded` jak wyżej, potem **Post Response**:

```js
bru.setVar("access_token", res.body.access_token);
```

3. Kolejne requesty: Auth **Bearer** `{{access_token}}`.
4. Environments per persona (`platform.admin` vs `merchant.manager`).

Bruno CLI (`bru run`) może powtarzać te requesty; w tym repo kanonem CI/Failsafe zostaje REST Assured.

---

## Smoke (curl)

```bash
TOKEN=$(curl -sS -X POST \
  'http://localhost:8081/realms/payment-quality/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=payment-quality-dashboard&username=platform.admin&password=platform.admin' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

curl -sS -D - http://127.0.0.1:8080/api/status
curl -sS -D - -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1:8080/api/merchants
```

| Warunek | Oczekiwanie |
|---|---|
| `GET /api/status` bez tokenu | 200 |
| `GET /api/merchants` bez `Authorization` | **401** `application/problem+json` |
| Token `merchant.denied` | **403** |
| Token `platform.admin` | 200 lista (przy `dev,seed`) |

OpenAPI (Wave 5, z JWT): `GET http://127.0.0.1:8080/v3/api-docs`.

---

## Jak układać przypadki biznesowe

Jeden folder = jeden use case (aktor + precondition + kroki + oracle). Nie „wszystkie GET-y”.

Mapa UC dashboardu: [07 live POM](../testing/live-pom-wave-2/07-istqb-decision-state-usecase.md). CPL (302, HMAC, fulfillment): [07 CPL](../testing/checkout-protocol-lab/07-istqb-decision-state-usecase.md). Bruno ma **powielać te same oracles** (status + `error` w problem+json + ETag), nie wymyślać trzeciego kontraktu.

Folder CPL (lab Bearer / HMAC, **nie** JWT `platform.admin`): `POST /api/checkout-lab/oauth/token` → `POST /api/checkout-lab/sessions` (302) → hosted simulate → `POST /api/checkout-lab/notify`. Oracle = fulfillment, nie query `status=success`. Luki, których Bruno nie zastąpi UI: PAY_NO_RETURN (PW-E2E-043), lie header (PW-API-071).

### Przykład — utworzenie i capture (merchant.manager, Alpha)

1. `POST /api/merchants/{{merchantId}}/payment-orders`  
   Headers: `Idempotency-Key: <uuid>`, `Content-Type: application/json`, opcjonalnie `X-Correlation-ID`.  
   Body: `amountMinor`, `currency`, `clientOrderReference` (unikalne).  
   Oracle: **201**, `Location`, `ETag`.
2. Replay tego samego key + body → **200**, to samo id.
3. Inny body, ten sam key → **409** `idempotency_conflict`.
4. `POST .../authorize` z `If-Match` ze świeżego GET.
5. `POST .../capture` ze świeżym ETag.
6. Stale `If-Match: "v99"` → **412**, status zostaje `CREATED` / poprzedni.

### Nagłówki (w folderze, nie w URL)

| Header | Kiedy |
|---|---|
| `Authorization: Bearer` | zawsze oprócz `/api/status` (i publicznych labów checkout) |
| `Idempotency-Key` | create order; refund / część capture |
| `If-Match` | authorize / capture / cancel / PATCH / refund approve |
| `X-Correlation-ID` | śledzenie; backend i tak doda |

### RBAC jako osobne requesty

Ten sam URL, trzy tokeny: `platform.admin` / `merchant.manager` / `merchant.denied`.

- Manager na **cudzym** merchancie (Beta) → 403 lub 404 (BOLA), nie 200 z obcymi danymi.
- Merchant `POST .../refund` przy dual-control → **409** `dual_control_required`; platform lifecycle nadal może refundować.
- Platform admin **bez** `merchant:payments:create` → POST order **403** (jak `admin-bff.spec.ts`).

### Merchant lifecycle

```text
DRAFT → POST .../activate → ACTIVE → POST .../suspend → SUSPENDED
```

Inne przejścia → 409 / 422 wg kontraktu. Create merchant jako platform admin wymaga `tenantReference` (inaczej 400).

---

## Czego nie robić

- Nie wklejaj JWT z `localStorage` — go tam nie ma.
- Nie dekoduj `nuxt-session`, żeby „wyciągnąć token” — do API idź po ROPC.
- Nie używaj klienta `payment-quality-admin` (secret, service account) do dashboard API.
- Nie commituj kolekcji z żywymi tokenami ani `.env` z hasłami poza labowym realm JSON.
- Token wygasa ~5 min — Get New Access Token / skrypt Bruno, nie jeden copy na cały dzień.
- ROPC jest **włączone pod testy** (RFC 9700 odradza to w produkcji). W Postmanie to świadomy skrót labowy, nie wzorzec bankowy.

---

## Skrót

Stos włączony → ROPC → `access_token` → `Authorization: Bearer` na `:8080` → foldery = use case’y z aktorami, nie lista endpointów.
