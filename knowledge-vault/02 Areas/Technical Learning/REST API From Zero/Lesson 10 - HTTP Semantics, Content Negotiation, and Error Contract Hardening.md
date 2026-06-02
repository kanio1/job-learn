---
type: lesson
status: planned
area: REST API From Zero
lesson: 10
module: HTTP Semantics, Content Negotiation, and Error Contract Hardening
date: 2026-05-31
tags:
  - rest-api
  - http
  - content-negotiation
  - error-contract
  - route-ambiguity
  - lesson-10
  - senior-sdet
---

# Lesson 10 — HTTP Semantics, Content Negotiation, and Error Contract Hardening

> **Evidence link:** `PaymentOrderSummaryHttpContractRestAssuredTest.java` (planned), `SecurityConfig.java`, `PaymentExceptionHandler.java`
>
> **Navigation:** [[REST API From Zero MOC]] | [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Przejść od testowania "co API zwraca na happy path" (Lessons 01-09) do testowania **semantyki protokołu HTTP** — co API robi z nieoczekiwanymi nagłówkami, metodami, ścieżkami i formatami. To jest poziom senior: API quality to nie tylko poprawne JSON body, ale też poprawne zachowanie protokołu.

## 2. Prerequisites

- HTTP methods: GET, POST, PUT, PATCH, DELETE (Lesson 01).
- HTTP status codes: 2xx, 3xx, 4xx, 5xx (Lesson 01).
- Request/response headers: `Authorization`, `Content-Type`, `Accept`, `ETag`, `Location` (Lessons 04, 06, 08).
- URL path design: `/api/merchants/{merchantId}/payment-orders/summary`.
- Spring MVC `@RequestMapping`, `@GetMapping`, `@PathVariable UUID` (Lessons 06-08).
- Security: `SecurityConfig` matcher ordering (Lesson 06).

## 3. Code Reading Map

| Plik | Odpowiedzialność HTTP |
|---|---|
| `PaymentOrderController.java:144-176` | `@GetMapping("/summary")` — jedyna dozwolona metoda |
| `PaymentOrderController.java:76-104` | `@GetMapping("/{paymentOrderId}")` — wildcard, potencjalna kolizja z `/summary` |
| `SecurityConfig.java:40-41` | matcher ordering: `/summary` przed `/*` |
| `PaymentExceptionHandler.java:59-64` | `MethodArgumentTypeMismatchException` → 400 (malformed UUID) |
| `PaymentExceptionHandler.java:47-51` | `IllegalArgumentException` → 400 (invalid filter) |
| `PaymentExceptionHandler.java:84-88` | `AccessDeniedException` → 403 (forbidden) |

## 4. Kluczowe Pojęcia

### 4.1 Content negotiation — Accept vs Content-Type

| | Request (klient → serwer) | Response (serwer → klient) |
|---|---|---|
| **Co klient chce / co serwer dał** | `Accept: application/json` | `Content-Type: application/json` |
| **Co klient wysyła / co serwer akceptuje** | `Content-Type: application/json` | — (serwer nie negocjuje) |

- `Accept` — klient mówi: "potrafię przetworzyć te formaty".
- `Content-Type` (response) — serwer mówi: "wysłałem ci format X".
- `406 Not Acceptable` — serwer nie potrafi wyprodukować żadnego z formatów z `Accept`.
- `415 Unsupported Media Type` — serwer nie potrafi skonsumować formatu z request `Content-Type` (dotyczy POST/PUT, nie GET).

### 4.2 Method semantics — idempotentność i bezpieczeństwo

| Metoda | Bezpieczna? | Idempotentna? | Dla summary? |
|---|---|---|---|
| GET | Tak | Tak | **Obsługiwana** |
| HEAD | Tak | Tak | Prawdopodobnie (Spring automatycznie) |
| OPTIONS | Tak | Tak | Prawdopodobnie (Spring automatycznie) |
| POST | Nie | Nie | **NIE** |
| PUT | Nie | Tak | **NIE** |
| PATCH | Nie | Nie | **NIE** |
| DELETE | Nie | Tak | **NIE** |

- **Bezpieczna** = nie modyfikuje zasobu (GET, HEAD, OPTIONS).
- **Idempotentna** = wielokrotne wywołanie daje ten sam efekt (GET, PUT, DELETE).
- Summary jest read-only → tylko bezpieczne + idempotentne metody (GET).
- `405 Method Not Allowed` — serwer powinien zwrócić ten status + nagłówek `Allow: GET, HEAD, OPTIONS`.

### 4.3 Route ambiguity — literal path vs path variable

```
GET /api/merchants/{merchantId}/payment-orders/summary    ← literal "/summary"
GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}  ← wildcard
```

- Spring MVC dopasowuje **najpierw literalne ścieżki**, potem wildcard.
- `/summary` jest traktowane jako **literal** — NIE jako wartość `{paymentOrderId}`.
- `SecurityConfig` musi mieć tę samą kolejność: `/summary` przed `/*`.
- **Bez explicitego matchera przed wildcardem:** Spring Security może potraktować `summary` jako `paymentOrderId` → zła autoryzacja (np. wymaganie `merchant:payments:read` na obu ścieżkach, ale różne zachowanie kontrolera).

### 4.4 Conditional headers na resource bez ETag

```
Request:  GET /summary + If-None-Match: "abc123"
Response: 200 OK (pełne body)
           (brak ETag, brak 304 Not Modified)
```

- `ETag` jest potrzebny do warunkowego cachowania (`If-None-Match` → `304`).
- Summary **nie ma** `ETag` (Lesson 08 — świadoma decyzja).
- `If-None-Match` na resource bez `ETag` → serwer ignoruje nagłówek i zwraca normalną odpowiedź.
- **Nie** powinno być `304 Not Modified`, bo nie ma z czym porównać.
- **Nie** powinno być `412 Precondition Failed`, bo `If-None-Match` to nie `If-Match`.

### 4.5 Malformed path variable — UUID vs String

```
GET /api/merchants/not-a-uuid/payment-orders/summary
→ 400 Bad Request
{
  "error": "validation",
  "message": "Invalid merchantId: must be a valid UUID",
  "correlationId": "..."
}
```

- `@PathVariable UUID merchantId` — Spring próbuje `UUID.fromString("not-a-uuid")`.
- Wyjątek: `MethodArgumentTypeMismatchException`.
- `PaymentExceptionHandler.handleTypeMismatch(...)` → `400` + `error=validation`.
- **Co sprawdzić:** czy response ma `Content-Type: application/json` i `X-Correlation-ID`.

### 4.6 Error contract consistency — cross-endpoint comparison

| Error case | Endpoint | Status | error code | message |
|---|---|---|---|---|
| Invalid currency | `/summary` | 400 | `validation` | `currency must be PLN, EUR, or USD` |
| Invalid status | `/summary` | 400 | `validation` | `status must be CREATED` |
| Invalid date | `/summary` | 400 | `validation` | `Invalid date format: ...` |
| Malformed UUID | `/summary` | 400 | `validation` | `Invalid merchantId: must be a valid UUID` |
| Missing token | `/summary` | 401 | — | (Spring default) |
| Creator only | `/summary` | 403 | — | (Spring Security filter, brak body) |
| Cross-tenant | `/summary` | 403 | `forbidden` | `Access denied` |
| Not found | `/{id}` | 404 | `not_found` | `Payment order not found: ...` |
| Merchant not eligible | `POST /` | 409 | `merchant_not_payment_eligible` | `Merchant ... is not payment eligible` |
| Idempotency conflict | `POST /` | 409 | `idempotency_conflict` | `Idempotency key ... already used` |

- **Stabilność kontraktu:** wszystkie endpointy payment orders zwracają ten sam kształt błędu: `{ error, message, details?, correlationId }`.
- `details` — tylko dla field-level validation errors (`BindException`, `MethodArgumentNotValidException`).

## 5. Walkthrough — Od Malformed UUID Do Asercji

```
1. Klient wysyła GET /api/merchants/not-a-uuid/payment-orders/summary
   z poprawnym tokenem (merchant:payments:read)

2. Spring DispatcherServlet próbuje dopasować routing:
   - /api/merchants/{merchantId}/payment-orders/summary pasuje
   - Próbuje przekonwertować "not-a-uuid" → UUID → wyjątek

3. MethodArgumentTypeMismatchException rzucony przed wejściem do kontrolera

4. PaymentExceptionHandler.handleTypeMismatch(...) przechwytuje:
   - Tworzy PaymentErrorResponse("validation", "Invalid merchantId: ...", correlationId)
   - Zwraca ResponseEntity.badRequest()

5. Spring serializuje do JSON i wysyła z Content-Type: application/json

6. Test REST Assured sprawdza:
   - statusCode(400)
   - contentType(ContentType.JSON)
   - body("error", equalTo("validation"))
   - body("message", containsStringIgnoringCase("uuid"))
```

## 6. Learning Delta — Co Nowe vs Lessons 06-09

| Temat | Lesson 06-09 | Lesson 10 |
|---|---|---|
| Content negotiation | tylko `Content-Type: application/json` | `Accept`, `406`, `415` |
| HTTP methods | tylko GET i POST | `PUT`, `PATCH`, `DELETE`, `405` |
| Path params | poprawne UUID | malformed UUID → 400 |
| Route design | poprawne ścieżki | route collision guardrail |
| Conditional headers | `ETag` na create/read | `If-None-Match` na endpoint bez ETag |
| Error contract | per-endpoint error body | cross-endpoint error contract consistency |
| Security | ręczne testy | parameterized matrix |
| BOLA/BFLA | Lesson 06 pojęcia | Lesson 10 konkretne case'y: który wiersz macierzy to BOLA, który BFLA |
| API surface | nie testowaliśmy czego endpoint NIE robi | świadome testy unsupported methods |

## 7. Typowe Błędy

1. **Mylenie `Accept` z `Content-Type`.** `Accept` w requeście mówi co klient akceptuje w odpowiedzi. `Content-Type` w requeście mówi jaki format ma request body.
2. **Zakładanie, że Spring Boot zawsze zwróci 406.** Domyślnie może nie być skonfigurowanego `ContentNegotiationConfigurer`. Trzeba scharakteryzować.
3. **Testowanie unsupported method bez tokena.** Bez tokena → 401. Z tokenem → 405 (lub 403 z Security). Trzeba testować oba.
4. **Route collision: sprawdzanie tylko statusu 200.** Summary może zwrócić 200, ale być pustym summary. Trzeba sprawdzić **kształt body**.
5. **Zapominanie, że `details` jest `null` dla większości błędów.** Tylko `BindException` i `MethodArgumentNotValidException` dają field-level details.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Narysuj tabelę: każda metoda HTTP vs expected status dla summary. Oznacz które są bezpieczne, które idempotentne | 20 min |
| 2 | Wyślij request `Accept: application/xml` do summary — opisz co zwrócił Spring i dlaczego | 20 min |
| 3 | Porównaj error body dla: invalid currency vs malformed UUID vs missing token — które mają te same pola, które różne? | 25 min |
| 4 | Wyjaśnij dlaczego `405` powinno zawierać `Allow` header | 15 min |
| 5 | Znajdź w `PaymentExceptionHandler` handler dla `MethodArgumentTypeMismatchException`. Dlaczego ma tylko `message`, a nie `details`? | 20 min |
| 6 | Wytłumacz różnicę między `401` a `403` używając konkretnych case'ów z summary | 20 min |
| 7 | Jak byś przetestował, że `/summary` nie jest traktowane jako `/{paymentOrderId}`, gdyby kolejność matcherów była odwrotna? | 30 min |

## 9. Pytania

1. Co to jest content negotiation i dlaczego `Accept` header jest ważny w REST API?
2. Jaka jest różnica między `415 Unsupported Media Type` a `406 Not Acceptable`?
3. Dlaczego GET jest bezpieczny i idempotentny, a POST nie?
4. Co powinien zawierać nagłówek `Allow` w odpowiedzi `405`?
5. Dlaczego kolejność `@RequestMapping` ma znaczenie przy `/summary` vs `/{paymentOrderId}`?
6. Jak `MethodArgumentTypeMismatchException` trafia do `PaymentExceptionHandler`, skoro wyjątek jest rzucony przed wejściem do kontrolera?
7. Dlaczego `If-None-Match` na summary jest ignorowany?
8. Po czym poznać, że error contract jest stabilny między endpointami?
9. Co to jest BOLA i BFLA w kontekście summary endpoint?
10. Jak testujesz API surface — co endpoint robi, a czego NIE robi?

## 10. Testy

| Test | HTTP koncept |
|---|---|
| `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape` | route ambiguity, literal vs wildcard |
| `malformedMerchantIdReturnsValidationError` | path variable parsing, 400 |
| `unsupportedMethodsDoNotExposeSummaryMutationSurface` | method semantics, 405 |
| `unsupportedAcceptIsRejectedOrExplicitlyCharacterized` | content negotiation, 406 |
| `ifNoneMatchDoesNotEnableSummaryCaching` | conditional headers, brak ETag |
| `summaryAccessMatrixEnforcesAuthenticationAuthorizationAndOwnership` | BOLA/BFLA, 401/403/200 |

## 11. Powiązane Notatki

- [[Lesson 08 - Summary Endpoint Contract, Status Codes, and Error Taxonomy]]
- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Senior SDET Competency Coverage Matrix]]
- [[REST API From Zero MOC]]
