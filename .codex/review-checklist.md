# Codex Review Checklist — Tenant Model and Isolation

Use this checklist before and after each implementation wave.

## Scope Guardrails

- Do not edit `.kiro/**` for implementation progress.
- Do not modify `payment` module source files for tenant isolation unless the user explicitly changes scope.
- Do not add frontend, Playwright, PSP, settlement, reconciliation, KYC, or dashboard work on tenant-isolation waves.
- Kafka belongs in Event Streaming Lab tickets only (`KAFKA-T*`, `eventlab-kafka`). See `.codex/review-checklist-eventlab.md`.
- Do not change existing authority strings or remove existing `@PreAuthorize` annotations.
- Do not weaken or delete existing tests to make a wave pass.

## Module Boundary Review

- `merchant` may import `lab.paymentquality.tenant.*` public API.
- `merchant` must not import `lab.paymentquality.tenant.internal.*`.
- `payment` must not import `merchant.internal.*` or `tenant.internal.*`.
- If a new tenant public API method is needed, add it under `lab.paymentquality.tenant`, not inside `tenant.internal`.
- `ModulithArchitectureTest` must remain green.

## Persistence Review

- Flyway owns schema; JPA must validate against migrations.
- `merchants.tenant_id` must be non-null in the database.
- `Merchant` maps `tenant_id` as plain `UUID`, not `@ManyToOne` to tenant internals.
- Tenant-filtered repository methods must filter by UUID `tenantId`, not tenant reference string.
- Platform-created merchants must persist a real tenant UUID from seeded tenant data.

## Security and Isolation Review

- Tenant-scoped principal:
  - List shows only own tenant merchants.
  - Detail for foreign tenant merchant returns masked `404`.
  - Activate/suspend foreign tenant merchant returns `403`.
  - Create ignores body `tenantReference` and assigns own tenant.
- Platform-scoped principal:
  - Can read all merchants.
  - Can optionally filter list by tenant.
  - Must provide valid `tenantReference` when creating a merchant.
- Missing or unresolvable JWT `tenant_id` must lead to `403` on tenant-scoped merchant resources.
- Tenant-related `403` bodies must not reveal foreign tenant IDs/references or resource existence.

## REST Contract Review

- Existing merchant endpoints and paths stay the same.
- The only controlled additive request-body field is `tenantReference` on `POST /api/merchants`.
- The only controlled additive query parameter is `tenantId` on `GET /api/merchants`.
- Existing `X-Correlation-ID` behavior must remain intact.
- Existing status codes unrelated to tenant boundaries must remain unchanged.

## Test Review

After Wave 2:

```bash
cd apps/backend
./mvnw test
```

After Wave 3 and before deeper integration work:

```bash
cd apps/backend
./mvnw test
./mvnw verify
```

Do not proceed to Wave 4 or Wave 5 if basic compile/unit/module checks are broken.

## Documentation Review

- Update `.codex/current-state.md` after a completed wave.
- Mention any minimal public tenant API addition in `.codex/current-state.md`.
- Do not check off `.kiro/specs/tenant-model-and-isolation/tasks.md` boxes unless the user explicitly asks for Kiro task maintenance.
