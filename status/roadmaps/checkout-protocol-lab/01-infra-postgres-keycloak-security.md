---
name: cpl-infra-postgres-keycloak-security
parent: checkout-protocol-lab
last_updated: 2026-08-09
---

# 01 — Infrastruktura: PostgreSQL · Keycloak · Spring Security

## Werdykt analityczny

| Warstwa | Adaptacja MVP? | Co |
|---|---|---|
| PostgreSQL 18 | **Tak** — schemat + Flyway locations | Nowe tabele `checkout_*` |
| Keycloak realm | **Nie** | Stub OAuth = in-app, nie OIDC |
| Spring Security / CORS | **Tak** | Lab token + HMAC notify + Lab-* headers |
| Compose / image Postgres | **Nie** | `postgres:18` wystarczy |
| Compose Keycloak | **Nie** | Bez nowego klienta |

---

## A. PostgreSQL

### A.1 Co już mamy (nie ruszać pod CPL)

```text
db/migration/
  tenant/     V0.1..V0.3   ← webhook_base_url to TYLKO settings
  merchant/   V1..V1.2
  payment/    V2..V5, V9, V10   ← payment_orders, idempotency_records
  shared/     V6            ← event_publication (Modulith) — NIE IPN
  audit/      V7, V8, V11   ← JSONB pattern OK do kopiowania stylu
```

`spring.flyway.locations` dziś (`application.yml` + `application-test.yml`):

```yaml
spring:
  flyway:
    locations: classpath:db/migration/tenant,classpath:db/migration/merchant,classpath:db/migration/payment,classpath:db/migration/shared,classpath:db/migration/audit
```

### A.2 Wymagana zmiana konfiguracji

Dodać folder i wpis:

```yaml
# application.yml + application-test.yml
spring:
  flyway:
    locations: >-
      classpath:db/migration/tenant,
      classpath:db/migration/merchant,
      classpath:db/migration/payment,
      classpath:db/migration/shared,
      classpath:db/migration/audit,
      classpath:db/migration/checkoutlab
```

Bez tego: tabele nie powstaną, `ddl-auto: validate` padnie.

### A.3 Docelowy schemat (V12)

Plik: `apps/backend/src/main/resources/db/migration/checkoutlab/V12__create_checkout_lab.sql`

Szkic (do doprecyzowania w E0-S2):

```sql
-- Learning SQL: UUID PK, TIMESTAMPTZ, CHECK, UNIQUE, indeks pollera

CREATE TABLE checkout_session (
    session_id           UUID PRIMARY KEY,
    ext_order_id         VARCHAR(120) NOT NULL,
    amount_minor         BIGINT NOT NULL,
    currency             VARCHAR(3) NOT NULL,
    status               VARCHAR(32) NOT NULL,
    continue_url         TEXT NOT NULL,
    notify_url           TEXT NOT NULL,
    redirect_uri         TEXT NOT NULL,
    validity_until       TIMESTAMPTZ,
    idempotency_key_hash VARCHAR(64),
    correlation_id       VARCHAR(128) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_checkout_session_amount
        CHECK (amount_minor BETWEEN 1 AND 100000000),
    CONSTRAINT chk_checkout_session_currency
        CHECK (currency IN ('PLN', 'EUR', 'USD')),
    CONSTRAINT chk_checkout_session_status
        CHECK (status IN (
            'CREATED', 'PENDING', 'COMPLETED', 'CANCELED', 'EXPIRED'
        )),
    CONSTRAINT chk_checkout_session_idem_hash
        CHECK (idempotency_key_hash IS NULL
            OR idempotency_key_hash ~ '^[0-9a-f]{64}$')
);

CREATE UNIQUE INDEX uk_checkout_session_idem
    ON checkout_session (idempotency_key_hash)
    WHERE idempotency_key_hash IS NOT NULL;

CREATE TABLE checkout_event (
    id               UUID PRIMARY KEY,
    event_id         VARCHAR(64) NOT NULL,
    session_id       UUID NOT NULL
        REFERENCES checkout_session (session_id),
    event_type       VARCHAR(64) NOT NULL,
    payload          JSONB NOT NULL,
    signature_header VARCHAR(512),
    received_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    process_status   VARCHAR(32) NOT NULL,
    attempts         INT NOT NULL DEFAULT 0,
    last_error       TEXT,
    CONSTRAINT uk_checkout_event_event_id UNIQUE (event_id),
    CONSTRAINT chk_checkout_event_process_status
        CHECK (process_status IN (
            'RECEIVED', 'PROCESSING', 'DONE', 'FAILED', 'DUPLICATE'
        ))
);

-- Learning SQL: indeks pod worker (status + czas), nie full scan
CREATE INDEX idx_checkout_event_poll
    ON checkout_event (process_status, received_at);

CREATE TABLE checkout_fulfillment (
    fulfillment_id   UUID PRIMARY KEY,
    session_id       UUID NOT NULL
        REFERENCES checkout_session (session_id),
    status           VARCHAR(32) NOT NULL,
    source_event_id  VARCHAR(64),
    confirmed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_checkout_fulfillment_session UNIQUE (session_id),
    CONSTRAINT chk_checkout_fulfillment_status
        CHECK (status IN (
            'AWAITING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'
        ))
);
```

**Świadomie bez FK do `payment_orders`.**

### A.4 Czego się uczymy (SQL)

- Migracje **per-moduł** + lista `locations` (nie „magiczny” jeden folder).
- `UNIQUE (event_id)` = dedup at-least-once na poziomie DB.
- Partial unique index na idempotency (jak payment V5, inny scope).
- `JSONB` na raw envelope (porównaj `audit_event.before_state`).
- Poller: `FOR UPDATE SKIP LOCKED` (Postgres 18) — bez Kafki.
- Osobny inbox ≠ `event_publication` (Modulith domain events).

### A.5 Reset testowy

Połączenie z `testing` module: gdy lab on, `/api/test/reset` (lub osobny endpoint) musi czyścić `checkout_*`, żeby nie mieszać deterministic seed merchant/payment.

---

## B. Keycloak

### B.1 Stan

| Client | Typ | Użycie dziś |
|---|---|---|
| `payment-quality-dashboard` | public + PKCE + direct grants | UI JWT; `azp` w Resource Server |
| `payment-quality-admin` | confidential + service account | IAM admin API |

Realm: `infra/keycloak/realms/payment-quality-realm.json`  
Kopia testowa: `apps/api-tests/src/test/resources/keycloak/payment-quality-realm.json`

### B.2 Decyzja MVP

**Żadnych zmian realm.**  
CPL OAuth stub = **in-app** (`client_id`/`client_secret` z config), nie token Keycloak.

Dlaczego:

1. PayU-like OAuth ≠ OIDC Keycloak.
2. Unikamy konfliktu z `AuthorizedPartyValidator` (`EXPECTED_AZP=payment-quality-dashboard`).
3. Nie uczymy błędnego modelu „token KC = token PSP”.

### B.3 Czego się uczymy (Keycloak)

- **Kontrast:** JWT Keycloak chroni dashboard/API merchant; notify PSP **nie** używa JWT — używa HMAC na raw body.
- Hosted checkout page jest publiczna (jak F-D2) — bez logowania KC.
- Booking/Inspector (E5/E6) mogą używać **istniejących** userów dashboard (`platform.admin`) — bez nowych ról.
- P2 (opcjonalnie, poza MVP): osobny confidential client tylko do lekcji „merchant API via KC client_credentials” — **obok** stubu PSP.

### B.4 Secrets (nie w Keycloak)

W `.env.example` / `application-dev.yml`:

```yaml
app:
  checkout-lab:
    enabled: false
    oauth-client-id: checkout-lab-merchant
    oauth-client-secret: ${CHECKOUT_LAB_OAUTH_SECRET:change-me}
    hmac-secret: ${CHECKOUT_LAB_HMAC_SECRET:change-me-too}
    signature-tolerance-seconds: 300
```

---

## C. Spring Security (główna adaptacja auth)

### C.1 Problem

Domyślny chain (`SecurityConfig` Order 3): `.anyRequest().authenticated()` + JWT.  
Bez zmian:

- stub OAuth → 401
- notify IPN → 401 (PSP nigdy nie ma Keycloak JWT)

### C.2 Wzorzec (już w kodzie)

```java
@Bean
@Order(2)
public SecurityFilterChain testEndpointPassThroughFilterChain(HttpSecurity http) {
    http.securityMatcher(/* /api/test/reset, /api/test/seed */)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
}
```

### C.3 Docelowe matchery CPL (gdy flaga on)

| Path | Auth | Learning HTTP |
|---|---|---|
| `POST /api/checkout-lab/oauth/token` | permitAll (walidacja secret w handlerze) | form-urlencoded, 401 złe credentials |
| `POST /api/checkout-lab/notify` | **bez JWT**; Filter czyta **raw body** + HMAC | 400 vs 503 |
| `POST /api/checkout-lab/sessions` | lab Bearer (nie KC JWT) | 302 + Location |
| `GET /api/checkout-lab/sessions/{id}` | lab Bearer lub JWT dashboard (decyzja w E1) | retrieve oracle |
| Inspector APIs | JWT dashboard (KC) OK | RBAC jak Error Lab |

Szkic Order:

```java
@Bean
@Order(2) // obok lub zamiast rozszerzenia test chain — osobny bean Order(2)/Order(2.5)
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public SecurityFilterChain checkoutLabPublicChain(HttpSecurity http) {
    http.securityMatcher(new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/checkout-lab/oauth/token"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/checkout-lab/notify")
        ))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
}
```

Notify: dodatkowy `OncePerRequestFilter` / `ContentCachingRequestWrapper` — **HMAC na bajtach przed** `ObjectMapper.readValue`.

### C.4 CORS (`dev`/`test`)

Dodać allowed/exposed:

```text
Allowed: Lab-Signature, Lab-Event-Id, Lab-Force-Scenario, Idempotency-Key
Exposed: Location, Lab-Event-Id (jeśli zwracane)
```

Połączenie: `SecurityConfig.corsConfigurationSource()` — dziś ma `Idempotency-Key`, `X-Correlation-ID`, `Location`.

---

## D. Macierz ryzyka

| Błąd | Skutek |
|---|---|
| JWT na notify | Fałszywe 401; zła lekcja IPN |
| Reuse `idempotency_records` | Zanieczyszczenie domeny payment |
| Reuse `event_publication` | Mylenie Modulith z webhook inbox |
| Nowy client KC pod PayU OAuth | Konflikt azp + zła lekcja |
| Flyway bez `locations` | validate fail |
| Traktowanie `webhook_base_url` jako IPN | Fałszywe „done” protokołu |

## E. Stories infrastrukturalne (cross-cutting)

| ID | Opis | Epic |
|---|---|---|
| INFRA-PG-01 | Folder + V12 + locations | E0 |
| INFRA-PG-02 | Indeksy pollera + UNIQUE event_id | E0/E2 |
| INFRA-PG-03 | Reset lab w test harness | E0/E7 |
| INFRA-SEC-01 | Security chains lab | E1/E2 |
| INFRA-SEC-02 | CORS Lab-* | E1/E2 |
| INFRA-CFG-01 | Secrets + flaga | E0 |
| INFRA-KC-00 | Brak zmian realm MVP | — |
| INFRA-KC-P2 | Opcjonalny client OIDC (poza MVP) | — |
