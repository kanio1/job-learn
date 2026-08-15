# Wave 5 — OpenAPI springdoc

Dokumentacja **task-force**. Wave 5 zamyka tooling `REST-OPENAPI-DRIFT-01`: code-first **generator** (springdoc) + filtr ścieżek + polityka ekspozycji `/v3/api-docs`. **Nie** commituje `openapi.yaml`, **nie** dodaje oasdiff CI, QUERY, `@Retryable`, versioningu ani RLS na `payment_orders`.

ADR: [docs/architecture/openapi-ownership.md](../../architecture/openapi-ownership.md). Wave 4 (multipart): [wave-4-rest-contract-gates](../wave-4-rest-contract-gates/README.md).

## Zakres

- Runtime `GET /v3/api-docs` z filtrem exclude set z ADR.
- Ekspozycja: authenticated na non-prod; wyłączone na `prod`; Swagger UI **off wszędzie**.
- Black-box asercje: publiczne ścieżki obecne, lab/test nieobecne, brak wycieku `.internal`.
- `apps/api-tests` jest **konsumentem** dokumentu, nie generatorem.

## Poza Wave 5

- Committed `openapi.yaml` / oasdiff CI — **Wave 6**, dopiero gdy generation jest stabilne.
- Swagger UI (`springdoc-openapi-starter-webmvc-ui`), SDK, drugi generator (OpenAPI Generator / spec-first YAML).
- Adnotowanie każdego headera (`ETag`, `Vary`, `Cache-Control`, `Idempotency-Replayed`) — allowlist ADR: brak w spec ≠ drift.
- Zmiana kontraktu HTTP, GET-by-`evidenceId`, QUERY, API versioning, `@Retryable`, Actuator, RLS na tabelach produkcyjnych.
- CORS na `/v3/**` (obecny CORS jest tylko `/api/**`; api-tests biją Spring bezpośrednio). Caddy już proxy całe `:8080` — bez nowej trasy.

Stop: nie wymyślać nowego kontraktu biznesowego. Produkcja tylko gdy focused test znajdzie defekt istniejącego API. Nie zamykać bramki jako CI drift.

## Decyzje (lock)

| Pole | Wybór |
|---|---|
| Mode | Code-first. Kontrolery i rekordy request/response są źródłem zachowania HTTP. |
| Owner | `apps/backend`. springdoc tylko tam. |
| Consumer | `apps/api-tests` asertuje live HTTP. **Nie** generuje OpenAPI. |
| Artifact | Runtime `/v3/api-docs` z path filtering. |
| Starter | `org.springdoc:springdoc-openapi-starter-webmvc-api` **3.1.0** (macierz: springdoc 3.x ↔ Boot 4.0.x). **Bez** `-ui`. |
| Auth | **Bez** `permitAll` na `/v3/**`. Łańcuch `.anyRequest().authenticated()` w `SecurityConfig`. |
| Prod | `springdoc.api-docs.enabled=false` → 404 nawet z JWT. |
| Filtr | Ścieżki lab + pakiety lab (`checkoutlab`, `mirrorlab`, `rlslab`, `testing`). **Nie** wykluczać `*.internal` — kontrolery produkcyjne żyją w `*.internal.web`; to opróżni spec. |

## Include / exclude

**Include (muszą być w `paths`):**

- `/api/status`
- `/api/merchants/**` (create/list/get/activate/suspend/risk-flag)
- `/api/merchants/{merchantId}/payment-orders/**` (w tym evidence, notes, history, summary, export, HEAD/OPTIONS/PATCH)
- `/api/users/**`
- `/api/audit/**`
- `/api/tenants/current/settings`

**Exclude set (ADR):**

- `/api/checkout-lab/**`
- `/api/mirror-lab/**`
- `/api/rls-lab/**`
- `/api/test/**`
- Error Lab / Nitro-only (brak Spring controllera)
- Actuator (nie istnieje; gdy pojawi się później — nie publikować bez ops exception)

Oracle exclude musi działać gdy laby są **włączone** (`dev` / obraz api-tests), nie tylko gdy flagi są off.

## Use cases

Format: aktor · precondition · kroki · oracle.

### UC-W5-01 — Ekspozycja (OA-EXP-01…04) — P0

- **Aktor:** dowolny seeded JWT (np. `Identities.seededMerchantCreator()`); kontrast: brak tokenu.
- **Kroki:** `GET /v3/api-docs`; `GET /swagger-ui.html` i `/swagger-ui/index.html`; wyłączenie `springdoc.api-docs.enabled=false`.
- **Oracle:** z JWT → `200` OpenAPI JSON. Bez tokenu → `401` `application/problem+json` `error=unauthorized`. Authenticated Swagger UI → `404` (brak startera `-ui`; anonymous na `/swagger-ui.html` to `401` z łańcucha, nie wyciek UI). Disabled → `404` nawet z JWT.

### UC-W5-02 — Exclude lab (OA-FLT-01…04) — P0

- **Precondition:** laby zarejestrowane (flagi on). Backend RA włącza checkout/mirror/rls/test; live api-tests używa profilu `dev`.
- **Kroki:** authenticated `GET /v3/api-docs`; odczyt `paths`.
- **Oracle:** brak kluczy zaczynających się od `/api/checkout-lab`, `/api/mirror-lab`, `/api/rls-lab`, `/api/test`.

### UC-W5-03 — Public surface (OA-PUB-01…02) — P0

- **Kroki:** ten sam dokument.
- **Oracle:** obecne: `/api/status`, merchants, payment-orders (w tym `/evidence`), users, audit, `/api/tenants/current/settings`.

### UC-W5-04 — Brak wycieku internals (OA-INT-01) — P0

- **Kroki:** body dokumentu (schema keys + `$ref`).
- **Oracle:** brak `.internal`. Nie włączać FQN (`springdoc.use-fqn`).

### UC-W5-05 — Allowlist (OA-ALW-01) — P1 dokumentacyjny

- **Oracle testu:** **nie** pada na `problem+json` vs domyślny error schema; względny `Location` na 201; brak adnotacji headerów. Zapis w tym katalogu + ADR, nie failing assertion.

## Wpływ na istniejące use cases i test cases

Wave 5 **nie zmienia** kontraktu biznesowego. Istniejące UC/TC zostają źródłem prawdy dla HTTP; OpenAPI jest mapą odkrywania, nie oraclem statusów/headerów.

| Istniejący UC / TC | Wpływ Wave 5 |
|---|---|
| UC-W4-01…04 / AT-MP-01…10 (evidence multipart) | **Brak zmiany zachowania.** POST/GET evidence, `400` truncated, `415` JSON, BOLA `404` list, względny `Location` bez GET-by-`evidenceId` — bez zmian. OA-PUB wymaga ścieżki `/evidence` w dokumencie; OA-ALW-01 zabrania padać testowi OpenAPI, gdy `Location` jest względny albo brak `@Header`. |
| UC-W3-* / live POM (guest, Error Lab, TLS, IDOR) | **Brak.** Frontend i BFF nie wołają `/v3/api-docs`. CORS nadal tylko `/api/**`. Error Lab (Nitro) nie jest w spec (nie ma Spring controllera). |
| Checkout / mirror / RLS lab UC i RA | **Runtime bez zmian** (flagi, 302, multipart disputes, RLS compare). Ścieżki lab **znikają z dokumentu** nawet gdy kontrolery są zarejestrowane — to jest OA-FLT, nie regresja labów. Testy lab (np. `CheckoutLabProtocolRestAssuredTest`) nie asertują OpenAPI. |
| `POST /api/test/seed\|reset` / `TestEndpointsEnabledIT` | Endpointy działają jak wcześniej. Nie publikować ich w `/v3/api-docs` (OA-FLT-04). |
| Security smoke 401/403 (merchants, payments) | **Bez zmiany.** Nowy sąsiad: anonymous `/v3/api-docs` też `401` `unauthorized` (OA-EXP-02) — ten sam łańcuch `.anyRequest().authenticated()`, **bez** `permitAll`. |
| Problem+json / ETag / Vary / Idempotency RA | **Oraclem zostają istniejące specy** (`ProblemAssert`, `ResponseSpecs.sensitive()`). Generowany dokument może pomijać te headery i używać domyślnych error schemas — OA-ALW-01; nie traktować tego jako drift w Wave 5. |
| `ModulithArchitectureTest` | Config w `shared` (OPEN). Brak nowej zależności międzymodułowej. |

Nowy surface do utrzymania: `GET /v3/api-docs` (authenticated). Nie dodaje GET-by-`evidenceId`. Snapshot YAML + oasdiff = Wave 6.

## Indeks ID → spec

| ID | Pokrycie | Spec | Oracle |
|---|---|---|---|
| OA-EXP-01 | existing-api-tests + existing-ra | `OpenApiContractSpec`, `OpenApiRestAssuredTest` | authenticated `GET /v3/api-docs` → 200 OpenAPI JSON |
| OA-EXP-02 | existing-api-tests + existing-ra | ten sam | anonymous GET → 401 `unauthorized` problem+json |
| OA-EXP-03 | existing-api-tests + existing-ra | ten sam | authenticated `GET /swagger-ui.html` (i `/swagger-ui/index.html` w RA) → 404 |
| OA-EXP-04 | existing-backend-it | `OpenApiDisabledIT` | `springdoc.api-docs.enabled=false` → 404 z JWT |
| OA-FLT-01 | existing-api-tests + existing-ra | `OpenApiContractSpec`, `OpenApiRestAssuredTest` | brak `/api/checkout-lab` w `paths` (laby on) |
| OA-FLT-02 | existing-api-tests + existing-ra | ten sam | brak `/api/mirror-lab` |
| OA-FLT-03 | existing-api-tests + existing-ra | ten sam | brak `/api/rls-lab` |
| OA-FLT-04 | existing-api-tests + existing-ra | ten sam | brak `/api/test` |
| OA-PUB-01 | existing-api-tests + existing-ra | ten sam | status, merchants, payment-orders (+ `/evidence`) |
| OA-PUB-02 | existing-api-tests + existing-ra | ten sam | users, audit, tenants/current/settings |
| OA-INT-01 | existing-api-tests + existing-ra | ten sam | body bez `.internal` |
| OA-ALW-01 | existing-docs | katalog + ADR | nie failing: problem+json, relative Location, omitted headers |

## Warstwy

```text
apps/api-tests OpenApiContractSpec  →  GET /v3/api-docs (consumer, nie generator)
Backend RA                         →  OpenApiRestAssuredTest (laby on: 401, 200+filtr, swagger 404, no .internal)
Backend IT                         →  OpenApiDisabledIT (api-docs off → 404)
ModulithArchitectureTest           →  config w shared (OPEN) bez łamania granic
```

Wzorzec wyłączenia: `TestEndpointsDisabledIT` / `OpenApiDisabledIT` (404 gdy feature off).

## Implementacja

1. `apps/backend/pom.xml`: `springdoc-openapi-starter-webmvc-api` **3.1.0**.
2. `application.yml`: `paths-to-exclude` + `packages-to-exclude` labów; `swagger-ui.enabled=false`.
3. `application-prod.yml`: `springdoc.api-docs.enabled=false`.
4. `OpenApiConfiguration` w `lab.paymentquality.shared.web`: bean `OpenAPI` z `Info` + Bearer JWT — bez exclude pakietu `internal`.
5. Caddy: bez nowej trasy.

## Następna fala

Wave 6: committed snapshot + breaking-change CI z allowlistą z [openapi-ownership.md](../../architecture/openapi-ownership.md). Dopiero po zielonym live Failsafe `OpenApiContractSpec`.
