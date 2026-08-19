---
name: rest-api-security-oauth-testing
description: Use when designing and testing REST API security with Keycloak, OAuth/OIDC concepts, token behavior, authorization matrices, ownership boundaries, and tenant isolation.
---

# REST API Security, OAuth, OIDC and Keycloak Testing

## Use when
- designing protected endpoints,
- planning Keycloak/OIDC/OAuth learning,
- building 401/403/ownership test matrices,
- reviewing frontend-vs-backend security responsibility.

## Required questions
- Who calls the endpoint?
- Authenticated?
- Authorized?
- Which role/scope/permission?
- What about ownership?
- What about tenant isolation?
- What response for missing/invalid/expired token?

## When Not to Use
Do not use this for generic non-auth API design or full Keycloak operations troubleshooting outside project scope.

See [`references/auth-matrix-template.md`](references/auth-matrix-template.md).
