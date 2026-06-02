---
type: lesson
status: planned
area: Java 25 For SDET
lesson: 11
module: Sealed Types, Defensive Copies, and Comparators
date: 2026-05-31
tags:
  - java-25
  - sealed-types
  - defensive-copies
  - comparators
  - lesson-11
  - senior-sdet
---

# Lesson 11 — Sealed Types, Defensive Copies, and Comparators

> **Evidence link:** `PaymentOrderBuilder.java` (planned), `PaymentOrderApi.java` (planned)
>
> **Navigation:** [[Java 25 For SDET MOC]] | [[Lesson 11 - REST Assured Framework Architecture and Test Organization]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się zaawansowanych Java 25 features w kontekście test framework architecture:
- **sealed interface** dla test data hierarchies (closed polymorphism)
- **Map.copyOf / List.copyOf** dla defensive copies (immutability)
- **Comparator.comparing / thenComparing** dla test data ordering
- **Method references** w assertions (czytelność)

Wszystko na realnym kodzie Lesson 11 — API clients, test data builders, scenario flows.

## 2. Prerequisites

- Java `record` składnia (Lesson 06-08).
- `Map.of()` i `List.of()` (Lesson 05).
- Podstawy `Comparator` (Lesson 07 list sorting).
- Lambda expressions i method references (Lesson 06-07).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `PaymentOrderBuilder.java` | Builder pattern zwracający immutable `CreatePaymentOrderRequest` record |
| `PaymentOrderApi.java` | API client wrapper z typed responses |
| `PaymentOrderRestAssuredTest.java` | Użycie `Comparator.comparing()` w test data assertions |
| `PaymentOrderListApiTestSupport.java` | Obecne `Map<String, Object>` → target: sealed interface hierarchy |

## 4. Kluczowe Pojęcia

### 4.1 sealed interface — Closed Polymorphism

```java
public sealed interface PaymentTestData 
    permits ValidPaymentOrder, InvalidPaymentOrder, EdgeCasePaymentOrder {
    
    record ValidPaymentOrder(long amountMinor, String currency, String reference) 
        implements PaymentTestData {}
    
    record InvalidPaymentOrder(String reason, Map<String, Object> payload) 
        implements PaymentTestData {}
    
    record EdgeCasePaymentOrder(String scenario, long amountMinor, String currency) 
        implements PaymentTestData {}
}
```

- `sealed` — kompilator wymusza, że tylko wymienione typy mogą implementować interface.
- `permits` — jawna lista dozwolonych implementacji.
- **Dlaczego sealed?** Test data ma skończoną liczbę wariantów (valid, invalid, edge case). Sealed interface dokumentuje to w typie.
- **Kiedy używać?** Gdy test data ma naturalną hierarchię i chcesz, aby compiler pilnował completeness.

### 4.2 Map.copyOf / List.copyOf — Defensive Copies

```java
// PRZED (mutable):
Map<String, Object> body = new LinkedHashMap<>();
body.put("amountMinor", 1000);
body.put("currency", "PLN");
return body; // Caller może zmodyfikować!

// PO (immutable defensive copy):
Map<String, Object> body = new LinkedHashMap<>();
body.put("amountMinor", 1000);
body.put("currency", "PLN");
return Map.copyOf(body); // Immutable! Próba modyfikacji → UnsupportedOperationException
```

- `Map.copyOf(map)` — tworzy niemutowalną kopię. Zmiany w oryginale nie wpływają na kopię.
- `List.copyOf(list)` — to samo dla list.
- **Różnica od `Map.of(...)`:** `Map.of()` tworzy mapę od zera, `Map.copyOf()` kopiuje istniejącą.
- **Kiedy używać?** Gdy budujesz mapę dynamicznie (np. w builderze) i chcesz zwrócić immutable result.

### 4.3 Comparator.comparing / thenComparing

```java
// Sortowanie test data po currency, potem po amount:
List<PaymentOrderResponse> sorted = orders.stream()
    .sorted(Comparator.comparing(PaymentOrderResponse::currency)
                      .thenComparing(PaymentOrderResponse::amountMinor))
    .toList();

// AssertJ z custom comparator:
assertThat(orders)
    .usingElementComparator(Comparator.comparing(PaymentOrderResponse::paymentOrderId))
    .containsExactly(expectedOrder1, expectedOrder2);
```

- `Comparator.comparing(keyExtractor)` — tworzy comparator na podstawie key extractor.
- `thenComparing(secondKeyExtractor)` — dodaje secondary sort key.
- **Dlaczego lepsze niż ręczne sortowanie?** Czytelne, type-safe, composable.

### 4.4 Method References w Assertions

```java
// PRZED (lambda):
assertThat(orders).extracting(o -> o.currency()).containsExactly("PLN", "EUR", "USD");

// PO (method reference):
assertThat(orders).extracting(PaymentOrderResponse::currency).containsExactly("PLN", "EUR", "USD");
```

- `PaymentOrderResponse::currency` — method reference do gettera.
- **Dlaczego lepsze?** Krótsze, czytelniejsze, mniej noise.
- **Kiedy lambda?** Gdy potrzebujesz złożonej logiki (np. `o -> o.amountMinor() * 100`).

## 5. Walkthrough — Od Builder Do Immutable Request

```
1. Test wywołuje: aPaymentOrder().withAmountMinor(5000).withCurrency("EUR").build()
2. Builder accumulates state w mutable fields (amountMinor, currency, reference)
3. build() tworzy CreatePaymentOrderRequest record (immutable)
4. Record jest przekazywany do API client
5. API client serializuje record do JSON (Jackson)
6. REST Assured wysyła JSON do backend
7. Backend deserializuje JSON do CreatePaymentOrderRequest record
8. Service waliduje i przetwarza request
```

## 6. Learning Delta — Co Nowe vs Lessons 06-10

| Temat | Lesson 06-10 | Lesson 11 |
|---|---|---|
| Test data types | `Map<String, Object>` | sealed interface + records |
| Immutability | `Map.of()` (static) | `Map.copyOf()` (dynamic defensive copy) |
| Sorting | `ORDER BY` w SQL | `Comparator.comparing()` w Java |
| Method references | Sporadycznie | Systematycznie w assertions |
| Type hierarchies | Brak | sealed interface dla test data variants |

## 7. Typowe Błędy

1. **Nadużywanie sealed interface.** Sealed jest dobre dla zamkniętych hierarchii. Jeśli test data ma wiele wariantów, zwykły interface + records mogą być prostsze.
2. **Zapominanie o `Map.copyOf()`.** Jeśli builder zwraca mutable map, caller może ją zmodyfikować → flaky tests.
3. **Ręczne sortowanie zamiast Comparator.** `Collections.sort(list, (a, b) -> ...)` jest mniej czytelne niż `Comparator.comparing(...)`.
4. **Lambda zamiast method reference.** `o -> o.getCurrency()` jest verbose. `PaymentOrderResponse::currency` jest concise.
5. **Sealed interface bez permits.** Compiler wymaga jawnej listy `permits`. Bez tego → compilation error.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Zdefiniuj sealed interface `TestData` z 3 wariantami (valid, invalid, edge) | 20 min |
| 2 | Zrefaktoruj `createPaymentOrderBody(...)` aby zwracał `Map.copyOf(...)` | 15 min |
| 3 | Posortuj listę payment orders po currency (ASC) potem po amount (DESC) | 20 min |
| 4 | Zastąp 3 lambda expressions method references w istniejących testach | 15 min |
| 5 | Wyjaśnij różnicę między `Map.of()` a `Map.copyOf()` | 10 min |

## 9. Pytania

1. Kiedy sealed interface jest lepszy niż zwykły interface?
2. Dlaczego `Map.copyOf()` jest ważne w test data builders?
3. Jak `Comparator.comparing().thenComparing()` różni się od ręcznego sortowania?
4. Kiedy używać method reference zamiast lambda?
5. Czy sealed interface może mieć default methods?
6. Czy `List.copyOf()` zachowuje kolejność elementów?
7. Jak `Comparator.nullsFirst()` / `nullsLast()` wpływają na sortowanie?
8. Dlaczego method references są type-safe?
9. Czy sealed interface może być zagnieżdżony (nested)?
10. Jak sealed interface współpracuje z pattern matching (future Java)?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `sealedInterfacePermitsOnlyDeclaredTypes` | Compiler enforcement |
| `mapCopyOfCreatesImmutableCopy` | `UnsupportedOperationException` on modification |
| `comparatorSortsByCurrencyThenAmount` | Multi-key sorting |
| `methodReferenceExtractsField` | `extracting(Class::method)` works |

## 11. Powiązane Notatki

- [[Lesson 08 - Java Records, Read-Only Services, and Input Validation]]
- [[Lesson 07 - Payment Order List Filter Search]]
- [[Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [[Senior SDET Competency Coverage Matrix]]
