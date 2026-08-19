# Mapa testów — Mirror Labs (as-built)

Dokumentacja **task-force** (PM + test architect + test analyst).  
Cel: kompletna mapa przypadków dla **zaimplementowanego** Mirror Labs (T01–T29 + poprawki z review, 2026-08-13). **Nie jest to implementacja** brakujących speców.

Źródła: kod w `apps/frontend` (hub, Session/Visual/Network, bank, BFF Nitro) i `apps/backend` (`checkoutlab` Wave 3, modulith `mirrorlab`, Flyway V15/V16).  
Roadmapa: [status/roadmaps/browser-session-visual-network-lab/](../../status/roadmaps/browser-session-visual-network-lab/).  
Sesja BFF vs OIDC: [session-bff-oidc-contract](../session-bff-oidc-contract.md).  
CPL (nie duplikować 1:1): [docs/testing/checkout-protocol-lab/](../checkout-protocol-lab/).

## Jak czytać

1. Co jest w produkcie i jakie oracles → **01**.
2. Warstwy, ryzyko, split mock/POM → **02**.
3. UI E2E → **03**.
4. REST (Spring + BFF: sesja, CSRF, PayU HTTP, bank) → **04**.
5. Visual + network interception → **05**.
6. EP/BVA → **06**.
7. DT / ST / UC / pairwise / error guessing → **07**.
8. FR → TC → **08**.

## Indeks

| Plik | Zawartość | Prefiks |
|---|---|---|
| [01-business-gap-analysis.md](01-business-gap-analysis.md) | As-built inventory, kontrakty, luki **testowe** | `GAP-*` |
| [02-test-strategy.md](02-test-strategy.md) | Piramida, oracles, ISTQB | — |
| [03-playwright-e2e-catalog.md](03-playwright-e2e-catalog.md) | Session, visual, network, widget, bank UI | `PW-MRL-E2E-###` |
| [04-playwright-api-session-cookies.md](04-playwright-api-session-cookies.md) | Flag, CSRF, cookies, GET-body, refund, bank API | `PW-MRL-API-###` |
| [05-playwright-visual-network.md](05-playwright-visual-network.md) | Screenshots, route, HAR | `PW-MRL-VIS-*`, `PW-MRL-NET-*` |
| [06-istqb-ep-bva.md](06-istqb-ep-bva.md) | Partycje i granice (wartości z kodu) | `EP-MRL-*`, `BVA-MRL-*` |
| [07-istqb-decision-state-usecase.md](07-istqb-decision-state-usecase.md) | DT / ST / UC | `DT-MRL-*`, `ST-MRL-*`, `UC-MRL-*` |
| [08-traceability-matrix.md](08-traceability-matrix.md) | FR → ID + P0–P2 | — |

## Warstwy wykonania

```text
Learner POM          tests-pom-learner          — copy-map; bez importu z tests-pom
Live POM             tests-pom, zero fulfill    — Keycloak, cookies, clock, download, dual role
PW request / BFF     APIRequestContext :3000    — CSRF, flag 404, TPP header
REST Assured         *Test.java / *IT.java      — Spring flag, multipart, refund, TPP
Modulith             ModulithArchitectureTest   — granica mirrorlab
Flyway / JPA         Schema IT                  — V15 mrl_*, V16 REFUNDED
Unit (designed)      idle TTL, CSRF compare     — bez Spring
```

`tests/e2e` (mocked Chromium) **nie istnieje**. Visual goldens i ARIA są w live POM (`PLAYWRIGHT_VISUAL=1`). HMAC / refund / TPP / multipart **needs-backend**.

## Pokrycie (kolumna w katalogach)

| Wartość | Znaczenie |
|---|---|
| `existing-pw` | Historyczne — drzewo `tests/e2e` usunięte; ślad = `existing-pom` |
| `existing-pom` | Jest w `tests-pom` |
| `existing-ra` | Jest w REST Assured `*Test.java` |
| `existing-it` | Jest w `*IT.java` (Failsafe / Surefire IT) |
| `designed` | Zaplanowany, **nie implementować** z tej mapy |
| `docs-only` | Anti-case / checklist, bez TC |

Priorytet **dalszych** testów (designed): **P0** sesja/re-auth/flag BFF, **P1** CSRF isolation / TPP 429 / PDF przez żywy BFF, **P2** pairwise CORS / dark / learner.

## Problem+json — słownik Mirror Labs + delty CPL

`Content-Type: application/problem+json`.  
`type` = `https://api.payment-quality.local/problems/{error-with-dashes}`.

| `error` | HTTP | Warstwa | Kiedy |
|---|---|---|---|
| `csrf_failed` | 403 | Nitro `/api/session-lab/csrf-demo` | brak / mismatch `X-Csrf-Token` vs cookie `mrl-csrf` |
| `get_with_body` | 403 | Spring filter (GET z body) | Content-Length > 0 lub Transfer-Encoding |
| `refund_not_allowed` | 409 | Spring checkout refund | status ≠ `COMPLETED` albo już `REFUNDED` |
| `step_up_required` | 403 | Spring high-value | `amountMinor >= 10000` i brak `X-Lab-Step-Up: confirmed` |
| `unsupported_evidence_content_type` | 415 | Spring evidence | typ poza pdf/png/jpeg/txt/csv |
| `evidence_file_too_large` | 413 | Spring evidence | size > 2 MiB (servlet max 5 MiB) |
| `empty_evidence_file` | 400 | Spring evidence | pusty plik |
| `invalid_evidence_filename` | 400 | Spring evidence | path segment / `..` |
| `self_approve_forbidden` | 403 | Spring approve | checker == maker subject |
| `approval_not_pending` | 409 | Spring approve | status ≠ `PENDING_APPROVAL` |
| `consent_owner_mismatch` | 403 | Spring revoke | actor ≠ `ownerSubject` |
| `consent_denied` | 403 | Spring TPP | brak tokenu / unknown / REVOKED |
| `rate_limit_exceeded` | 429 | Spring TPP | > 30 GET/min/IP |
| `not_found` | 404 | Spring | spór / approval / consent / flag off (IT) |

**Nie** problem+json: lab Bearer 401 (CPL); BFF flag off **404** tekstowy Nitro; dashboard POST merchant **bez** CSRF (kontrast).

## Poza zakresem

- Mobile projects, real PSP, PAN/3DS, Kafka, KYC, Berlin Group produkcyjny.
- Argos/Percy; zmiana realm Keycloak.
- `payment_orders` refund (maker-checker **nie** wiąże lifecycle).
- Uczenie „KC JWT = PayU token”.
- Implementacja `designed` z tej mapy.
