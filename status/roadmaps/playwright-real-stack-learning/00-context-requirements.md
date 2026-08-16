---
name: playwright-real-stack-learning-requirements
origin: POST_KIRO_WORK
status: IMPLEMENTED
last_updated: 2026-08-15
---

# Wymagania — Playwright na realnym stosie

## Cel

Każda nowa kontrolka Playwright ma uzasadnienie biznesowe (ops, compliance, concurrency) i oracle poza mockiem: backend + BFF + (dla POM) Keycloak.

## FR

| ID | Wymaganie |
|---|---|
| FR-HEAD | BFF proxy `HEAD` payment order z ETag / Cache-Control / Vary jak GET |
| FR-REST | Live `APIRequestContext` ze `storageState` Keycloak: status, 201 Location, 304, HEAD, 428 Problem Details + Zod, multipart evidence |
| FR-EVIDENCE | Kategoria INVOICE/RECEIPT/OTHER, dropzone, GET bajtów, thumbnail `alt`, download |
| FR-CLIP | Copy client reference i correlation id (`navigator.clipboard`) |
| FR-OFFLINE | Banner offline na liście płatności (`navigator.onLine` + `offline` event) |
| FR-SWEEP | `POST /api/payment-ops/expiration-sweep` (platform lifecycle) woła istniejący expiration service |
| FR-OIDC | Session lab **End OIDC session**: clear BFF cookie + Keycloak `end_session` (`client_id` + `post_logout_redirect_uri`, **bez** `id_token_hint`). Menu Sign out / idle Unlock = tylko BFF (`auth.logout`) — **nie** ten FR. Produkt DONE; Playwright hop **designed** ([session-bff-oidc-contract](../../../docs/testing/session-bff-oidc-contract.md)) |
| FR-TITLE | Status badge `title="{STATUS}"` dla `getByTitle` |
| FR-BANK-UI | Mirror bank maker-checker przez `ConfirmActionModal` (API Spring bez zmian kontraktu testidów) |
| FR-AXE | `@axe-core/playwright` na login i merchants list |
| FR-EXPORT | `POST .../export-jobs` → 202 + Location + Retry-After; GET job; GET content gdy READY; sync `GET .../export` zostaje |
| FR-DC | Maker `PENDING` refund request; ten sam subject → 409 `dual_control_self_approve`; checker → istniejący refund + If-Match |

## NFR

- Spring Modulith: nowe typy w `payment.internal`
- Flyway V19–V21; JPA `ddl-auto: validate`
- Worker exportu wyłączony w profilu `test`; testy wołają `processDueJobs()`
- Playwright 1.61; bez native dialog

## Non-goals

GraphQL, WebSocket, WebAuthn, Kafka, real PSP, i18n, mobile matrix, native `page.on('dialog')`, cross-origin iframe, fake rate limiter.
