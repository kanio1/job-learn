# 02 — Strategia testów (test architect)

Wave A uczy **trzech oracles**: UI, HTTP (BFF vs Spring), SQL (tylko JDBC w RA).

## 1. Cele jakości

| Cel | Oracle |
|---|---|
| RLS ≠ Java WHERE | JDBC `rls_lab_app` bez GUC = 0; REST 404 na obcy UUID |
| Table owner / superuser omija RLS | JDBC owner count = 2; `rls_lab_app` + fake `app.rls_bypass` = 0; bypass role = 2 |
| Filtr UI = query API | ten sam `minAmount`/`status` w URL i `BffClient.listPaymentOrders` |
| Page-reset | po Apply brak `page=1`; request `page=0` lub brak page |
| Widget 1-based | klik „2” → query `page=1` |
| POM czysty | zero `page.route` / `fulfill` |
| JWT nie w Web Storage | `expectNoTokenInBrowserStorage` |
| Desktop only | brak project `iPhone` |

## 2. Piramida

```text
        PW-E2E live POM TLS (PKCE HTTPS :8443 hybrid albo `--full`)
           PW-E2E live POM HTTP (filtry vs API, RLS dual Keycloak, ConfirmModal)
              PW-E2E mocked + FE flag-off :3010
                 REST Assured (date/amount, BOLA 404, compare 2×403, JDBC, Location, issuer, CORS)
                    IT flag on/off
                       Modulith + Flyway V17–V18
                          Stack smoke (dev-stack.sh)
```

Wave B (stos, TLS, TC): [09](09-wave-b-stack-tls-catalog.md). Wave 3 UC/TC: [wave-3-compose-tls-pom](../wave-3-compose-tls-pom/). RA-032/033 są **existing-ra**.

**Zasada:** Playwright nie łączy się z Postgres. SQL oracle = `RlsLabRestAssuredTest` JDBC. UI oracle = widoczność wiersza + URL. API oracle = `BffClient` na `:3000`.

## 3. Ryzyko → głębokość

| Ryzyko | Prio | Technika | Warstwa |
|---|---|---|---|
| Superuser omija FORCE RLS | P0 | JDBC owner vs `rls_lab_app` | RA |
| Client-settable GUC bypass | P0 | `SET app.rls_bypass=on` jako `rls_lab_app` | RA count=0 |
| REST list na owner DS | P0 | JdbcTemplate restricted role | RA compare `restrictedWithoutTenantGuc=0` |
| Apply zostawia page=1 | P0 | URL + request | mocked + POM |
| USelect native select | P1 | count `select` | POM |
| Confirm bez POST | P0 | GET status CREATED | POM (`confirm-action-dismiss`) |
| Flag off 404 | P0 | IT | existing-it |
| FE flag-off | P1 | osobny project `playwright.rls-flag-off.config.ts` | existing-pom |
| X-Forwarded przepisuje Location | P0 | RA hostile + Caddy-shaped headers | existing-ra RA-030/031 |
| PKCE / iss TLS | P0 | live POM TLS + issuer-uri | existing-pom + existing-ra RA-032 |
| CORS HTTPS obok HTTP | P1 | OPTIONS Origin | existing-ra RA-033 |
| Rootless :443 | P1 | Caddy 8443 | existing-setup |

## 4. Dane

| Parametr | Wartość |
|---|---|
| Alpha item | `00000000-0000-0000-0000-0000000000a1` label `Alpha secret` |
| Other item | `00000000-0000-0000-0000-0000000000a2` label `Other tenant secret` |
| Rola DB | `rls_lab_app` / `rls_lab_app`; `rls_lab_bypass` / `rls_lab_bypass` |
| GUC | tylko `app.tenant_id` (SET LOCAL). Platforma = osobna rola BYPASSRLS, nie GUC |
| Flaga FE | default on (`!== 'false'`) |
| Flaga Spring | default false; dev true |
| List status API | tylko `CREATED` (`@Pattern`) |
| TLS lab origin | `https://app.payment-quality.local:8443` (`CADDY_HTTPS_PORT`) |

## 5. Zakaz

- `waitForTimeout` jako sync.
- `page.route` w `tests-pom`.
- RLS na `payment_orders` „przy okazji”.
- Uczenie „dashboard JWT = GUC” bez `SET LOCAL` na restricted role.
