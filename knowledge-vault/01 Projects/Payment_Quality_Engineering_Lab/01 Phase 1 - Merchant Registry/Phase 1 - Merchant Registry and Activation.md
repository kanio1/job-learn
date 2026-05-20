# Phase 1 - Merchant Registry and Activation

Phase 1 introduces the first real business module before payment orders: merchants must exist, be identifiable, and have lifecycle state before future payments can belong to them.

## Links

- Spec: `specs/002-merchant-registry-activation/spec.md`
- Plan: `specs/002-merchant-registry-activation/plan.md`
- Tasks: `specs/002-merchant-registry-activation/tasks.md`
- Orientation: `docs/setup/phase-1-merchant-orientation-pack.md`
- Test design: `docs/testing/phase-1-merchant-test-design.md`

## Learning Themes

- Spring Modulith boundaries for the first business module.
- PostgreSQL constraints as final safety net for duplicate references.
- OAuth/OIDC boundary without exposing tokens to browser state.
- Parallel-safe test data from the first mutating feature.
