---
type: lesson
status: planned
area: JUnit REST Assured
lesson: 10
module: HTTP Edge Contract, Route Guardrails, and Matrix Tests
date: 2026-05-31
tags:
  - rest-assured
  - http
  - contract-testing
  - authorization-matrix
  - route-guardrail
  - lesson-10
  - senior-sdet
---

# Lesson 10 — HTTP Edge Contract, Route Guardrails, and Matrix Tests

> **Evidence link:** `PaymentOrderSummaryHttpContractRestAssuredTest.java` (planned), `PaymentOrderSummaryAuthorizationMatrixTest.java` (planned)
>
> **Navigation:** [[JUnit REST Assured MOC]] | [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się testować **semantykę HTTP** — nie tylko JSON body i status codes (jak w Lessons 06-09), ale również:
- `Accept` header i content negotiation,
- unsupported methods (`PUT`, `PATCH`, `DELETE` na read-only endpoint),
- malformed path variables (UUID),
- route collision guardrail (`/summary` vs `/{paymentOrderId}`),
- conditional headers (`If-None-Match`) na endpointzie bez `ETag`,
- parameterized authorization matrix zamiast ręcznie pisanych metod.

## 2. Prerequisites

- REST Assured `given()/when()/then()` i podstawowe asercje (Lessons 01-05).
- `extract().as(ResponseClass.class)` + AssertJ (Lessons 06-08).
- `TestJwtSupport` — tokeny z różnymi rolami i `merchant_id`.
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers (Lesson 06).
- Endpoint summary: `GET /api/merchants/{merchantId}/payment-orders/summary`.

## 3. Code Reading Map — Istniejące Testy

| Plik | Co pokrywa | Co NIE pokrywa |
|---|---|---|
| `PaymentOrderSummaryRestAssuredTest.java` | 200/400/header/filter/date — 10 testów | `Accept`, unsupported methods, malformed UUID, route collision |
| `PaymentOrderSummarySecurityTest.java` | 7 ręcznych testów: 401/403/200 | parameterized matrix, BFLA bez `merchant_id`, platform non-payment role |
| `PaymentOrderSummaryBusinessFlowRestAssuredTest.java` | seed → oracle, cross-tenant, platform | — (pokryte) |
| `PaymentOrderListRestAssuredTest.java` | list filters, pagination, sort | HTTP edge dla listy (poza scope Lesson 10) |

## 4. Kluczowe Pojęcia

### 4.1 Accept header — content negotiation

```java
given()
    .header("Authorization", "Bearer " + token)
    .accept("text/html")  // zamiast "application/json"
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
.then()
    .statusCode(406);  // Not Acceptable — oczekiwane zachowanie
```

- `Accept` mówi serwerowi **jaki format odpowiedzi klient akceptuje**.
- `Content-Type` (w response) mówi **jaki format serwer faktycznie zwrócił**.
- Różnica: `Accept` = request (klient → serwer), `Content-Type` = response (serwer → klient).
- Jeśli serwer nie wspiera żądanego formatu → `406 Not Acceptable`.
- **Uwaga:** Spring Boot domyślnie może zwrócić 200 z JSON nawet przy `Accept: text/html`, jeśli nie ma skonfigurowanego `ContentNegotiationConfigurer`. Test w Lesson 10 musi **scharakteryzować** rzeczywiste zachowanie, nie zakładać.

### 4.2 Unsupported methods — zawężanie API surface

```java
given()
    .header("Authorization", "Bearer " + readerToken)
    .contentType(ContentType.JSON)
    .body("{}")
.when()
    .put("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
    // .patch(...)
    // .delete(...)
.then()
    .statusCode(405);  // Method Not Allowed
```

- Summary to **read-only** endpoint — tylko `GET` i ewentualnie `HEAD`/`OPTIONS` powinny działać.
- `PUT`, `PATCH`, `DELETE`, `POST` na summary powinny zwracać `405` (lub `401` jeśli brak auth, ale `405` przy poprawnym tokenie).
- **Dlaczego to ważne:** API surface powinno być **intencjonalnie wąskie**. Niechciane metody to potencjalne wektory ataku.
- **Uwaga:** Spring Security może odrzucić request przed kontrolerem (403 zamiast 405). Test musi scharakteryzować, co faktycznie zwraca system.

### 4.3 Malformed path variable — UUID validation

```java
given()
    .header("Authorization", "Bearer " + readerToken)
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", "not-a-uuid")
.then()
    .statusCode(400)
    .body("error", equalTo("validation"))
    .body("message", containsString("UUID"));
```

- `@PathVariable UUID merchantId` — Spring próbuje sparsować string jako UUID.
- Gdy string nie jest UUID → `MethodArgumentTypeMismatchException`.
- `PaymentExceptionHandler.java:59-64` — handler mapuje to na `400 validation`.
- **Co sprawdzić:** czy response zawiera `Content-Type: application/json` i `X-Correlation-ID`.

### 4.4 Route collision guardrail — `/summary` vs `/{paymentOrderId}`

```java
// To NIE powinno zwrócić pojedynczy payment order
given()
    .header("Authorization", "Bearer " + readerToken)
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
.then()
    .statusCode(200)
    .body("totalOrders", notNullValue())    // summary field
    .body("paymentOrderId", nullValue());   // NIE single-read field
```

- Spring MVC dopasowuje ścieżki **od najbardziej konkretnej do najmniej**.
- `/summary` jest **literalną** ścieżką — powinna wygrać z `/{paymentOrderId}` (wildcard).
- `SecurityConfig.java` explicitnie ma `/summary` **przed** `/*` — Spring Security też respektuje tę kolejność.
- **Test potwierdza:** response ma pola `totalOrders`, `byCurrency`, `byStatus` — a NIE `paymentOrderId`, `amountMinor`, `currency` (single read).

### 4.5 Conditional headers na endpointzie bez ETag

```java
given()
    .header("Authorization", "Bearer " + readerToken)
    .header("If-None-Match", "\"some-etag\"")
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
.then()
    .statusCode(200)          // normalna odpowiedź
    .header("ETag", nullValue());  // brak ETag
```

- Summary **nie ma** `ETag` (Lesson 08 §6 Headers: "Summary has no ETag").
- `If-None-Match` powinien być **ignorowany** — summary zawsze zwraca `200` z pełnym body.
- **Nie** powinno być `304 Not Modified` — summary nie wspiera cache'owania warunkowego.
- **Kontrast z Lesson 06:** `GET /{paymentOrderId}` zwraca `ETag` i mógłby wspierać `If-None-Match` / `304`.

### 4.6 Parameterized authorization matrix w REST Assured

```java
@ParameterizedTest(name = "[{index}] {0}")
@MethodSource("summaryAccessCases")
void summaryAccessMatrix(String displayName, Supplier<String> tokenSupplier,
                          String targetMerchantId, int expectedStatus) {
    String token = tokenSupplier != null ? tokenSupplier.get() : null;
    Response response = (token != null
        ? PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, token, "corr-l10-matrix")
        : MerchantApiTestSupport.publicRequest(port))
        .when()
        .get("/api/merchants/{merchantId}/payment-orders/summary", targetMerchantId);

    assertThat(response.statusCode()).isEqualTo(expectedStatus);

    if (expectedStatus == 200) {
        assertThat(response.contentType()).contains("application/json");
    }
}
```

## 5. Walkthrough — Od Niespodziewanego Accept do Asercji

```
1. Tworzę merchanta (lub używam istniejącego UUID dla 401/403 case'ów)
2. Tworzę token z odpowiednią rolą i merchant_id
3. Buduję request: .accept("text/html") zamiast .accept(ContentType.JSON)
4. Wysyłam GET /summary
5. Charakteryzuję response:
   - Jeśli 406: API prawidłowo odrzuca nieobsługiwany format
   - Jeśli 200: API ignoruje Accept i zwraca JSON — dokumentuję to jako "characterized, acceptable for now"
6. Sprawdzam, czy response.body() nie jest pusta i nie zawiera HTML
7. Zapisuję decyzję: czy to jest akceptowalne zachowanie Spring Boot default?
```

## 6. Learning Delta — Co Nowe vs Lessons 06-09

| Temat | Lesson 06-09 | Lesson 10 |
|---|---|---|
| Status codes | 200, 201, 400, 401, 403, 404, 409 | + `405 Method Not Allowed`, `406 Not Acceptable` (lub charakterystyka) |
| Request headers | `Authorization`, `Content-Type`, `Idempotency-Key`, `X-Correlation-ID` | + `Accept`, `If-None-Match` |
| Response headers | `Location`, `ETag`, `X-Correlation-ID`, `Content-Type` | asercja **braku** `ETag` przy conditional request |
| Path params | poprawne UUID | + malformed UUID (nie-UUID string) |
| Route matching | `/{paymentOrderId}` | świadomy test że `/summary` ≠ `/{paymentOrderId}` |
| Security tests | 7 ręcznych `@Test` | 1 `@ParameterizedTest` z 12+ wierszami danych |
| Error body | `error`, `message`, `correlationId` | + `details` (field-level errors dla malformed UUID) |
| Test assertions | happy path body assertions | + charakterystyka HTTP edge (dokumentowanie, nie tylko pass/fail) |

## 7. Typowe Błędy

1. **Zakładanie statusu 406 bez sprawdzenia.** Spring Boot domyślnie może NIE zwracać 406. Test musi najpierw scharakteryzować rzeczywiste zachowanie, potem zdecydować czy to bug czy feature.
2. **Zapominanie o auth dla unsupported methods.** `PUT /summary` bez tokena → `401`, nie `405`. Najpierw auth, potem method check.
3. **Używanie `containsString("UUID")` zamiast `containsStringIgnoringCase(...)`.** Komunikat błędu może być "UUID", "Uuid", "uuid".
4. **Route collision: test sprawdza tylko status 200.** Musi też sprawdzić **kształt body** — czy to summary shape, nie single-order shape.
5. **Conditional header: test tylko sprawdza brak ETag.** Musi też sprawdzić, że status to `200` (nie `304`), a body nie jest puste.
6. **Matrix test: za dużo logiki w jednym `@ParameterizedTest`.** Jeśli case'e różnią się setupem (tworzenie merchanta vs nie), rozbij na osobne testy.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Napisz test, który wysyła `Accept: text/html` do summary i dokumentuje wynik | 20 min |
| 2 | Wyślij `PUT`, `PATCH`, `DELETE` z poprawnym tokenem — co zwraca Spring? | 30 min |
| 3 | Wyślij `GET` z `merchantId="not-a-uuid"` — zweryfikuj `400` + `error=validation` | 15 min |
| 4 | Napisz test route collision: wyślij summary request i sprawdź, że body NIE zawiera `paymentOrderId` | 20 min |
| 5 | Wyślij `If-None-Match` do summary — sprawdź, że odpowiedź to `200` (nie `304`) i brak `ETag` | 20 min |
| 6 | Porównaj `415 Unsupported Media Type` vs `406 Not Acceptable` — który jest request, który response? | 15 min |
| 7 | Sparametryzuj security matrix: 10+ wierszy w jednym teście, każdy z nazwą wyświetlaną | 45 min |

## 9. Pytania

1. Dlaczego `Accept` i `Content-Type` to różne nagłówki? Który jest request, który response?
2. Kiedy API powinno zwrócić `415`, a kiedy `406`?
3. Dlaczego `405 Method Not Allowed` powinno zawierać nagłówek `Allow: GET`?
4. Jak `MethodArgumentTypeMismatchException` jest mapowane na `400 validation` w PaymentExceptionHandler?
5. Dlaczego `/summary` route musi być przed `/{paymentOrderId}` w Spring Security matchers?
6. Co by się stało, gdyby kolejność matcherów była odwrotna?
7. Dlaczego summary nie wspiera `304 Not Modified`?
8. Jak `@ParameterizedTest` z `@MethodSource` różni się od 7 osobnych `@Test` metod?
9. Kiedy warto sparametryzować test, a kiedy lepiej zostawić osobne metody?
10. Jak odróżnić `401` od `403` w odpowiedzi? Po czym poznać, że token jest nieważny vs brak uprawnień?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape` | route collision guardrail |
| `malformedMerchantIdReturnsValidationError` | UUID parsing → 400 |
| `unsupportedMethodsDoNotExposeSummaryMutationSurface` | PUT/PATCH/DELETE → 405 lub charakterystyka |
| `unsupportedAcceptIsRejectedOrExplicitlyCharacterized` | Accept: text/html → 406 lub dokumentacja |
| `ifNoneMatchDoesNotEnableSummaryCaching` | If-None-Match ignorowany, 200, brak ETag |
| `summaryAccessMatrixEnforcesAuthenticationAuthorizationAndOwnership` | 12+ wierszy parameterized |

## 11. Powiązane Notatki

- [[Lesson 08 - Aggregation Contract, Security, and Business Flow Tests]]
- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[13-22 Professional Practice After Refactoring]]
- [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Senior SDET Competency Coverage Matrix]]
