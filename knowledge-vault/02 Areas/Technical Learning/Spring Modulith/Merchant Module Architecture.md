# Merchant Module Architecture

The merchant module owns merchant registry and activation behavior. Its internals live under `lab.paymentquality.merchant.internal`; other modules must not depend on those internals.

## Boundary

- Public module marker: `lab.paymentquality.merchant.package-info.java`.
- Internal implementation: domain, application service, repository, and web adapter.
- Shared security/web utilities remain outside merchant and must not depend on merchant internals.

## Tests

- `ModulithArchitectureTest` verifies global module boundaries.
- `MerchantModuleTest` boots the merchant module with `@ApplicationModuleTest` and verifies core beans.

## Design Note

No domain events are introduced in Phase 1 because no other module needs to react to merchant changes yet.
