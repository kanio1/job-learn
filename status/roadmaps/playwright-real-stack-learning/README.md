---
name: playwright-real-stack-learning
origin: POST_KIRO_WORK
status: IMPLEMENTED
related_gate: Playwright 1.61 curriculum on Spring + BFF + Keycloak (not mocks-only)
last_updated: 2026-08-15
---

# Playwright na realnym stosie — backlog wykonawczy

Uzupełnienia operacyjnego dashboardu płatności (evidence, export async, sesja OIDC, refund dual-control) tak, żeby kontrolki Playwright 1.61 miały oracle na **Spring + Nitro BFF + Keycloak + UI + Postgres**.

To **nie** jest Kiro spec. Nie edytować `.kiro/**`.

## Jak czytać ten katalog

| Plik | Rola |
|---|---|
| [README.md](./README.md) | Indeks, granice, fale |
| [00-context-requirements.md](./00-context-requirements.md) | Cel, FR, non-goals |
| [learning-map.md](./learning-map.md) | Lekcja → feature → spec |
| [task-board.md](./task-board.md) | Fale 1–4 |

Sesja vs OIDC: [docs/testing/session-bff-oidc-contract.md](../../../docs/testing/session-bff-oidc-contract.md).

## Fale

1. Playwright REST przez BFF (`APIRequestContext`, HEAD, 304, multipart, Problem Details + Zod)
2. Modyfikacje UI: dropzone, radio, thumbnail/GET bytes, clipboard, offline, expiration sweep, OIDC `end_session` (produkt; TC hopu designed), `title` na badge, bank ConfirmActionModal, axe
3. Async export-jobs `202` + `Location` + worker + poll + download (sync CSV zostaje)
4. Maker-checker na prawdziwym refundzie `payment_orders` (dwa `storageState`)

## Granice (MUST)

- Bez GraphQL, WebSocket, passkeys, geolocation, native `window.confirm`, Kafki, prawdziwego PSP/PCI, i18n packs, mobile device matrix, fake rate limiter, BasePage.
- REST Assured (`apps/api-tests` + `apps/backend` RA) zostaje ownerem pełnej macierzy; Playwright REST = ta sama semantyka przez cookie BFF.
- Mocked `tests/e2e` tylko jako kopia dydaktyczna po kontrakcie live/RA.
- Labi checkout / mirror / RLS / error zostają; dual-control na `payment_orders` jest osobny od mirror lab.

## Testy

| Warstwa | Gdzie |
|---|---|
| REST Assured | `PlaywrightLearningStackRestAssuredTest`, evidence RA |
| Playwright REST (live) | `tests/live/http/bff-rest-contract.spec.ts`, `conditional-get-and-head.spec.ts` |
| Live POM | `tests-pom/specs/payments-refund-dual-control.spec.ts` |
| Mocked Chromium | `tests/e2e/evidence-upload.spec.ts`, `payments-offline.spec.ts`, `payment-order-read.spec.ts`, `payments-async-export.spec.ts`, `a11y-axe.spec.ts` |
