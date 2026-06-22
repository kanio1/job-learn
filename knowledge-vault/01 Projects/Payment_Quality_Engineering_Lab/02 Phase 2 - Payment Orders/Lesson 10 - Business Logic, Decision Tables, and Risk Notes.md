---
type: lesson
status: planned
area: Payment Quality Engineering Lab — Phase 2
lesson: 10
module: BOLA/BFLA Deep Dive, Authorization Policy, and HTTP Surface Hardening
date: 2026-05-31
tags:
  - business-logic
  - bola-bfla
  - authorization-policy
  - http-surface
  - risk-analysis
  - lesson-10
  - senior-sdet
---

# Lesson 10 — BOLA/BFLA Deep Dive, Authorization Policy, and HTTP Surface Hardening

> **Evidence link:** `PaymentOrderSummaryAuthorizationMatrixTest.java` (planned), `PaymentOrderSummaryHttpContractRestAssuredTest.java` (planned), `SecurityConfig.java`
>
> **Navigation:** [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]] | [[Lesson Evidence Tracker]] | [[Current Sprint]]

## 1. Cel Lekcji

Przejść od **ogólnych pojęć** BOLA/BFLA (Lesson 06) do **konkretnych, testowalnych case'ów** w macierzy authorization dla endpointu summary. Dodać świadome testowanie **czego API NIE robi** — unsupported methods, route collision, HTTP edge.

## 2. Prerequisites

- Lesson 06: idempotency, BOLA/BFLA koncepty, masked 404 vs overt 403.
- Lesson 07: list/filter security matrix.
- Lesson 08: summary security matrix (7 ręcznych testów).
- Lesson 09: frontend denied-state UI (UI hiding ≠ security).
- Role model: `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, `platform:payments:read`, `platform:merchants:*`.
- JWT claim `merchant_id`.

## 3. Code Reading Map

| Plik | Reguła biznesowa / decyzja |
|---|---|
| `SecurityConfig.java:39-41` | POST → create role, GET summary → read role, GET `/*` → read role |
| `PaymentOrderController.java:50-53` | create: `merchant_id` claim must match path `{merchantId}` |
| `PaymentOrderController.java:83-95` | single read: platform bypasses ownership, merchant masks 404 |
| `PaymentOrderController.java:122-130` | list: platform bypasses, merchant → `AccessDeniedException` (403) |
| `PaymentOrderController.java:154-162` | summary: platform bypasses, merchant → `AccessDeniedException` (403) |
| `KeycloakRealmRoleConverter.java:14-31` | realm roles bez prefixu → `platform:*`, z prefixem → zachowane |

## 4. Decision Table — Authorization Matrix (rozszerzona vs Lesson 08)

| # | Actor | Token / Role | merchant_id claim | Target | Expected | BOLA/BFLA |
|---|---|---|---|---|---|---|
| 1 | Unauthenticated | brak tokena | — | any | **401** | — |
| 2 | Invalid issuer | `invalidIssuerToken()` | — | any | **401** | — |
| 3 | Invalid signature | `platformOperatorToken() + "tampered"` | — | any | **401** | — |
| 4 | Expired token | `expiredToken()` | — | any | **401** | — |
| 5 | Denied identity | `deniedToken()` | — | any | **403** (no body) | BFLA |
| 6 | `merchant:payments:create` | creator token | matching | own | **403** (no body) | **BFLA** |
| 7 | `merchant:payments:operate` | operator token | matching | own | **403** (no body) | **BFLA** |
| 8 | `merchant:payments:read` | reader token | **brak claim** | any | **403** (body) | **BFLA** |
| 9 | `merchant:payments:read` | reader token | matching | own | **200** | — |
| 10 | `merchant:payments:read` | reader token | different | other | **403** (body) | **BOLA** |
| 11 | `platform:payments:read` | platform reader | — | selected | **200** | — |
| 12 | `platform:merchants:create` | platform merchant role | — | selected | **403** (no body) | **BFLA** |

**Nowe wiersze vs Lesson 08:**
- #2-4: invalid issuer / signature / expired token — brakowało w Lesson 08 security tests.
- #8: `merchant:payments:read` **bez** `merchant_id` claim — najciekawszy przypadek BFLA, nie testowany w Lesson 08.
- #12: platform nie-payment role — upewnia się, że `platform:merchants:*` nie daje dostępu do payment resources.

### 4.1 Dlaczego wiersz #8 jest krytyczny?

```java
// Token ma merchant:payments:read, ale NIE ma merchant_id claim
String token = TestJwtSupport.tokenWithRoles("merchant.reader.no.merchant",
    List.of("merchant:payments:read"));  // BEZ merchant_id!

// Kontroler:
String jwtMerchantId = jwt.getClaimAsString("merchant_id");  // → null
if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
    throw new AccessDeniedException("Merchant scope mismatch");  // → 403 z body
}
```

- `SecurityConfig` przepuszcza request (rola się zgadza).
- Kontroler sprawdza `merchant_id` claim → **brak** → `AccessDeniedException`.
- `PaymentExceptionHandler` mapuje to na `403 forbidden` z JSON body.
- To jest **BFLA**: właściwa funkcja (read), ale brak ownership claim.

### 4.2 Dlaczego #6 i #7 mają "no body" przy 403?

```java
// SecurityConfig: .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/summary")
//     .hasAnyAuthority("merchant:payments:read", "platform:payments:read")
```

- Creator ma tylko `merchant:payments:create` → nie pasuje do `hasAnyAuthority(...)`.
- Spring Security `AuthorizationFilter` odrzuca request **przed** kontrolerem.
- **Brak body**, brak `Content-Type`, brak `X-Correlation-ID` — to nie przechodzi przez aplikację.
- W przeciwieństwie do #10 (cross-tenant) gdzie kontroler sam rzuca `AccessDeniedException` → handler zwraca JSON body.

### 4.3 403 z body vs 403 bez body — decision table

| Przypadek | Kto odrzuca | Body? | Content-Type? | X-Correlation-ID? |
|---|---|---|---|---|
| Brak roli (BFLA #5-7, #12) | Spring Security filter | Nie | Nie | Nie |
| Brak `merchant_id` claim (#8) | Kontroler → handler | Tak | Tak | Tak |
| Cross-tenant (BOLA #10) | Kontroler → handler | Tak | Tak | Tak |

**Wniosek QA:** jak test widzi 403 **bez** body → to jest błąd konfiguracji security (rola). Jak test widzi 403 **z** body → to jest błąd biznesowy (ownership).

## 5. Decision Table — HTTP Surface Hardening

| Method | Endpoint | Expected | Dlaczego |
|---|---|---|---|
| GET | `/summary` | 200 (z auth) | Jedyna obsługiwana metoda |
| GET | `/summary` (bez auth) | 401 | Brak tokena |
| HEAD | `/summary` | 200 (bez body) lub 405 | Spring automatycznie wspiera HEAD dla GET |
| OPTIONS | `/summary` | 200 (Allow header) lub 405 | CORS/preflight |
| POST | `/summary` | 405 (lub 401 bez auth, potem 403/405) | Summary to read-only resource |
| PUT | `/summary` | 405 | Nie ma semantyki PUT dla agregacji |
| PATCH | `/summary` | 405 | Nie ma semantyki częściowej aktualizacji |
| DELETE | `/summary` | 405 | Nie usuwamy agregacji |

## 6. Decision Table — Route Ambiguity

| Ścieżka | Co Spring dopasowuje | Co zwraca |
|---|---|---|
| `/payment-orders/summary` | `@GetMapping("/summary")` | `PaymentOrderSummaryResponse` — `totalOrders`, `byCurrency`, `byStatus` |
| `/payment-orders/550e8400-e29b-...` | `@GetMapping("/{paymentOrderId}")` | `PaymentOrderResponse` — `paymentOrderId`, `amountMinor`, `currency`, `status` |

**Test:** wyślij GET `/payment-orders/summary` i sprawdź, że body zawiera `totalOrders`, a NIE `paymentOrderId`.

**Gdyby kolejność była odwrotna:** Spring dopasowałby `/summary` do `/{paymentOrderId}` → próba UUID.fromString("summary") → `MethodArgumentTypeMismatchException` → 400.

## 7. Risk Notes (QA Architecture)

### 7.1 BOLA — Broken Object Level Authorization (rozszerzone vs Lesson 06)

**Ryzyko:** Merchant reader z `merchant_id=A` czyta summary merchanta B.

**Mitigacja:** `PaymentOrderController.java:157-161` — sprawdzenie `merchant_id` claim. Platform reader bypassuje ten check (`isPlatformReader = true` → pomija).

**Nowe w Lesson 10:** Testujemy NIE tylko cross-tenant (jak Lesson 08), ale też **brak** `merchant_id` claim — scenariusz, gdzie token ma właściwą rolę, ale brakuje ownership context.

### 7.2 BFLA — Broken Function Level Authorization (rozszerzone vs Lesson 06)

**Ryzyko:** Token z rolą `merchant:payments:create` uzyskuje dostęp do summary (read).

**Mitigacja:** `SecurityConfig.java:40` — `/summary` explicitnie wymaga `merchant:payments:read` lub `platform:payments:read`.

**Nowe w Lesson 10:** Dodajemy test dla platform roli nie-paymentowych (`platform:merchants:*`), które NIE powinny dawać dostępu do payment resources.

### 7.3 Token claim integrity

**Ryzyko:** Token ma `merchant:payments:read`, ale brak `merchant_id` claim. Kontroler rzuca `AccessDeniedException`, ale czy na pewno?

**Nowe w Lesson 10:** Explicitny test — `TestJwtSupport.tokenWithRoles("subject", List.of("merchant:payments:read"))` **bez** `merchant_id`.

### 7.4 HTTP surface expansion (nowe ryzyko)

**Ryzyko:** Ktoś dodaje `@PostMapping("/summary")` w przyszłości, nieświadomie tworząc mutowalny endpoint summary.

**Mitigacja:** Test, który wysyła POST/PUT/PATCH/DELETE i oczekuje 405. Jeśli w przyszłości ktoś doda nową metodę do summary, test to wykryje.

### 7.5 Route collision regression (nowe ryzyko)

**Ryzyko:** Ktoś zmienia kolejność `@RequestMapping` lub `SecurityConfig` matchers.

**Mitigacja:** Test route collision — potwierdza, że `/summary` zwraca summary shape, nie single-order shape.

## 8. Learning Delta — Co Nowe vs Lesson 08 Business Logic

| Temat | Lesson 08 | Lesson 10 |
|---|---|---|
| BOLA case'y | cross-tenant → 403 | + brak `merchant_id` claim → 403 |
| BFLA case'y | creator/operator → 403 | + platform non-payment role → 403, invalid token → 401 |
| Token edge case'y | tylko poprawne tokeny | invalid issuer, invalid signature, expired |
| 403 z body vs bez body | dorozumiane | explicitna decision table |
| HTTP surface | nie testowane | metody, route collision, conditional headers |
| Risk notes | 5 ryzyk (BOLA, BFLA, validation drift, date edge, filter interaction) | + 2 ryzyka (HTTP surface expansion, route collision regression) |
| Decision tables | 3 tabele (security, date filter, filter interaction) | + 3 tabele (extended auth matrix, HTTP surface, route ambiguity, 403 body vs no body) |

## 9. Pytania

1. Dlaczego summary cross-tenant zwraca 403, a single-read cross-tenant maskuje jako 404?
2. Co się stanie, gdy token ma `merchant:payments:read`, ale NIE ma `merchant_id` claim?
3. Jaka jest różnica między 403 z body a 403 bez body? Który przypadek jest który?
4. Dlaczego `platform:merchants:create` NIE powinien dawać dostępu do summary?
5. Co by się stało, gdyby kolejność matcherów w SecurityConfig była odwrotna?
6. Dlaczego `PUT /summary` powinno zwracać 405, a nie 403?
7. Jak `KeycloakRealmRoleConverter` mapuje role na authorities?
8. Co to znaczy, że GET jest bezpieczny, a POST nie? Jak to się ma do summary?
9. Jak odróżnić bug w konfiguracji security od bugu w kontrolerze patrząc tylko na response body?
10. Dlaczego Lesson 10 nie dodaje nowych ról ani nowych endpointów?

### Odpowiedzi

1. Summary cross-tenant zwraca `403`, bo jest operacją kolekcji/raportu i jawnie odmawia zakresu. Single-read maskuje `404`, bo dotyczy konkretnego ID i ryzyka enumeracji.
2. SecurityConfig przepuści token z rolą read, ale kontroler odrzuci brak ownership context. Wynik to `403`, zwykle z body z handlera aplikacyjnego.
3. `403` bez body zwykle pochodzi z filtra Spring Security przed wejściem do kontrolera. `403` z body pochodzi z aplikacji, np. `AccessDeniedException` z kontrolera obsłużony przez handler.
4. `platform:merchants:create` dotyczy domeny merchant management, nie payment resources. Dostęp do summary wymaga `platform:payments:read`.
5. Odwrotna kolejność matcherów mogłaby sprawić, że bardziej ogólny matcher przechwyci `/summary`. Test route/security powinien wykryć zły status lub zły shape odpowiedzi.
6. `PUT /summary` powinien zwracać `405`, bo metoda nie jest obsługiwana dla read-only agregatu. `403` sugerowałoby, że metoda istnieje, ale caller nie ma prawa jej użyć.
7. Converter przenosi role z tokena na Spring Security authorities. Role platformowe mogą być mapowane bez prefiksu, a role merchantowe zachowują domenową nazwę authority.
8. GET jest safe, bo nie powinien modyfikować stanu systemu. Summary jest tylko odczytem agregacji, więc GET pasuje, a POST sugerowałby mutację.
9. Brak body przy `403` wskazuje na odrzucenie przez security filter. JSON body z error code wskazuje, że request doszedł do aplikacji i został odrzucony przez kontroler/handler.
10. Lesson 10 hartuje istniejący kontrakt zamiast rozszerzać produkt. Nowe role lub endpointy byłyby product behavior, a nie HTTP/security hardening.

## 10. Testy (Planned)

| Test | Co sprawdza |
|---|---|
| `summaryAccessMatrixEnforcesAuthenticationAuthorizationAndOwnership` | 12+ wierszy macierzy authorization |
| `merchantReaderWithoutMerchantIdClaimIsForbidden` | BFLA przez brak claim |
| `platformMerchantRoleCannotAccessPaymentSummary` | BFLA przez złą domenę ról |
| `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape` | route collision |
| `unsupportedMethodsDoNotExposeSummaryMutationSurface` | HTTP surface hardening |
| `malformedMerchantIdReturnsValidationError` | UUID parsing → 400 |
| `ifNoneMatchDoesNotEnableSummaryCaching` | conditional headers na resource bez ETag |
| `unsupportedAcceptIsRejectedOrExplicitlyCharacterized` | content negotiation |

## 11. Powiązane Notatki

- [[Lesson 08 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Senior SDET Competency Coverage Matrix]]
- [[Prompt - Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
