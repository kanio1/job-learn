# Module map

## Contents

- Detected modules
- Package layout
- Allowed cross-module edges
- Events
- New module checklist

## Detected modules

Root: `lab.paymentquality` (`PaymentQualityApplication`). Each **direct** sub-package is a module. Annotated roots:

| Package | Display name | Notes |
|---|---|---|
| `merchant` | Merchant Registry | Public: `MerchantPaymentEligibilityService`, `MerchantPaymentEligibility`, `MerchantSeedCapability` |
| `payment` | Payment Orders | Public: `PaymentSeedCapability`, `PaymentOrderSeed`. Depends on merchant **public** eligibility API (`@MockitoBean` in `PaymentModuleTest`) |
| `tenant` | Tenant Registry | Merchant may import `lab.paymentquality.tenant.*` only |
| `iam` | Identity & Access Management | |
| `audit` | Audit Log | Listens to `shared.events` |
| `shared` | (OPEN) | Security, CORS, authorities, `AuditableActionOccurred`. OPEN is the documented idiom for this cross-cutting module — do not copy OPEN onto feature modules |
| `testing` | Testing Support | |
| `checkoutlab` | Checkout Protocol Lab | Feature-flagged; not a product PSP |
| `mirrorlab` | Mirror Lab | Feature-flagged |
| `rlslab` | RLS Lab | Feature-flagged |
| `eventlab` | Event Streaming Lab | Flag-gated Kafka overlay (ADR 0002). Placement: `eventlab-kafka`. Implementation in progress — confirm current state on the `KAFKA-T*` task board before assuming code exists |

`foundation.status` is a supporting package for `GET /api/status`, not a product module. Do not grow it into a second web stack.

`shared.web` is MVC exception mapping / filters / OpenAPI — not a dumping ground.

## Package layout

Copy an existing module. Typical tree:

```
lab.paymentquality.<module>/
  package-info.java          # @ApplicationModule(displayName = "…")
  PublicSeam.java            # interfaces / DTOs other modules may import
  internal/
    application/             # services, use-cases
    domain/                  # entities, value objects, domain exceptions
    infrastructure/          # JPA repositories, adapters
    web/                     # controllers, HTTP DTOs, exception handlers, mappers
```

Rules:

- Other modules import **root types only**. `internal` exists so types can be `public` for intra-module wiring without becoming API.
- Do not introduce `@NamedInterface` packages unless a spec explicitly needs a second inbound API (SPI). Today the unnamed API (module root) is enough.
- Do not set `allowedDependencies` unless you are tightening an existing leak and have a failing `verify()` first.

## Allowed cross-module edges

| From | To | How |
|---|---|---|
| `payment` | `merchant` | `MerchantPaymentEligibilityService` at merchant **root**. Never `merchant.internal` |
| `merchant` | `tenant` | Public tenant API only. Never `tenant.internal` |
| feature modules | `shared` | Allowed because `shared` is OPEN (authorities, events, security types) |
| any | `payment.internal` | Forbidden |

If a new call would require `*.internal`, either move a type to the module root (small public seam) or publish an event that the other module already understands. Prefer the existing `AuditableActionOccurred` over a new event type unless the spec says otherwise.

## Events

Already on the classpath: `spring-modulith-events-api` + `spring-modulith-events-jpa`. `application.yml` sets `spring.modulith.events.republish-outstanding-events-on-restart: true`.

Use `ApplicationEventPublisher` the way `MerchantService` / `PaymentLifecycleService` already do. Keep the existing `event_publication` outbox. Do **not** add a second outbox table or put Kafka producers/listeners in `shared` / `audit` / `payment`. Kafka externalization is **only** `eventlab` (`eventlab-kafka` skill, ADR 0002). Do not start using `Scenario` event tests unless the slice is specifically async event delivery.

## New module checklist

Only when a real feature owns behavior (see root `package-info.java`). Then:

1. `package-info.java` with `@ApplicationModule(displayName = "…")`.
2. `internal/` layout as above.
3. Flyway folder `db/migration/<module>/` and add it to `spring.flyway.locations` in `application.yml` **and** `application-test.yml` if that file lists locations.
4. `*ModuleTest` next to the module package, `@ActiveProfiles("test")`, Testcontainers via `PostgresContainerSupport`.
5. `ApplicationModules.of(PaymentQualityApplication.class).verify()` still green (`ModulithArchitectureTest`).
6. Do not enable `checkoutlab` / `mirrorlab` / `rlslab` / `eventlab` style labs unless the user asked for that lab (Event Streaming Lab tickets are `KAFKA-T*` on the kafka roadmap).
