---
type: lesson
status: ready
area: REST API From Zero
lesson: 08
module: Summary Endpoint Contract, Status Codes, and Error Taxonomy
date: 2026-05-30
tags:
  - rest-api
  - http
  - contract
  - status-codes
  - error-taxonomy
  - lesson-08
  - payment-order-summary
  - senior-sdet
---

# Lesson 08 — Summary Endpoint Contract, Status Codes, and Error Taxonomy

> **Evidence link:**
> - `specs/005-payment-order-summary/contracts/payment-order-summary-api.md`
> - `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
> - `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
> - `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
>
> **Navigation:** [[REST API From Zero MOC]] | [[Lesson 08 - Payment Aggregation Summary]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Zrozumieć pełny kontrakt HTTP endpointu agregacyjnego summary:
- request: path params, query params, headers, auth,
- response: body shape, status codes, required/optional headers,
- error taxonomy: stabilne kody błędów (`validation`, `forbidden`, `not_found`),
- routing i matcher ordering w Spring Security,
- `X-Correlation-ID` jako kontrakt observability.

## 2. Prerequisites

- Podstawy HTTP: metody GET/POST, status codes, nagłówki.
- Struktura URL: `/api/merchants/{merchantId}/payment-orders/summary`.
- JWT Bearer token i role (`merchant:payments:read`, `platform:payments:read`).
- `@RestControllerAdvice` i `@ExceptionHandler` (Lesson 06).

## 3. Code Reading Map

| Plik | Odpowiedzialność |
|---|---|
| `payment-order-summary-api.md` | pełny kontrakt: endpoint, request/response, statusy, błędy, sortowanie |
| `PaymentOrderController.java` | handler `/summary`, own/plat reader, `X-Correlation-ID`, brak `ETag` |
| `SecurityConfig.java` | matcher `/summary` przed `/{paymentOrderId}` wildcard |
| `PaymentExceptionHandler.java` | `IllegalArgumentException` → 400, `AccessDeniedException` → 403, `DateTimeParseException` → 400 |

## 4. Kluczowe Pojęcia

### 4.1 Endpoint i URL design

```http
GET /api/merchants/{merchantId}/payment-orders/summary
```

- Zagnieżdżony pod merchantem — summary jest scoped do jednego merchanta.
- `/summary` jest podścieżką kolekcji (`/payment-orders`), nie osobnym top-level endpointem.
- Query params są **wszystkie opcjonalne** — brak parametrów = summary wszystkich orderów merchanta.

### 4.2 Status codes taxonomy

| Status | Condition | Error code | Body |
|---|---|---|---|
| 200 | success | — | `PaymentOrderSummaryResponse` JSON |
| 200 | empty merchant | — | `{ totalOrders: 0, totalAmountMinor: 0, byCurrency: [], byStatus: [] }` |
| 400 | invalid `currency`/`status`/date format | `validation` | `{ error: "validation", message: "...", correlationId: "..." }` |
| 401 | brak tokena / expired | — | (Spring Security default) |
| 403 | brak roli read / cross-tenant | `forbidden` | `{ error: "forbidden", message: "Access denied", correlationId: "..." }` |

### 4.3 Security matcher ordering

```java
.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/summary").hasAnyAuthority(...)  // line 40
.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*").hasAnyAuthority(...)        // line 41
```

- **Kolejność ma znaczenie** — Spring Security sprawdza matchery od góry do dołu.
- `/summary` MUSI być przed `/*`, inaczej `summary` zostanie potraktowane jako `{paymentOrderId}`.
- Skutek złej kolejności: 404 zamiast prawidłowego summary lub 403 zamiast 200.

### 4.4 Required vs optional headers

| Header | Required? | Kto ustawia |
|---|---|---|
| `Authorization: Bearer <token>` | Tak | Klient |
| `Accept: application/json` | Nie (ale zalecane) | Klient |
| `X-Correlation-ID` (request) | Nie | Klient lub `CorrelationIdFilter` |
| `X-Correlation-ID` (response) | Tak | Serwer (zawsze obecny) |
| `Content-Type: application/json` (response) | Tak | Serwer |
| `ETag` (response) | **Nie** | — summary nie ma ETag |

### 4.5 Error contract shape

```json
// 400 — validation
{
  "error": "validation",
  "message": "currency must be PLN, EUR, or USD",
  "details": null,
  "correlationId": "corr-l08-invalid-currency"
}

// 403 — forbidden (kontroler)
{
  "error": "forbidden",
  "message": "Access denied",
  "correlationId": "a1b2c3..."
}

// 403 — forbidden (security filter, bez body)
(empty response, no Content-Type)
```

- `details` — używane tylko gdy są field-level errors (`BindException`, `MethodArgumentNotValidException`).
- Security filter 403 różni się od kontrolera 403 — **nie zwraca JSON body**.

### 4.6 Error taxonomy for summary

| Błąd | Przykład | Status | error code | message |
|---|---|---|---|---|
| Invalid currency | `?currency=GBP` | 400 | `validation` | `currency must be PLN, EUR, or USD` |
| Invalid status | `?status=INVALID` | 400 | `validation` | `status must be CREATED` |
| Malformed date | `?fromDate=2026/05/30` | 400 | `validation` | `Invalid date format: 2026/05/30. Expected ISO date (YYYY-MM-DD)` |
| Malformed UUID | `merchantId=not-uuid` | 400 | `validation` | `Invalid merchantId: must be a valid UUID` |
| Missing auth | brak `Authorization` | 401 | — | (Spring Security) |
| Expired token | token przeterminowany | 401 | — | (Spring Security) |
| Wrong role | `merchant:payments:create` | 403 | `forbidden` | `Access denied` |
| Cross-tenant | reader A → summary B | 403 | `forbidden` | `Access denied` |
| `fromDate > toDate` | `?fromDate=2026-12-31&toDate=2026-01-01` | 200 | — | (empty summary, zero totals) |

## 5. Walkthrough — Request → Response

1. Klient wysyła `GET /api/merchants/{merchantId}/payment-orders/summary?currency=PLN` z `Authorization: Bearer <reader_token>` i `X-Correlation-ID: my-corr-123`.
2. `CorrelationIdFilter` — jeśli brak `X-Correlation-ID`, generuje `UUID`.
3. Spring Security — sprawdza matcher `/summary` (line 40), potwierdza rolę `merchant:payments:read`.
4. `PaymentOrderController.summarizePaymentOrders()` — sprawdza `merchant_id` claim.
5. Service `summarize()` — waliduje `currency=PLN`, parsuje daty (null), wywołuje repo.
6. Repo zwraca projekcję → service mapuje na `PaymentOrderSummaryResponse`.
7. Kontroler zwraca `200 OK` z `Content-Type: application/json`, `X-Correlation-ID: my-corr-123`, body JSON.
8. **Brak `ETag`** w odpowiedzi.

## 6. Delta vs Lesson 07

| Aspekt | Lesson 07 HTTP | Lesson 08 HTTP |
|---|---|---|
| Response body | `PaymentOrderListResponse` (content + page metadata) | `PaymentOrderSummaryResponse` (totals + grouped rows) |
| Pagination | `page`, `size`, `sort` params | brak |
| Sortowanie | param `sort` | stałe: `byCurrency ASC`, `byStatus ASC` |
| ETag | nie dotyczy listy | **jawnie NIE ma ETag** |
| Security matcher | `GET /api/merchants/*/payment-orders` (kolekcja) | `GET /api/merchants/*/payment-orders/summary` (przed `/*`) |
| Error kody | te same (`validation`, `forbidden`) | **reuse** + nowy `DateTimeParseException` handler |
| Nowe | — | error taxonomy tabela, matcher ordering reasoning |

## 7. Typowe Błędy i Antywzorce

| Błąd | Objaw | Poprawnie |
|---|---|---|
| Security matcher `/summary` za `/*` | `/summary` traktowane jako `{paymentOrderId}` → 404 | `/summary` przed `/*` |
| Zwracanie `ETag` dla summary | klient oczekuje wersjonowania, a go nie ma | brak headera `ETag` |
| `fromDate > toDate` → 400 | łamie kontrakt | 200 + empty summary |
| Brak `X-Correlation-ID` w response | observability broken | zawsze zwracaj correlation ID |
| Cross-tenant → 404 zamiast 403 | niespójność z listą | 403 (overt refusal) |
| Niespójne error codes | `currency_invalid` zamiast `validation` | zawsze `"validation"` dla 400 |

## 8. Ćwiczenia

1. **Narysuj** pełny flow requestu z `X-Correlation-ID` — gdzie jest tworzone, propagowane, zwracane?
2. **Dodaj w myśli** nowy query param `?limit=5` — czy to ma sens dla summary?
3. **Jak zmieni się** kontrakt gdy dojdzie nowy status `AUTHORIZED`?
4. **Który matcher** w SecurityConfig jest sprawdzany pierwszy dla `GET /api/merchants/123/payment-orders/summary`?
5. **Co zwróci** `GET .../summary?currency=PLN&status=CREATED&fromDate=2026-01-01` gdy merchant ma 0 orderów?

## 9. Pytania Kontrolne

1. Dlaczego security matcher `/summary` musi być przed `/*`?
2. Jaka jest różnica między 403 z JSON body a 403 bez body?
3. Co zwraca summary dla `fromDate > toDate`?
4. Który header jest wymagany w każdej odpowiedzi summary?
5. Jaki error code dostaje klient dla `?currency=GBP`?

## 10. Jak To Testować (Kontrakt)

- **Positive**: seed data → GET summary → sprawdź 200, strukturę JSON, totals.
- **Negative**: invalid params → 400, sprawdź `error: "validation"` i konkretne `message`.
- **Security**: matrix testów (7 przypadków: 401, 403×4, 200×2).
- **Headers**: zawsze asercjonuj `X-Correlation-ID`, sprawdź brak `ETag`.
- **Edge cases**: empty merchant, `fromDate > toDate`, `currency=PLN` gdy brak PLN orderów.

## 11. Next Links

- [[Lesson 08 - Payment Aggregation Summary]] — pełna notatka lekcji
- [[Lesson 08 - Aggregation Contract, Security, and Business Flow Tests]] — testy REST Assured
- [[Lesson 08 - Business Logic, Decision Tables, and Risk Notes]] — domain rules
- [[REST API From Zero MOC]]
- [[Lesson Evidence Tracker]]
