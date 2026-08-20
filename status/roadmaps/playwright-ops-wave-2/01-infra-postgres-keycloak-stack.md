---
name: playwright-ops-wave-2-infra
parent: playwright-ops-wave-2
last_updated: 2026-08-20
---

# 01 — Infrastruktura: PostgreSQL 18 · Flyway · Keycloak · stos

## Werdykt

| Warstwa | Zmiana w tym milestone? | Co |
|---|---|---|
| PostgreSQL 18 compose | **Nie** obrazu | Nowe tabele + indeksy V31+ |
| Flyway | **Tak** | `support/V31`, `merchant/V32`, `payment/V33`, `ops/V34`, `iam/V35`, `tenant/V36` (numery globalne — **nie** kolidować z M360 V23–V30) |
| Keycloak realm JSON | **Tak, minimalnie** | Realm roles w istniejących composites; opcjonalnie user `support.agent.b` |
| `scripts/dev-stack.sh` | **Nie** (używać) | `--app` do POM; default do DX |
| Spring Security authorities | **Tak** | `platform:support:*`, `platform:ops:*`, `platform:notifications:read` |
| Spring dependencies | **Tak (zgoda)** | `spring-boot-starter-websocket` w E6 |
| Nuxt BFF | **Tak** | proxy cases/views/challenges/search; WS `/api/ops/feed` |
| `@nuxtjs/i18n` | **Tylko E11** | osobna zgoda; nie w Fali 1 |

## A. Stos operatora (MUST)

Źródło: [docs/setup/run-stack-and-pom.md](../../../docs/setup/run-stack-and-pom.md), [scripts/dev-stack.sh](../../../scripts/dev-stack.sh).

```bash
cp infra/compose/.env.example infra/compose/.env
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=platform.admin
export PLAYWRIGHT_PLATFORM_OPERATOR_PASSWORD=platform.operator
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
export PLAYWRIGHT_SUPPORT_AGENT_PASSWORD=support.agent
scripts/dev-stack.sh --app
```

Issuer oczekiwany: `http://localhost:8081/realms/payment-quality`.  
Nuxt: `http://127.0.0.1:3000`. Spring: `http://127.0.0.1:8080/api/status`.

| Tryb | Kiedy w tym milestone |
|---|---|
| `--app` | Fale 1–7 POM/RA na prawdziwym deploy HTTP |
| default (host Spring/Nuxt) | implementacja z hot reload |
| `--full` | opcjonalnie Fala 8+ TLS — nie blocker |

`--app` **nie** łączyć z `--tls`/`--full`. Nie mockować Keycloak. Nie mockować WS.

Live POM:

```bash
# z apps/frontend, stos już stoi
corepack pnpm exec playwright test --config playwright.pom.config.ts
# albo
corepack pnpm test:e2e:app
```

## B. Flyway — zasady laby

- Lokacje dziś: `tenant,merchant,payment,shared,audit,checkoutlab,mirrorlab,rlslab,testing`.
- Wave 2 **dodaje** `support` i `ops` do `spring.flyway.locations` w `application.yml` **i** `application-test.yml`.
- Wersja w nazwie pliku jest **globalna**. M360 rezerwuje **V23–V30**. Ten milestone: **V31+**. Jeśli M360 nie wylądował, implementer i tak **nie** zajmuje V23–V30 (zostawić lukę albo skonsolidować numer przy starcie fali — zapisać w tasku T01).
- `hibernate.ddl-auto: validate` — migracja najpierw, potem encja.
- Wzorzec indeksów: [V3__add_payment_order_list_indexes.sql](../../../apps/backend/src/main/resources/db/migration/payment/V3__add_payment_order_list_indexes.sql) — `CREATE INDEX IF NOT EXISTS`.
- **Zakaz** `CREATE INDEX CONCURRENTLY` ([PG 18 CREATE INDEX](https://www.postgresql.org/docs/18/sql-createindex.html) — nie w transakcji Flyway).

### B.1 V31 — support_cases (E3)

Plik: `apps/backend/src/main/resources/db/migration/support/V31__create_support_cases.sql`

```sql
-- V31__create_support_cases.sql
-- Support work queue. Not payment_orders.status.

CREATE TABLE support_cases (
    case_id             UUID PRIMARY KEY,
    case_reference      VARCHAR(32)  NOT NULL,
    tenant_id           UUID         NOT NULL REFERENCES tenants (tenant_id),
    merchant_id         UUID         NOT NULL REFERENCES merchants (merchant_id),
    payment_order_id    UUID         REFERENCES payment_orders (payment_order_id),
    status              VARCHAR(20)  NOT NULL,
    priority            VARCHAR(10)  NOT NULL,
    assignee_subject    VARCHAR(255),
    title               VARCHAR(200) NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_support_cases_reference UNIQUE (case_reference),
    CONSTRAINT chk_support_cases_status
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'WAITING', 'RESOLVED')),
    CONSTRAINT chk_support_cases_priority
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH'))
);

CREATE INDEX IF NOT EXISTS idx_support_cases_tenant_status_updated
    ON support_cases (tenant_id, status, updated_at DESC, case_id ASC);

CREATE INDEX IF NOT EXISTS idx_support_cases_assignee
    ON support_cases (assignee_subject, status)
    WHERE assignee_subject IS NOT NULL;
```

`case_reference` format `INC-{n}` unikalny globalnie (jak merchant reference). POM: `uniqueCaseReference(testInfo)`. Partial index na assignee jest OK w PG 18 (immutable predicate).

### B.2 V32 — merchant contact + HTTP ETag already has version (E1)

Kolumna `merchants.version` **już istnieje** (V1). Brak HTTP ETag na merchant detail (tylko payment / tenant settings).

```sql
-- V32__merchant_contact_fields.sql
ALTER TABLE merchants
    ADD COLUMN contact_phone   VARCHAR(32),
    ADD COLUMN contact_address VARCHAR(200);
```

Indeks niepotrzebny (to nie list filter Fali 1 M360). Bound: phone E.164-ish max 32; address max 200. PATCH tylko tych pól + `display_name`.

Jeśli M360 T13 już dodał ETag na GET/activate — **nie** duplikować handlerów; tylko dodać kolumny i conflict DTO.

### B.3 V33 — refund challenges (E5)

```sql
-- V33__create_payment_refund_challenges.sql
CREATE TABLE payment_refund_challenges (
    challenge_id      UUID PRIMARY KEY,
    approval_id       UUID         NOT NULL REFERENCES payment_refund_approvals (approval_id),
    payment_order_id  UUID         NOT NULL REFERENCES payment_orders (payment_order_id),
    pin_hash          VARCHAR(128) NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    attempt_count     INTEGER      NOT NULL DEFAULT 0,
    locked_until      TIMESTAMPTZ,
    verified_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_refund_challenge_attempts CHECK (attempt_count >= 0 AND attempt_count <= 20)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_refund_challenge_open
    ON payment_refund_challenges (approval_id)
    WHERE verified_at IS NULL;
```

PIN **hashed** (Argon2/BCrypt już na classpath jeśli jest; inaczej `SCryptPasswordEncoder` Spring Security). Nigdy plaintext. Partial unique: jeden otwarty challenge na approval.

Próg: `amount_minor > 100000` (EUR 1_000.00 przy currency EUR; dla innych walut ten sam próg minor — udokumentować w AC jako lab simplification, nie FX).

### B.4 V34 — ops notifications + feed outbox (E6, E7)

Bez Kafki. Tabela notyfikacji + opcjonalna `ops_feed_event` do replay/inject (nie Spring Modulith `event_publication` — to już jest V6 shared).

```sql
-- V34__create_ops_notifications.sql
CREATE TABLE ops_notifications (
    notification_id   UUID PRIMARY KEY,
    recipient_subject VARCHAR(255) NOT NULL,
    event_id          UUID         NOT NULL,
    event_type        VARCHAR(64)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              VARCHAR(500) NOT NULL,
    payload           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    read_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ops_notifications_recipient_event
        UNIQUE (recipient_subject, event_id)
);

CREATE INDEX IF NOT EXISTS idx_ops_notifications_recipient_unread
    ON ops_notifications (recipient_subject, created_at DESC)
    WHERE read_at IS NULL;
```

UNIQUE `(recipient_subject, event_id)` = dedup inbox. Feed WS może iść in-memory; inject zapisuje wiersz z zadanym `event_id` / `occurred_at` (kolumna w payload JSON).

### B.5 V35 — saved views (E8)

```sql
-- V35__create_user_saved_views.sql
CREATE TABLE user_saved_views (
    view_id        UUID PRIMARY KEY,
    owner_subject  VARCHAR(255) NOT NULL,
    resource       VARCHAR(32)  NOT NULL,
    name           VARCHAR(80)  NOT NULL,
    filters        JSONB        NOT NULL,
    columns        JSONB        NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_saved_views_owner_name UNIQUE (owner_subject, resource, name),
    CONSTRAINT chk_user_saved_views_resource CHECK (resource IN ('PAYMENT_ORDERS'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_saved_views_default
    ON user_saved_views (owner_subject, resource)
    WHERE is_default;
```

Fala 6 może zacząć od **localStorage** (T13a) zanim V35 (T13b). JWT w localStorage nadal zakazany — tylko JSON filtrów.

### B.6 V36 — tenant payment policy (E10)

`tenants` już ma `settings_version` (V0.3). Dodać JSONB, nie nową tabelę:

```sql
-- V36__tenant_payment_policy.sql
ALTER TABLE tenants
    ADD COLUMN payment_policy JSONB NOT NULL DEFAULT '{
        "autoCapture": false,
        "maxAutoCaptureMinor": 0,
        "riskThreshold": 50,
        "refundPolicy": "MANUAL"
    }'::jsonb;
```

CHECK w aplikacji + Zod. Opcjonalnie PG CHECK `riskThreshold BETWEEN 0 AND 100` przez `(payment_policy->>'riskThreshold')::int` — expression musi być immutable; jeśli wątpliwe, walidacja tylko w Spring.

## C. Modulith

Nowe pakiety (checklist [modules.md](../../../.agents/skills/spring-modulith/modules.md)):

| Module | Public seam | Internal |
|---|---|---|
| `lab.paymentquality.support` | ids, status enum, `SupportCaseSeedCapability` | cases, bulk-assign, web |
| `lab.paymentquality.ops` | `OpsFeedPublisher` (jeśli inny moduł emituje) | WS, inject, notifications |

- `support` może zależeć od public `merchant` / `payment` root **tylko** jeśli potrzebuje eligibility — preferuj przechowywać UUID + FK, bez importu `payment.internal`.
- `ops` słucha `AuditableActionOccurred` jak `audit`. Nie OPEN.
- `*ModuleTest` + `ModulithArchitectureTest` po dodaniu pakietów.
- Nie flag-gate jak `checkoutlab` — to product-like ops, zawsze włączone w `dev`/`test`.

## D. Keycloak / Security

Brak nowych **composite** roles. Dodać realm roles i włożyć w istniejące composites:

| Realm role | PLATFORM_ADMIN | TENANT_ADMIN | SUPPORT_AGENT | READ_ONLY | MERCHANT_MANAGER |
|---|---|---|---|---|---|
| `platform:support:read` | tak | tak (tenant scope) | tak | tak | nie |
| `platform:support:operate` | tak | nie | tak | nie | nie |
| `platform:ops:feed` | tak | tak | tak | tak | tak (własny merchant — filtrować payload) |
| `platform:ops:inject` | tak | nie | nie | nie | nie |
| `platform:notifications:read` | tak | tak | tak | tak | tak |
| `platform:notifications:write` | implied by events | — | — | — | — |

Spring `Authorities` stałe analogicznie do [Authorities.java](../../../apps/backend/src/main/java/lab/paymentquality/shared/security/Authorities.java). Mapper Keycloak `realm` → `platform:*` jak dziś.

User `platform.operator` **już jest** w realm (composite PLATFORM_ADMIN). POM: dodać `setup-platform-operator` / env `PLAYWRIGHT_PLATFORM_OPERATOR_PASSWORD` (default lab `platform.operator`).

Opcjonalnie `support.agent.b` (T08) — ten sam composite, inny `sub`.

WebSocket: handshake przez BFF cookie session; Spring widzi Bearer z Nitro. 401 na WS = disconnect + chip „disconnected”.

## E. HTTP kody (nie zgadywać)

| Kod | Znaczenie w tym labie |
|---|---|
| 409 | duplikat reference, idempotency, dual-control self-approve, **nielegalna** transycja case (RESOLVED→NEW) |
| 412 | stale `If-Match` merchant / case / tenant policy |
| 428 | brak `If-Match` na mutacji |
| 429 | PIN lockout (5 fail) — Problem Details `error=rate_limited` |
| 400 | walidacja Zod/Bean Validation |
| 403 | brak authority |
| 404 | case/merchant poza tenantem — **ta sama polityka co płatności** (mask 404 vs 403 — skopiować payment BOLA) |

## F. Testcontainers / Failsafe

Nowe tabele: `SupportModuleTest`, `OpsModuleTest`, REST Assured w `lab.paymentquality.rest`. Seed `dev,seed` musi wstawać po V31–V36 (puste tabele OK). Nie JDBC z Node.
