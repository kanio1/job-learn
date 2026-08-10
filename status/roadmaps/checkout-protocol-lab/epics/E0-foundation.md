---
name: epic-e0-foundation
parent: checkout-protocol-lab
epic: E0
tasks: [CPL-T01, CPL-T02, CPL-T03]
last_updated: 2026-08-09
---

# Epic E0 — Foundation & Guardrails

**Cel produktowy:** izolowany, flagowany moduł CPL + schemat DB, bez wpływu na payment.  
**Cel dydaktyczny:** Modulith boundaries, feature flags, Flyway multi-location.

**Połączenia:** wzorzec `lab.paymentquality.testing` + `app.testing.enabled`; SecurityConfig Order chains; `ModulithArchitectureTest`.

---

## Story E0-S1 — Modulith module skeleton  
**Task:** `CPL-T01` · P0

### Jako / chcę / aby
Jako architekt chcę moduł `checkoutlab` z publicznym API i `internal.*`, aby izolować lab od `payment.internal`.

### Acceptance criteria
- [ ] Pakiet `lab.paymentquality.checkoutlab` (+ `package-info` jeśli konwencja).
- [ ] Implementacja wyłącznie pod `checkoutlab.internal.*`.
- [ ] `@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")` na web beans.
- [ ] `enabled=false` (default) → brak mapowań kontrolerów / 404.
- [ ] `ModulithArchitectureTest` zielony.

### Szkic struktury

```text
lab.paymentquality.checkoutlab/
  package-info.java          # opcjonalnie: @ApplicationModule
  internal/
    config/CheckoutLabProperties.java
    config/CheckoutLabConfiguration.java
    web/                     # puste / health stub
```

```java
@ConfigurationProperties(prefix = "app.checkout-lab")
public record CheckoutLabProperties(
    boolean enabled,
    String oauthClientId,
    String oauthClientSecret,
    String hmacSecret,
    long signatureToleranceSeconds
) {}
```

### Learning
- `MOD:` public vs `internal` — jak merchant/payment.
- `KC:` flaga **nie** wymaga realm; lab wyłączony = zero powierzchni.
- `HTTP:` 404 gdy off — ten sam wzorzec co testing endpoints.

### Połączone z
- `TestController` / `TestEndpointsDisabledIT`
- `AGENTS.md` Modulith rules

---

## Story E0-S2 — Flyway schema v1  
**Task:** `CPL-T02` · P0 · INFRA-PG-01/02

### Jako / chcę / aby
Jako backend dev chcę tabele `checkout_session`, `checkout_event`, `checkout_fulfillment`, aby persystować protokół.

### Acceptance criteria
- [ ] Plik `db/migration/checkoutlab/V12__create_checkout_lab.sql` (patrz [01-infra…](../01-infra-postgres-keycloak-security.md)).
- [ ] `flyway.locations` zaktualizowane w `application.yml` **i** `application-test.yml`.
- [ ] JPA entities `validate` zgodne z migracją.
- [ ] Testcontainers IT smoke: tabele istnieją po starcie kontekstu.
- [ ] Brak FK do `payment_orders` / braku alter istniejących tabel.

### Learning
- `SQL:` CHECK status/currency; UNIQUE `event_id`; indeks `(process_status, received_at)`.
- `SQL:` JSONB na payload (styl audit V11).
- `SQL:` dlaczego **nie** reuse `event_publication`.

### Połączone z
- `payment/V2__create_payment_orders.sql` (wzorzec stylu)
- `shared/V6__create_event_publication.sql` (anty-wzorzec reuse)
- E2/E3 (konsumują tabele)

---

## Story E0-S3 / INFRA-CFG-01 — Config & secrets  
**Task:** `CPL-T03` · P0

### Jako / chcę / aby
Jako operator labu chcę bezpieczne defaulty i env secrets, aby lab nie wyciekał do prod.

### Acceptance criteria
- [ ] Default `app.checkout-lab.enabled: false` w `application.yml`.
- [ ] `application-dev.yml` / test: można włączyć lab.
- [ ] Secrets przez env (`CHECKOUT_LAB_OAUTH_SECRET`, `CHECKOUT_LAB_HMAC_SECRET`) w `.env.example` (bez realnych wartości).
- [ ] Brak commitowania sekretów.

### Learning
- `KC:` sekrety CPL **nie** żyją w realm Keycloak.
- `HTTP:` HMAC secret ≠ OAuth client secret (dwa różne hop’y).

### Połączone z
- `infra/compose/.env.example`
- E1-S1, E2-S2
