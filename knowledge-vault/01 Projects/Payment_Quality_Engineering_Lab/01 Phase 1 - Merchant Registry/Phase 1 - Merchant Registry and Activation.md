# Phase 1 - Merchant Registry and Activation

Phase 1 introduces the first real business module before payment orders: merchants must exist, be identifiable, and have lifecycle state before future payments can belong to them.

## Links

- Spec: `specs/002-merchant-registry-activation/spec.md`
- Plan: `specs/002-merchant-registry-activation/plan.md`
- Tasks: `specs/002-merchant-registry-activation/tasks.md`
- Orientation: `docs/setup/phase-1-merchant-orientation-pack.md`
- Test design: `docs/testing/phase-1-merchant-test-design.md`
- Payment gateway roadmap analysis: `docs/architecture/payment-gateway-roadmap-analysis.md`
- SDET learning plan: `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`
- Learning prompts: `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Prompts/README.md`
- REST Assured learning path: `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/README.md`
- SQL learning path: `knowledge-vault/02 Areas/Technical Learning/PostgreSQL and SQL From Zero/README.md`
- Java 25 learning path: `knowledge-vault/02 Areas/Technical Learning/Java 25 For SDET/README.md`
- Spring MVC learning path: `knowledge-vault/02 Areas/Technical Learning/Spring Boot Spring MVC/README.md`
- Spring Data JPA and Flyway path: `knowledge-vault/02 Areas/Technical Learning/Spring Data JPA and Flyway/README.md`
- Security learning path: `knowledge-vault/02 Areas/Technical Learning/Security and Authorization Testing/README.md`

## Learning Themes

- Spring Modulith boundaries for the first business module.
- PostgreSQL constraints as final safety net for duplicate references.
- OAuth/OIDC boundary without exposing tokens to browser state.
- Parallel-safe test data from the first mutating feature.
- REST Assured from zero to professional API testing.
- Merchant Registry as a learning laboratory before Payment Order.
- Spiral learning: each backend sprint repeats REST/API/test data/security while adding one new concept.
