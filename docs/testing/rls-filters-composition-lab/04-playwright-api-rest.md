# 04 — REST (Playwright request / REST Assured)

## RA-RFC-010 — JDBC no-GUC count=0

| | |
|---|---|
| Pokrycie | existing-ra `RlsLabRestAssuredTest.jdbcWithoutGucSeesZeroRowsAndWithGucSeesAlpha` |
| Prio | P0 |
| Kroki | `DriverManager` jako `rls_lab_app`; `SELECT count(*) FROM rls_lab_item` bez GUC → 0; `set_config('app.tenant_id', alpha, true)` → 1 |
| Uczy | FORCE RLS wiąże **non-owner**. Superuser Testcontainers nie jest oracle RLS. |

## RA-RFC-011 — REST list nie używa owner DS

| | |
|---|---|
| Pokrycie | existing-ra compare `restrictedWithoutTenantGuc=0` + Alpha list size 1 |
| Prio | P0 |
| Uczy | Drugi DataSource. Gdyby list szedł ownerem, `restrictedWithoutTenantGuc` nie byłoby 0. |

## RA-RFC-012 — Alpha list 1 wiersz

| | |
|---|---|
| Pokrycie | existing-ra |
| Prio | P0 |

## RA-RFC-013 — GET other item 404 `not_found`

| | |
|---|---|
| Pokrycie | existing-ra |
| Prio | P0 |

## RA-RFC-014 — merchant compare 403 (brak authority)

| | |
|---|---|
| Pokrycie | existing-ra `RlsLabRestAssuredTest.merchantWithoutPlatformReadIsForbiddenByMethodSecurity` |
| Prio | P0 |
| Kroki | JWT `merchant:payments:read` + `TENANT_ALPHA` → `GET /api/rls-lab/compare` |
| Asercje | HTTP **403**; **nie** asertować `error=rls_forbidden` (matcher + `@PreAuthorize` kończą wcześniej) |
| Uczy | Brak `platform:payments:read` to 403 Spring Security, nie problem domeny RLS. |

## RA-RFC-018 — compare 403 `rls_forbidden` (authority + merchant tenant)

| | |
|---|---|
| Pokrycie | existing-ra `platformReadOnMerchantTenantIsRlsForbidden` |
| Prio | P0 |
| Kroki | JWT `platform:payments:read` + `TENANT_ALPHA` → `GET /compare` |
| Asercje | 403, `error=rls_forbidden` |
| Uczy | Trzecia bramka: `TenantContext.isPlatformScoped()`. Authority ≠ platform tenant. |

## RA-RFC-019 — platform compare JSON (BYPASSRLS, nie GUC)

| | |
|---|---|
| Pokrycie | existing-ra `platformCompareShowsLeakContrast` + `supportAgentWithPlatformReadCanCompare` + IT `compareReturnsLeakContrastForPlatform` |
| Prio | P0 |
| Asercje | `bypassRoleCount=2`; `restrictedWithoutTenantGuc=0`; `unprotected=2`; support.agent (ma `platform:payments:read` + `PLATFORM_TENANT`) → 200 |
| Uczy | Support agent widzi leak demo, bo ma tę samą authority co panel UI (`canReadPlatformPayments`). |

## RA-RFC-015 — flag off 404

| | |
|---|---|
| Pokrycie | existing-it `RlsLabEndpointsDisabledIT` |
| Prio | P0 |

## RA-RFC-016 — fake GUC `app.rls_bypass` nie wycieka

| | |
|---|---|
| Pokrycie | existing-ra `jdbcFakeBypassGucDoesNotLeakRows` |
| Prio | P0 |
| Kroki | `rls_lab_app` + `set_config('app.rls_bypass', 'on', true)` → count 0 |
| Uczy | GUC ustawiane przez klienta nie jest mechanizmem platformy. |

## RA-RFC-017 — owner i BYPASSRLS role widzą 2 wiersze

| | |
|---|---|
| Pokrycie | existing-ra `jdbcOwnerSeesBothRowsAndBypassRoleSeesBothRows` |
| Prio | P0 |
| Kroki | JDBC owner count = 2; `rls_lab_bypass` count = 2 |
| Uczy | FORCE RLS wiąże table owner bez BYPASSRLS; superuser i `rls_lab_bypass` omijają polityki. |

## RA-RFC-020 — fromDate/toDate today

| | |
|---|---|
| Pokrycie | existing-ra `PaymentOrderListRestAssuredTest` |
| Prio | P0 |
| Oracle | `LocalDate.now(ZoneOffset.UTC)` — te same UTC day bounds co `PaymentOrderSpecification.createdBetween` |

## RA-RFC-021 — future date empty

| | |
|---|---|
| Pokrycie | existing-ra |
| Prio | P0 |

## RA-RFC-022 — min/max amount

| | |
|---|---|
| Pokrycie | existing-ra |
| Prio | P0 |

## RA-RFC-023 — min>max → 400 `validation`

| | |
|---|---|
| Pokrycie | existing-ra |
| Prio | P0 |

## RA-RFC-024 — status+amount combo

| | |
|---|---|
| Pokrycie | existing-ra |
| Prio | P0 |

## PW-RFC-API-001 — POM BffClient list/compare/get item

| | |
|---|---|
| Pokrycie | existing-pom (oracle w specs, nie osobny api spec) |
| Prio | P1 |
| Uczy | Ten sam cookie session co UI; BFF mapuje 404 problem+json. |

## RA-RFC-030 — forwarded headers (Wave B)

Pełny katalog proxy/TLS: [09-wave-b-stack-tls-catalog.md](09-wave-b-stack-tls-catalog.md).

| ID | Przypadek | Klasa / metoda | Pokrycie |
|---|---|---|---|
| RA-RFC-030 | wrogie `Host` / `X-Forwarded-*` / `Forwarded` | `createLocationStaysRelativeWhenForwardedHostAndProtoAreHostile` | existing-ra |
| RA-RFC-031 | nagłówki jak Caddy `reverse_proxy` | `createLocationStaysRelativeWhenHeadersMatchCaddyReverseProxyDefaults` | existing-ra (Spring; bez kontenera Caddy) |
| RA-RFC-032 | `iss` TLS-lab vs HTTP Keycloak | `TlsLabIssuerMismatchRestAssuredTest` (3 testy) | existing-ra |
| RA-RFC-033 | CORS HTTP + HTTPS `:8443` + evil 403 | `TlsLabCorsRestAssuredTest` (4 testy) | existing-ra |

## RA-RFC-036 — modulith `rlslab`

| | |
|---|---|
| Pokrycie | existing `RlsLabModuleTest` |
| Prio | P1 |
| Asercje | moduł `rlslab` w grafie; brak explicit named interfaces; żaden inny moduł nie zależy od `rlslab` |
| Uczy | Lab izolacji DB nie jest API płatności. `testing` seeduje SQL-em nazwy tabel, bez importu `rlslab`. |
