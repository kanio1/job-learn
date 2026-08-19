# Uruchamianie stosu (Podman) i nauka POM / TypeScript

Operator runbook: codzienny HTTP, HTTPS jak produkcja, Playwright POM, REST.
Szczegóły TLS/mkcert: [tls-lab.md](tls-lab.md). Compose Phase 0: [local-infra.md](local-infra.md). Bruno/Postman (JWT ROPC na `:8080`): [bruno-postman-api.md](bruno-postman-api.md).

## Krótka odpowiedź

| Pytanie | Odpowiedź |
|---|---|
| Czy Caddy jest zepsuty? | **Nie.** Przy `--app` Caddy w ogóle nie startuje. HTTPS to osobny tryb `--full` / `--tls`. |
| Czy mogę uczyć się TS + Playwright + E2E + REST? | **Tak, teraz, na HTTP** (`scripts/dev-stack.sh --app`). |
| Czy HTTPS już działa „samo”? | **Nie w tej samej komendzie.** `--app` i `--full` się wykluczają. HTTPS = `--full` + mkcert. |
| Czy Caddy na HTTP (bez TLS) jest bliżej produkcji? | **Topologia edge tak, TLS nie.** Produkcja ma HTTPS na brzegu. Najbliższy istniejący tryb to `--full` (Caddy + trzy vhosty + te same nagłówki; cert z mkcert zamiast Let’s Encrypt). Osobnej flagi „Caddy HTTP” nie ma — patrz [Caddy HTTP?](#caddy-jako-reverse-proxy-na-http). |

Nie łącz `--app` z `--tls` ani `--full`.

---

## 1. Jednorazowe przygotowanie

Z katalogu głównego repozytorium:

```bash
cp infra/compose/.env.example infra/compose/.env
```

W `.env` zostaw `NUXT_SESSION_PASSWORD` o **co najmniej 32 znakach** (obraz Nuxt to `NODE_ENV=production`; nuxt-auth-utils nie wymyśla hasła sesji). Przykład w `.env.example` już spełnia limit.

Hasła do POM **tylko w środowisku**, nigdy w gicie:

```bash
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD='…'   # lab: platform.admin
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD='…' # lab: merchant.manager
# Worker managers (empty MERCHANT-W0..W3): PLAYWRIGHT_MERCHANT_MANAGER_W{0-3}_PASSWORD
# defaults to merchant.manager.w{n} if unset.
```

Na Fedorze/RHEL używasz Podmana; `docker compose` w skryptach to ten sam provider (`podman-compose`).

---

## 2. Wybór trybu

| Komenda | Co działa | Wejście | Kiedy |
|---|---|---|---|
| `scripts/dev-stack.sh` | Postgres + Keycloak w Podman; Spring i Nuxt **na hoście** | `http://127.0.0.1:3000` | Hot reload przy kodowaniu Java/Vue |
| `scripts/dev-stack.sh --app` | **Wszystko w kontenerach**, bez Caddy | `http://127.0.0.1:3000` | Nauka POM/TS, prawdziwy deploy HTTP |
| `scripts/dev-stack.sh --tls` | Caddy HTTPS → host Spring/Nuxt | `https://app.payment-quality.local:8443` | TLS + hot reload |
| `scripts/dev-stack.sh --full` | Caddy HTTPS → obrazy Spring/Nuxt | `https://app.payment-quality.local:8443` | Stos jak produkcja (lokalne CA) |

Zatrzymanie:

```bash
scripts/dev-stack.sh --stop    # tylko procesy hosta (Spring/Nuxt)
scripts/dev-stack.sh --down    # host + compose (wolumen Postgres zostaje)
```

---

## 3. Krok po kroku: HTTP (nauka POM / TypeScript / REST)

To jest domyślna ścieżka nauki. Nie potrzebujesz mkcert ani `/etc/hosts`.

### 3.1 Start

```bash
scripts/dev-stack.sh --app
```

Skrypt buduje obrazy (kolejne starty biorą cache), czeka na Postgres, **sprawdza issuer Keycloak**, potem Spring i Nuxt. Na rootless Podman po healthcheck robi `stop`/`start` kontenerów aplikacji, żeby porty `:3000` i `:8080` naprawdę słuchały na hoście.

Oczekiwany issuer:

```text
http://localhost:8081/realms/payment-quality
```

### 3.2 Oracle (30 sekund)

```bash
curl -sS http://127.0.0.1:8081/realms/payment-quality/.well-known/openid-configuration \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['issuer'])"
# → http://localhost:8081/realms/payment-quality

curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/api/status
# → 200

curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:3000
# → 302 (redirect na /login)
```

Jeśli issuer to `https://auth.payment-quality.local:8443/...`, został stary Keycloak z `--full`. Skrypt powinien go odtworzyć sam; w razie czego: `scripts/dev-stack.sh --down` i znowu `--app`.

### 3.3 Przeglądarka

Otwórz **`http://127.0.0.1:3000`** (nie `https://`, nie `:8443`). Zaloguj się przez Keycloak (`localhost:8081`).

`http://localhost:3000` bywa IPv6 (`::1`); hostowy Nuxt binduje tylko `127.0.0.1`. `--app` publikuje `0.0.0.0:3000`, więc oba mogą działać — POM i tak ustawiaj na `127.0.0.1`.

### 3.4 Playwright POM (E2E na żywym stosie)

Jedna komenda (oracles + POM + BFF REST, bez Caddy). **Jedyny katalog Playwright to `tests-pom`.**

```bash
scripts/run-app-stack-tests.sh            # Playwright
scripts/run-app-stack-tests.sh --visual     # screenshoty Visual Lab + ARIA
scripts/run-app-stack-tests.sh --rls-off    # drugi Nuxt :3010, RLS flag off
scripts/run-app-stack-tests.sh --backend  # plus ./mvnw test (bez restkit/, Ryuk off)
```

Ręcznie:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 \
PLAYWRIGHT_BASE_URL=http://localhost:3000 \
  corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.config.ts
```

`PLAYWRIGHT_SKIP_WEBSERVER=1` jest obowiązkowe przy `--app`: Playwright **nie** ma odpalać hostowego `pnpm dev`. `playwright.config.ts` re-eksportuje ten sam POM.

Własne specy ucz się w `apps/frontend/tests-pom-learner/` (wzorzec: `apps/frontend/tests-pom/`). Nie używaj `page.route` / `route.fulfill` w live POM.

### 3.5 REST

- Bezpośrednio Spring: `http://127.0.0.1:8080/api/...` (JWT z Keycloak).
- Przez BFF (jak UI): `http://127.0.0.1:3000/api/...` z ciasteczkiem sesji.
- Backend testy kontraktu: z `apps/backend` — `./mvnw test` (bez `restkit/` i `paymentsupport/` chyba że explicite).

TypeScript: `corepack pnpm --dir apps/frontend typecheck`.

---

## 4. Krok po kroku: HTTPS jak produkcja (Caddy)

To **nie jest** ten sam proces co `--app`. Caddy startuje tylko tutaj (`--tls` / `--full`).

### 4.1 Hosty (raz)

```text
127.0.0.1 app.payment-quality.local api.payment-quality.local auth.payment-quality.local
```

do `/etc/hosts`. Playwright TLS i tak mapuje nazwy przez `--host-resolver-rules`; `curl` w skrypcie używa `--resolve`.

### 4.2 Certyfikat lokalny (raz)

```bash
scripts/tls-lab-certs.sh
mkcert -install
```

Na Fedorze Chromium czyta NSS. Jeśli nadal `ERR_CERT_AUTHORITY_INVALID`:

```bash
sudo dnf install nss-tools    # pakiet może nazywać się libnss3-tools
mkcert -install
```

`PLAYWRIGHT_TLS_INSECURE=1` **nie** jest dowodem, że CA działa.

### 4.3 Start (obrazy + Caddy)

```bash
scripts/dev-stack.sh --full
```

Oczekiwane URL-e (rootless: host **8443** → kontener 443; port 443 wymaga uprawnień):

| Rola | URL |
|---|---|
| Aplikacja | `https://app.payment-quality.local:8443` |
| API | `https://api.payment-quality.local:8443` |
| Keycloak (przeglądarka) | `https://auth.payment-quality.local:8443` |
| Keycloak HTTP (JWKS/admin) | `http://127.0.0.1:8081` |

Issuer musi być `https://auth.payment-quality.local:8443/realms/payment-quality`.

### 4.4 POM TLS

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 \
PLAYWRIGHT_BASE_URL=https://app.payment-quality.local:8443 \
  corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.tls.config.ts
```

Hot reload przy HTTPS: `scripts/dev-stack.sh --tls` (Caddy → procesy na hoście), nie `--full`.

---

## 5. Czy jest problem z Caddy?

**Nie ma błędu Caddy.** Trzy fakty, które łatwo pomylić:

1. **`--app` nie uruchamia Caddy.** Dlatego `curl https://app.payment-quality.local:8443` dostaje „connection refused”. To nie awaria — ten tryb publikuje Nuxt na `:3000`.
2. **Let’s Encrypt nie wystawi certu dla `*.payment-quality.local`.** Publiczne ACME wymaga publicznego DNS. Caddy to dokumentuje ([hostname requirements](https://caddyserver.com/docs/automatic-https#hostname-requirements)). Lokalnie Caddy ładuje pliki mkcert; log `skipping automatic certificate management because one or more matching certificates are already loaded` znaczy: „mam cert, nie wołam CA”.
3. **W produkcji** bierzesz `infra/caddy/Caddyfile.prod.example`: **bez** `tls /certs/...`. Caddy sam bierze cert ACME, gdy A/AAAA wskazuje na serwer i otwarte są 80/443. mkcert tam nie wchodzi.

Co Caddy **robi** lokalnie (oprócz TLS): kompresja `gzip`/`zstd`, limit body 5 MB, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, ukryty `Server`, krótkie HSTS (`max-age=300` — nie rok, żeby nie przykleić `.local` do HTTPS), na `--full` **404** dla `/__oidc*` (discovery OIDC tylko z loopback kontenera).

---

## 6. Caddy jako reverse proxy na HTTP?

Tak, **Caddy to umie**. Prefiks `http://` w adresie site wyłącza Automatic HTTPS. Przykład z dokumentacji Caddy: `http://app.example.com { reverse_proxy frontend:3000 }`.

**Czy to jest „najbliżej produkcji”?** Prawie topologia, nie kontrakt TLS.

| | `--app` | Caddy na HTTP (brak flagi) | `--full` | Produkcja |
|---|---|---|---|---|
| Spring + Nuxt w kontenerach | tak | tak | tak | tak |
| Jeden reverse proxy (app/api/auth) | nie | można | **tak** | **tak** |
| Nagłówki / encode / limit body | nie (bezpośrednio Nuxt/Spring) | tak | **tak** | **tak** |
| HTTPS na brzegu | nie | nie | **tak (mkcert)** | **tak (ACME)** |
| Issuer Keycloak | `http://localhost:8081` | nowy URL vhosta | `https://auth…:8443` | publiczny HTTPS |

Dlatego **nie ma** `scripts/dev-stack.sh --caddy-http`:

- Produkcja kończy TLS na brzegu. `--full` uczy ten sam kształt (Caddy → kontenery, trzy vhosty, Secure cookie). Różnica to tylko CA (mkcert vs Let’s Encrypt).
- HTTP Caddy na `.local` i tak wymaga `/etc/hosts` i **trzeciego** issuer Keycloak (`http://auth.payment-quality.local:8082`). Rootless Podman nie zbindowałby `:80`; byłoby `:8082`. To ten sam problem sticky hostname, który psuł HTTP POM.
- Nauka POM bez DNS i certów zostaje na `--app` (`127.0.0.1:3000`).

Jeśli kiedyś dodamy tryb HTTP+Caddy, będzie to osobna flaga z własnym oracle issuer — nie mieszaj go z `--app`.

**Najbliżej całości stosu produkcyjnego, co możesz uruchomić dziś:** `scripts/dev-stack.sh --full` po `mkcert -install`.

---

## 7. Keycloak: `start` i dwa URL-e

Kontener Keycloak używa `start` (profil produkcyjny), nie `start-dev`.

- **Frontend (przeglądarka):** `KC_HOSTNAME` = `http://localhost:8081` albo `https://auth.payment-quality.local:8443`.
- **Back-channel (Spring/Nitro w sieci compose):** `hostname-backchannel-dynamic=true` — token i JWKS idą na `http://payment-quality-keycloak:8080`.

Nitro na `--app` / `--full` serwuje przepisane discovery pod `/__oidc/openid-configuration` tylko z loopback. Caddy na `--full` zwraca 404 na `/__oidc*`.

Oracle: `scripts/keycloak-issuer-oracle.sh`.

---

## 8. Podman / pasta (porty „są”, a curl nie łączy)

`podman ps` może pokazać `0.0.0.0:3000`, a `ss -tlnp` nie ma `rootlessport` na `:3000`. To `podman-compose` + pasta, nie bug Springa.

`dev-stack.sh --app` / `--full` po healthcheck robi `docker stop` + `docker start` na backend/frontend/Caddy. Jeśli host nadal nie słucha:

```bash
ss -tlnp | grep -E ':3000|:8080|:8443'
scripts/dev-stack.sh --down
scripts/dev-stack.sh --app    # albo --full
```

---

## 9. Co działa, a czego nie mylić z „zielonym CI”

| | Stan po wdrożeniu tego runbooka |
|---|---|
| `--app` (HTTP kontenery, issuer `:8081`) | Zweryfikowane: Nuxt 302, Spring 200, OIDC rewrite |
| Login w UI na HTTP | Ścieżka poprawna — pierwszy raz zaloguj się ręcznie |
| `--full` po tych zmianach Caddyfile | Składnia Caddyfile **valid**; pełny login HTTPS trzeba odpalić komendą z §4 (nie równolegle z `--app`) |
| Cały Playwright POM na zielono | Nie jest automatycznym dowodem przy starcie stosu — odpalasz suite sam |

---

## See also

- [tls-lab.md](tls-lab.md) — mkcert, NSS, oracles cert/OIDC/Location
- [local-infra.md](local-infra.md) — Postgres/Keycloak compose
- [apps/frontend/tests-pom/README.md](../../apps/frontend/tests-pom/README.md) — zasady live POM
- [infra/caddy/Caddyfile.prod.example](../../infra/caddy/Caddyfile.prod.example) — Automatic HTTPS na publicznym DNS
