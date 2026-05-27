---
type: moc
status: draft
area: Security and Authorization Testing
date: 2026-05-21
tags:
  - security-testing
  - keycloak
  - oauth
  - oidc
  - jwt
  - sdet
---

# Security and Authorization Testing - MOC

Ta ścieżka uczy Keycloak/OAuth/OIDC/JWT i testów autoryzacji przez realne endpointy Payment Quality Engineering Lab. Celem jest rozumieć 401/403, role, ownership i tenant isolation na poziomie co najmniej średnim, docelowo zaawansowanym.

## Lekcje

| Lesson | Topic | What tester must understand |
|---:|---|---|
| 1 | Authentication vs Authorization | kim jesteś vs co możesz |
| 2 | HTTP 401 vs 403 | missing/invalid token vs insufficient authority |
| 3 | JWT anatomy | header, payload, signature, claims |
| 4 | Bearer token | jak token trafia do backendu |
| 5 | Keycloak realm/client/user/role | minimum operacyjne Keycloak |
| 6 | Spring Resource Server | Spring sprawdza token |
| 7 | Role converter | Keycloak roles -> Spring authorities |
| 8 | Authorization matrix | endpoint x role x expected status |
| 9 | Ownership/tenant isolation | merchant A nie widzi danych merchant B |
| 10 | Token expiry/invalid token | negatywne security tests |
| 11 | PKCE in Nuxt | browser login bez tokenów w JS |
| 12 | Security logging | nie logować Authorization |
| 13 | Role drift risk | allowlist ról, nadmiarowe uprawnienia |
| 14 | Security review checklist | review endpointów |

## Security Repeat Per Endpoint

Każdy nowy endpoint musi odpowiedzieć:

- Who calls the endpoint?
- Authenticated?
- Authorized?
- Which role/scope/permission?
- What about ownership?
- What about tenant isolation?
- What response for missing, invalid or expired token?

## Zasada SDET

Security tests nie są dodatkiem po happy path. W systemie płatniczym 401/403/ownership są częścią kontraktu biznesowego i regresji wysokiego ryzyka.
