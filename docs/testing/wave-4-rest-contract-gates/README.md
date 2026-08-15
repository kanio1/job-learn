# Wave 4 — REST contract gates

Dokumentacja **task-force**. Wave 4 zamyka `REST-MULTIPART-01` (black-box `apps/api-tests`) i zapisuje decyzję `REST-OPENAPI-DRIFT-01`. **Nie** dodaje springdoc, CI drift, QUERY, `@Retryable`, versioningu ani RLS na `payment_orders`.

OpenAPI ADR: [docs/architecture/openapi-ownership.md](../../architecture/openapi-ownership.md).

## Zakres

- Czarnoskrzynkowy Failsafe spec na **payment-order evidence** (`POST` / `GET` `.../payment-orders/{id}/evidence`).
- Decyzja code-first; tooling OpenAPI = **Wave 5**.

## Poza Wave 4

- springdoc, `/v3/api-docs`, Swagger UI, committed `openapi.yaml`.
- Mirror-lab disputes w api-tests (backend RA nie zamyka bramki).
- GET-by-`evidenceId`, nowe kody `error`, zmiana 201/Location.
- QUERY, API versioning, `@Retryable`, Actuator, RLS na tabelach produkcyjnych.

Stop: nie wymyślać nowego kontraktu biznesowego. Produkcja tylko gdy focused test znajdzie defekt istniejącego API.

## Use cases

Format: aktor · precondition · kroki · oracle.

### UC-W4-01 — Happy-path evidence (AT-MP-01…04) — P0

- **Aktor:** `merchant.alpha.creator` (`Identities.seededMerchantCreator()`).
- **Kroki:** POST part `file` (PNG) na order Alpha 001; GET lista.
- **Oracle:** `201`; względny `Location` `.../evidence/{evidenceId}`; lista `200` + `ResponseSpecs.sensitive()`; body bez pola `storageKey`.

### UC-W4-02 — Negatives construction (AT-MP-05a…f, 06, 09) — P0

- **Kroki:** missing part, empty, unsupported type, 2 MiB+1, exact 2 MiB, blank filename, `../secret.txt`, JSON zamiast multipart.
- **Oracle:** `400` `validation` / `empty_evidence_file` / `missing_evidence_filename` / `invalid_evidence_filename`; `415` `unsupported_evidence_content_type`; `413` `evidence_file_too_large`; exact 2 MiB → `201`; JSON → `415` `unsupported_media_type` (global MVC, nie evidence-specific detail).

### UC-W4-03 — Truncated multipart (AT-MP-07) — P1

- **Kroki:** `Content-Type: multipart/form-data` z poprawnym boundary, body bez zamykającego delimitera.
- **Oracle:** `400` `application/problem+json` `error=validation`. Parse failure (`MultipartException`) is mapped in `GlobalExceptionHandler` to the existing `validation` code — no new domain `error`.

### UC-W4-04 — Cross-merchant denial (AT-MP-08 / AT-MP-10) — P0

- **Kroki:** token Alpha vs order Alpha 002.
- **Oracle:** POST evidence → `403` `forbidden` (`verifyMerchantOwnership`); GET list → `404` `not_found` (`findReadablePaymentOrder` BOLA mask).

## Indeks ID → spec

| ID | Pokrycie | Spec | Oracle |
|---|---|---|---|
| AT-MP-01 | existing-api-tests | `PaymentEvidenceMultipartContractSpec` | part `file` + filename + content-type |
| AT-MP-02 | existing-api-tests | ten sam | minimal PNG `image/png` → 201 |
| AT-MP-03 | existing-api-tests | ten sam | `Location` względny `.../evidence/{id}` |
| AT-MP-04 | existing-api-tests | ten sam | GET list 200 + sensitive headers; body bez `storageKey` |
| AT-MP-05a | existing-api-tests | ten sam | missing part `file` → 400 `validation` |
| AT-MP-05b | existing-api-tests | ten sam | empty → 400 `empty_evidence_file` |
| AT-MP-05c | existing-api-tests | ten sam | 415 `unsupported_evidence_content_type` |
| AT-MP-05d | existing-api-tests | ten sam | 2 MiB+1 → 413 `evidence_file_too_large` |
| AT-MP-05e | existing-api-tests | ten sam | blank filename → 400 `missing_evidence_filename` |
| AT-MP-05f | existing-api-tests | ten sam | exact 2 MiB → 201 |
| AT-MP-06 | existing-api-tests | ten sam | `../secret.txt` → 400 `invalid_evidence_filename` |
| AT-MP-07 | existing-api-tests | ten sam | truncated multipart → 400 `validation` (probe; no new `error`) |
| AT-MP-08 | existing-api-tests | ten sam | cross-merchant POST 403 `forbidden` |
| AT-MP-09 | existing-api-tests | ten sam | JSON body → 415 `unsupported_media_type` |
| AT-MP-10 | existing-api-tests | ten sam | cross-merchant GET list 404 `not_found` |

## Warstwy

```text
apps/api-tests *Spec  →  Spring :8080 POST/GET .../evidence
                         (brak assertu DB; read proof = GET list)
Backend RA            →  PaymentOrderEvidenceRestAssuredTest (częściowe AC; nie zamyka bramki)
Backend IT            →  TestEndpointsEnabledIT.resetAfterEvidenceUploadReturns200AndClearsEvidenceRows
Mirror lab RA         →  poza Wave 4
```

## Produkcja (focused defect)

- `PaymentSeedService.clear()` kasuje `payment_order_evidence` i `payment_order_note` przed `payment_orders`. Bez tego `POST /api/test/reset` łamał FK po happy-path uploadzie.
- `GlobalExceptionHandler` mapuje `MultipartException` (przed kontrolerem) na `400` `validation`. Truncated multipart wcześniej dawał `500`.

## Następna fala

Wave 5: springdoc + `/v3/api-docs` **zaimplementowane** — [wave-5-openapi-springdoc](../wave-5-openapi-springdoc/README.md). AT-MP oracles bez zmian (OpenAPI nie jest źródłem prawdy dla multipart). Snapshot + CI = Wave 6.
