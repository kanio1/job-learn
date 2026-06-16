# Repository Structure — Payment Quality Engineering Lab

## Top-Level Layout

```
job-learn/
├── apps/
│   ├── backend/          # Java / Spring Boot backend
│   └── frontend/         # Nuxt 4 frontend
├── infra/
│   ├── compose/          # Docker Compose files + .env.example
│   └── keycloak/         # Keycloak realm import
├── docs/                 # Project documentation
├── knowledge-vault/      # Obsidian learning vault
├── references/           # External reference material
├── specs/                # Legacy spec files (pre-Kiro)
├── .kiro/                # Kiro IDE configuration
│   ├── specs/            # Kiro Feature Specs (requirements / design / tasks)
│   ├── skills/           # Kiro workspace skills
│   └── steering/         # Kiro persistent project context (this file)
├── .kilocode/            # Kilo Code configuration (preserved, do not delete)
│   ├── skills/           # Kilo Code skills (source of truth for Kilo)
│   ├── rules/            # Kilo Code rules
│   └── workflows/        # Kilo Code workflow definitions
└── AGENTS.md             # Top-level agent rules (applies to Codex CLI)
```

## Backend Structure (`apps/backend/src/main/java/lab/paymentquality/`)

```
lab/paymentquality/
├── foundation/
│   └── status/           # GET /api/status — health/public endpoint
├── merchant/
│   ├── MerchantModule.java          # Public module API (Spring Modulith)
│   └── internal/
│       ├── application/             # MerchantService (business logic)
│       ├── domain/                  # Merchant entity, value objects, exceptions
│       └── web/                     # MerchantController, request/response DTOs,
│                                    #   MerchantExceptionHandler, MerchantMapper
└── payment/
    ├── PaymentModule.java           # Public module API (Spring Modulith)
    └── internal/
        ├── application/             # PaymentOrderService, PaymentLifecycleService,
        │                            #   PaymentOrderListService, PaymentOrderSummaryService
        ├── domain/                  # PaymentOrder entity, value objects, ETag logic,
        │                            #   exceptions, status history
        └── web/                     # PaymentOrderController, PaymentExceptionHandler,
                                     #   request/response DTOs, mappers, HTTP header utils
```

**Spring Modulith rules:**
- Public module APIs live under the module root package (e.g. `merchant/`, `payment/`).
- Implementation lives under `internal/`. Never import `*.internal.*` from another module.
- The payment module must NOT depend on `merchant.internal`.
- Module boundaries are verified by `ModulithArchitectureTest`, `MerchantModuleTest`, `PaymentModuleTest`.

## Backend Test Structure (`apps/backend/src/test/java/lab/paymentquality/`)

```
lab/paymentquality/
├── rest/                 # RestKit helpers (thin REST Assured client layer)
│   ├── MerchantRestClient.java
│   ├── PaymentOrderRestClient.java
│   └── ...
├── merchant/             # Merchant module tests (unit + integration)
├── payment/              # Payment module tests (unit + integration)
├── security/             # Security tests (import TestJwtConfiguration)
└── support/
    ├── PostgresContainerSupport.java   # Base for DB-dependent tests
    └── TestJwtConfiguration.java       # JWT test token factory
```

**Test conventions:**
- DB-dependent tests extend `PostgresContainerSupport`; Flyway owns the schema.
- Spring-context tests use `@ActiveProfiles("test")`.
- REST Assured tests live in `rest/` and use RestKit helpers.
- Security tests live in `security/` and import `TestJwtConfiguration`.
- Architecture tests are top-level.
- Ignore learner copies (`My*`, `Lesson*`) unless the task explicitly concerns learning files.

## Frontend Structure (`apps/frontend/`)

```
apps/frontend/
├── app/
│   ├── app.vue                    # Root: UApp > NuxtLayout > NuxtPage
│   ├── app.config.ts              # Nuxt UI colors (primary: green, neutral: zinc)
│   ├── assets/css/main.css        # Global CSS (minimal)
│   ├── layouts/
│   │   └── dashboard.vue          # UDashboardGroup + UDashboardSidebar shell
│   ├── middleware/
│   │   └── auth.global.ts         # Global auth route guard
│   ├── pages/
│   │   ├── index.vue              # Dashboard Overview (home)
│   │   ├── login.vue              # Login page
│   │   ├── error-lab.vue          # Error Lab (HTTP learning)  [planned]
│   │   └── admin/merchants/
│   │       ├── index.vue          # Merchant list + create
│   │       └── [merchantId]/payments/
│   │           ├── index.vue      # Payment order list + filters
│   │           ├── new.vue        # Create payment order
│   │           └── [paymentOrderId].vue  # Payment order detail + lifecycle
│   ├── components/
│   │   ├── merchant/              # CreateMerchantForm, MerchantStatusBadge, MerchantTable
│   │   ├── payment/               # CreatePaymentOrderForm, PaymentOrderDetail,
│   │   │                          #   PaymentOrderListTable, PaymentOrderSummaryCards,
│   │   │                          #   PaymentStatusBadge
│   │   └── shared/                # [planned] BusinessStatusBadge, HttpStatusBadge,
│   │                              #   HeaderKeyValuePanel, ProblemDetailsCard,
│   │                              #   RawJsonViewer, IdempotencyKeyInput, EtagDisplay,
│   │                              #   IfMatchInput, ApiDebugPanel, EmptyStateCard,
│   │                              #   LoadingState, ErrorState, ConfirmActionModal
│   ├── schemas/
│   │   ├── merchant.schema.ts     # Zod: createMerchantSchema
│   │   ├── payment-order.schema.ts # Zod: create/response/list/summary/history schemas
│   │   ├── problem-details.schema.ts  # [planned] Zod: ProblemDetails
│   │   └── app-shell.schema.ts    # App shell Zod schema
│   └── stores/
│       ├── auth.ts                # Session-backed login/logout
│       ├── payment-orders.ts      # List/detail/history/lifecycle console state
│       └── app-shell.ts           # Shell UI state
├── server/
│   ├── api/merchants/             # Proxy routes: merchants CRUD + activate/suspend
│   │   └── [merchantId]/payment-orders/   # Proxy routes: payment orders + lifecycle
│   ├── routes/auth/               # OIDC auth routes (nuxt-auth-utils)
│   └── utils/
│       └── backendApi.ts          # Token attach + header forward
│                                  #   (ETag, Location, Vary, X-Correlation-ID,
│                                  #    Cache-Control, Accept-Patch, Allow)
├── shared/types/                  # Shared TypeScript types (api.ts planned)
├── tests/
│   ├── .auth/                     # Playwright auth storage state
│   ├── auth/auth.setup.ts         # Auth setup (platform-operator session)
│   └── e2e/                       # Playwright E2E specs
├── nuxt.config.ts                 # Nuxt config (modules, runtimeConfig, routeRules)
├── playwright.config.ts           # Playwright config
└── package.json
```

**Frontend conventions:**
- All backend API calls go through `server/api/**` proxy routes — never directly from the browser.
- The proxy attaches the bearer token server-side via `backendApi.ts`.
- `backendApi.ts` forwards these headers to the browser: `ETag`, `Location`, `Vary`, `X-Correlation-ID`, `Cache-Control`, `Accept-Patch`, `Allow`.
- Use `$fetch.raw` (not plain `$fetch`) in composables that need to read response headers and status.
- Prefer composables for API calls; use Pinia only for genuinely shared cross-component state.
- Zod schemas live in `app/schemas/`. Validate every API response before rendering.

## Kiro Specs (`/.kiro/specs/`)

Each feature spec lives in `.kiro/specs/{feature-name}/`:

```
.kiro/specs/{feature-name}/
├── .config.kiro       # specId, specType, workflowType
├── requirements.md    # User stories + EARS acceptance criteria
├── design.md          # Architecture, components, sequence diagrams, correctness properties
└── tasks.md           # Small, dependency-ordered implementation tasks
```

## Kiro Skills (`/.kiro/skills/`)

One folder per skill, each containing `SKILL.md` with valid frontmatter:

```yaml
---
name: skill-name          # matches folder name, kebab-case
description: Use when...  # activation-friendly trigger description
---
```

## Kiro Steering (`/.kiro/steering/`)

Persistent context files injected into every Kiro session:

| File | Purpose |
|---|---|
| `product.md` | Project purpose, business domains, learning goals, non-goals |
| `tech.md` | Stack versions, commands, preferred approach |
| `structure.md` | This file — directory layout, conventions |
| `rest-api-contracts.md` | HTTP contract rules, headers, status codes |
| `frontend-nuxt-ui.md` | Nuxt UI dashboard conventions, component rules |
| `testing-strategy.md` | Test layer strategy, RestKit, Playwright, assertions |
