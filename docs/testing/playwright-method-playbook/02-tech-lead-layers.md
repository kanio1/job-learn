# 02 — Tech-lead: warstwy i trzy API

```text
E2E live POM     tests-pom          UI + waitForBff / waitForRequest
PW REST / BFF    BffClient          cookie sesji, IPv4
REST Assured     apps/backend + api-tests   pełna macierz — nie dubluj 1:1
Mocked e2e       tests/e2e          page.route — poza live POM
```

Stop-gate: modal / badge / deep-link = E2E. Status+header+body = REST lub RA. Playwright nie woła SQL.

## Trzy kategorie API (MUST)

| Kategoria | Endpointy | Kto w POM |
|---|---|---|
| PRODUCT | `/api/merchants/**`, payments, users, audit, lab UI | tak |
| CONTRACT DATA | `POST /api/test/seed` (~104) | tylko jeśli stack nie jest `dev,seed` |
| DATA LEARNING / ETL | `seed-learning`, `etl/payments/full\|incremental\|rebuild` | **nigdy** |

10k replace niszczy asercje ~104. `BffClient` nie ma metod ETL. Grep `seed-learning` / `etl/payments` w `tests-pom` = 0.

IPv6: Node `127.0.0.1`, przeglądarka `localhost` (OIDC). Overlay Vite: locator handler, nie klikać. Nie mieszać `--app` i `--full`.
