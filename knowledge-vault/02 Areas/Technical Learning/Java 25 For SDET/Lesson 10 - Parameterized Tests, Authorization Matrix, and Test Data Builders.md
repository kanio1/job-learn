---
type: lesson
status: planned
area: Java 25 For SDET
lesson: 10
module: Parameterized Tests, Authorization Matrix, and Test Data Builders
date: 2026-05-31
tags:
  - java-25
  - junit
  - parameterized-tests
  - authorization-matrix
  - records
  - lesson-10
  - senior-sdet
---

# Lesson 10 — Parameterized Tests, Authorization Matrix, and Test Data Builders

> **Evidence link:** `PaymentOrderSummaryAuthorizationMatrixTest.java` (planned), `PaymentOrderSummaryHttpContractRestAssuredTest.java` (planned)
>
> **Navigation:** [[Java 25 For SDET MOC]] | [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się używać JUnit `@ParameterizedTest` + `@MethodSource` do zastąpienia ręcznie pisanych testów security macierzą deklaratywną oraz używać Java `record` do modelowania test case'ów dla REST Assured zamiast luźnych Map/List.

Wszystko na realnym kodzie Lesson 10 — macierz authorization dla `GET /payment-orders/summary` oraz HTTP edge contract tests.

## 2. Prerequisites

- `@Test`, `@DisplayName` (Lesson 07).
- `TestJwtSupport` — token factory z `merchant_id` claim (Lesson 06).
- `PaymentOrderSummaryApiTestSupport` — request spec + seed data (Lesson 08).
- `assertThat(...)`, `extracting(...)` (Lesson 06-07).
- Enum `PaymentStatus`, `CurrencyCode` (Lesson 06).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `TestJwtSupport.java` | wszystkie warianty tokenów: `deniedToken()`, `tokenWithRoles(...)`, `merchantPaymentCreatorToken(...)`, `merchantPaymentReaderToken(...)`, `platformPaymentReaderToken(...)` |
| `PaymentOrderSummarySecurityTest.java` | istniejące ręczne testy — 7 metod, każda osobny `@Test`. To będzie punkt startowy do sparametryzowania |
| `PaymentOrderSummaryApiTestSupport.java` | `summaryReaderRequest(...)` — reusable request spec z tokenem i correlation ID |
| `MerchantApiTestSupport.java` | `publicRequest(...)`, `requestWithToken(...)` — bazowe request specs |

## 4. Kluczowe Pojęcia

### 4.1 Parameterized test — co to jest

```java
@ParameterizedTest(name = "[{index}] {0}")
@MethodSource("summaryAccessCases")
void summaryAccessMatrix(String displayName, String token, String targetMerchantId, int expectedStatus) {
    // ...
}
```

- `@ParameterizedTest` — uruchamia test wielokrotnie, za każdym razem z innymi parametrami.
- `@MethodSource("summaryAccessCases")` — wskazuje metodę, która zwraca kolekcję `Arguments`.
- `name = "[{index}] {0}"` — formatuje nazwę każdego uruchomienia (index + pierwszy argument).

### 4.2 MethodSource — fabryka test case'ów

```java
static Stream<Arguments> summaryAccessCases() {
    return Stream.of(
        Arguments.of("unauthenticated → 401", null, SOME_MERCHANT, 401),
        Arguments.of("denied → 403", TestJwtSupport.deniedToken(), SOME_MERCHANT, 403),
        Arguments.of("creator only → 403", creatorToken, ownMerchant, 403),
        Arguments.of("reader own → 200", readerToken, ownMerchant, 200),
        Arguments.of("reader other → 403", readerToken, otherMerchant, 403),
        Arguments.of("platform reader → 200", platformToken, selectedMerchant, 200)
    );
}
```

### 4.3 Record jako typ test case'a (alternatywnie)

```java
record SummaryAccessCase(
    String displayName,
    Supplier<String> tokenSupplier,
    String targetMerchantId,
    int expectedStatus,
    boolean isBola,
    boolean isBfla
) {}
```

- `Supplier<String>` — token tworzony **leniwie**, nie przy inicjalizacji streama (ważne gdy tokeny zależą od merchantId tworzonego per test).
- `isBola` / `isBfla` — metadane do dokumentowania, który przypadek to BOLA, a który BFLA.

### 4.4 Immutable test data

```java
List<SummaryAccessCase> cases = List.of(
    new SummaryAccessCase("unauthenticated", () -> null, null, 401, false, false),
    new SummaryAccessCase("denied identity", TestJwtSupport::deniedToken, null, 403, false, true),
    ...
);
```

- `List.of(...)` — niemutowalna lista, bezpieczna w testach równoległych.
- `TestJwtSupport::deniedToken` — method reference zamiast lambdy (krótsze).

### 4.5 Test data builders vs factory methods

Lesson 06-09 używa głównie **factory methods** (`TestJwtSupport.merchantPaymentReaderToken(...)`, `PaymentApiTestSupport.createActiveMerchant(...)`). W Lesson 10:

- Dla parametrized matrix — **factory methods są wystarczające** (jeden merchant per test case).
- Dla złożonych scenariuszy (wiele merchantów) — **builder pattern** byłby czytelniejszy, ale jest overkill dla obecnego scope.
- Świadoma decyzja: **nie implementujemy generycznego DSL**. Matrix ma być czytelny jak tabela, nie jak framework.

## 5. Walkthrough — Od Parameterized Test do REST Assured Assertion

```
1. @MethodSource tworzy Stream<Arguments>
2. JUnit pobiera kolejny zestaw argumentów
3. Test tworzy merchanta (jeśli potrzebny do case'a)
4. Tworzy token z odpowiednią rolą i merchant_id
5. Woła REST Assured:
   - authenticated: summaryReaderRequest(port, token, correlationId)
   - unauthenticated: MerchantApiTestSupport.publicRequest(port)
6. Sprawdza expectedStatus
7. Dla 200: asercja body (totalOrders, byCurrency itp.)
8. Dla 403: asercja body.error == "forbidden" LUB brak body (security filter)
9. Dla 401: tylko status code
```

## 6. Learning Delta — Co Nowe vs Lessons 06-09

| Temat | Lesson 06-09 | Lesson 10 |
|---|---|---|
| Token creation | ręczne per test (`merchantPaymentReaderToken(...)`) | fabryka w metodzie `@MethodSource` — jedna metoda karmi wszystkie case'y |
| Security tests | 7 osobnych `@Test` metod | 1 `@ParameterizedTest` + 12 wierszy danych |
| BOLA/BFLA rozróżnienie | dorozumiane w nazwach testów | jawne metadane w rekordzie test case'a |
| Subject/merchant_id | zawsze ustawiane | test case bez `merchant_id` claim — przypadek wymagany dla BFLA |
| `Supplier<T>` | nieużywane | używane do leniwego tworzenia tokenów |
| `List.of(...)` | używane od Lesson 05 | używane do deklaratywnej listy case'ów |
| `record` dla test data | `SeedOrder`, `ExpectedSummary` w support helperze (nie w teście) | `record` w teście do modelowania case'ów |

## 7. Typowe Błędy

1. **Token tworzony przed merchantem.** Jeśli token potrzebuje `merchantId`, a merchant jest tworzony w `@BeforeEach`, token musi być tworzony w teście, nie w `@MethodSource`.
2. **Brak rozróżnienia 403 z body vs 403 bez body.** Spring Security filter odrzuca przed kontrolerem (brak body, brak `Content-Type`). Kontroler rzuca `AccessDeniedException` → handler zwraca JSON body. Test musi wiedzieć, który przypadek jest który.
3. **`@MethodSource` zwraca `null` zamiast `Stream.empty()`.** Dla 0 przypadków zwróć `Stream.empty()` zamiast `null`.
4. **Nazwy case'ów są za długie lub nieczytelne.** Używaj formatu `"unauthenticated -> 401"`, nie `"testCaseNumber7AuthenticationFailure"`.
5. **Token `deniedToken()` ma role, ale puste.** `TestJwtSupport.deniedToken()` tworzy token z `List.of()` — ma poprawnego issuera i sygnaturę, ale zero ról. Spring Security widzi brak autoryzacji → 403.
6. **Zapominanie o `merchant_id` claim.** Token bez `merchant_id` + `merchant:payments:read` rola → kontroler sprawdza `jwt.getClaimAsString("merchant_id")` → `null` → `AccessDeniedException`.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Przekonwertuj istniejący `PaymentOrderSummarySecurityTest` z 7 ręcznych `@Test` na 1 `@ParameterizedTest` z `@MethodSource` | 45 min |
| 2 | Dodaj wiersz dla tokenu `merchant:payments:read` **bez** `merchant_id` claim | 15 min |
| 3 | Dodaj wiersz dla tokenu `platform:merchants:create` (platform merchant role) — powinien dostać 403 | 15 min |
| 4 | Zastąp `Arguments.of(...)` rekordem `SummaryAccessCase` z polem `isBola` — wypisz w `name` czy to BOLA czy BFLA | 30 min |
| 5 | Napisz test, gdzie 3 różne tokeny wołają summary tego samego merchanta i tylko 2 dostają 200 | 20 min |

## 9. Pytania

1. Dlaczego `@MethodSource` zwraca `Stream<Arguments>`, a nie `List<Arguments>`?
2. Co się stanie, gdy `tokenSupplier.get()` rzuci wyjątkiem? Gdzie ten błąd będzie widoczny?
3. Jak `@ParameterizedTest` współpracuje z `@BeforeEach`? Ile razy `@BeforeEach` jest wywoływane?
4. Dlaczego `record` dla test case'a jest lepszy niż `Arguments.of(...)`?
5. Kiedy `Supplier<String>` jest konieczny, a kiedy można użyć zwykłego `String`?
6. Jak odróżnić 403 z body od 403 bez body w sparametryzowanym teście?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `summaryAccessMatrixEnforcesAuthenticationAuthorizationAndOwnership` | 12+ wierszy: 401, 403 (BFLA), 403 (BOLA), 200 (own/platform) |
| `merchantReaderWithoutMerchantIdClaimIsForbidden` | BFLA — rola read bez ownership claim |
| `platformMerchantRoleCannotAccessPaymentSummary` | platform:merchants:create nie daje dostępu do payment resource |

## 11. Powiązane Notatki

- [[Lesson 08 - Java Records, Read-Only Services, and Input Validation]]
- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Senior SDET Competency Coverage Matrix]]
- [[REST Assured from Zero to Professional Backend API Testing]]
