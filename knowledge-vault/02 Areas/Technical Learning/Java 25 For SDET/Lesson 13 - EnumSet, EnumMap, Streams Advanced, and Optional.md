---
type: lesson
status: planned
area: Java 25 For SDET
lesson: 13
module: EnumSet, EnumMap, Streams Advanced, and Optional
date: 2026-05-31
tags:
  - java-25
  - enumset
  - enummap
  - streams
  - optional
  - lesson-13
  - senior-sdet
---

# Lesson 13 — EnumSet, EnumMap, Streams Advanced, and Optional

> **Evidence link:** `PaymentOrderParallelTest.java` (planned), `PaymentOrderTransactionTest.java` (planned)
>
> **Navigation:** [[Java 25 For SDET MOC]] | [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się zaawansowanych Java 25 collections i functional patterns:
- **EnumSet** — efficient set dla enums (bit vector implementation)
- **EnumMap** — efficient map dla enum keys (array implementation)
- **Streams advanced** — groupingBy, partitioningBy, downstream collectors
- **Optional<T>** — proper usage w return types i test assertions

## 2. Prerequisites

- Java enums (Lesson 06).
- Basic collections (List, Set, Map) — Lesson 05.
- Basic streams (filter, map, collect) — Lesson 07.
- Optional basics (Lesson 12).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `PaymentStatus.java` | Enum dla payment statuses |
| `CurrencyCode.java` | Enum dla currencies |
| `PaymentOrderParallelTest.java` | EnumSet dla allowed statuses |
| `PaymentOrderTransactionTest.java` | EnumMap dla status counts |

## 4. Kluczowe Pojęcia

### 4.1 EnumSet — Efficient Set dla Enums

```java
// EnumSet: bit vector implementation (memory efficient, fast)
EnumSet<PaymentStatus> allowedStatuses = EnumSet.of(
    PaymentStatus.CREATED, 
    PaymentStatus.AUTHORIZED
);

EnumSet<PaymentStatus> allStatuses = EnumSet.allOf(PaymentStatus.class);

EnumSet<PaymentStatus> noStatuses = EnumSet.noneOf(PaymentStatus.class);

EnumSet<PaymentStatus> range = EnumSet.range(
    PaymentStatus.CREATED, 
    PaymentStatus.CAPTURED
);

EnumSet<PaymentStatus> complement = EnumSet.complementOf(allowedStatuses);

// Dlaczego EnumSet?
// - Memory efficient: bit vector (1 bit per enum value)
// - Fast: O(1) operations (bitwise AND/OR)
// - Type-safe: compiler sprawdza enum type
// - Immutable: nie można dodać/usunąć elements po creation

// Porównanie z HashSet:
// HashSet<PaymentStatus>: 16 bytes per element + hash table overhead
// EnumSet<PaymentStatus>: 1 long (8 bytes) dla <= 64 enum values
```

**Kiedy używać EnumSet?**
- Gdy masz set of enum values (np. allowed statuses, supported currencies)
- Gdy potrzebujesz fast membership checks (`contains()`)
- Gdy chcesz type-safe set (compiler sprawdza enum type)
- **Nie używaj:** Gdy potrzebujesz mutable set (EnumSet jest immutable po creation)

### 4.2 EnumMap — Efficient Map dla Enum Keys

```java
// EnumMap: array implementation (memory efficient, fast)
EnumMap<PaymentStatus, Long> statusCounts = new EnumMap<>(PaymentStatus.class);
statusCounts.put(PaymentStatus.CREATED, 10L);
statusCounts.put(PaymentStatus.AUTHORIZED, 5L);
statusCounts.put(PaymentStatus.CAPTURED, 3L);

Long createdCount = statusCounts.get(PaymentStatus.CREATED);  // 10L

// Dlaczego EnumMap?
// - Memory efficient: array (1 slot per enum value)
// - Fast: O(1) operations (array indexing)
// - Type-safe: compiler sprawdza enum type
// - Ordered: iteration order = enum declaration order

// Porównanie z HashMap:
// HashMap<PaymentStatus, Long>: 32 bytes per entry + hash table overhead
// EnumMap<PaymentStatus, Long>: 8 bytes per entry (array slot)
```

**Kiedy używać EnumMap?**
- Gdy masz map z enum keys (np. status → count, currency → total amount)
- Gdy potrzebujesz fast lookups (`get()`)
- Gdy chcesz type-safe map (compiler sprawdza enum type)
- **Nie używaj:** Gdy keys nie są enums (użyj HashMap)

### 4.3 Streams Advanced: groupingBy

```java
// groupingBy: group elements by key
Map<String, List<PaymentOrderResponse>> ordersByCurrency = orders.stream()
    .collect(Collectors.groupingBy(PaymentOrderResponse::currency));

// Result:
// {
//   "PLN": [order1, order2, order3],
//   "EUR": [order4, order5],
//   "USD": [order6]
// }

// groupingBy z downstream collector:
Map<String, Long> countByCurrency = orders.stream()
    .collect(Collectors.groupingBy(
        PaymentOrderResponse::currency,
        Collectors.counting()
    ));

// Result:
// {
//   "PLN": 3,
//   "EUR": 2,
//   "USD": 1
// }

Map<String, Long> totalAmountByCurrency = orders.stream()
    .collect(Collectors.groupingBy(
        PaymentOrderResponse::currency,
        Collectors.summingLong(PaymentOrderResponse::amountMinor)
    ));

// Result:
// {
//   "PLN": 15000,
//   "EUR": 8000,
//   "USD": 5000
// }

// groupingBy z multiple levels:
Map<String, Map<String, Long>> countByCurrencyAndStatus = orders.stream()
    .collect(Collectors.groupingBy(
        PaymentOrderResponse::currency,
        Collectors.groupingBy(
            PaymentOrderResponse::status,
            Collectors.counting()
        )
    ));

// Result:
// {
//   "PLN": {"CREATED": 2, "AUTHORIZED": 1},
//   "EUR": {"CREATED": 1, "CAPTURED": 1},
//   "USD": {"CREATED": 1}
// }
```

**Kiedy używać groupingBy?**
- Gdy chcesz group elements by key (np. orders by currency, orders by status)
- Gdy chcesz aggregate per group (count, sum, average)
- Gdy chcesz nested grouping (np. by currency then by status)

### 4.4 Streams Advanced: partitioningBy

```java
// partitioningBy: split into two groups (predicate true/false)
Map<Boolean, List<PaymentOrderResponse>> partitionedByAmount = orders.stream()
    .collect(Collectors.partitioningBy(order -> order.amountMinor() > 5000));

List<PaymentOrderResponse> highAmount = partitionedByAmount.get(true);
List<PaymentOrderResponse> lowAmount = partitionedByAmount.get(false);

// Result:
// {
//   true: [order1 (6000), order2 (7000)],
//   false: [order3 (3000), order4 (4000)]
// }

// partitioningBy z downstream collector:
Map<Boolean, Long> countByAmount = orders.stream()
    .collect(Collectors.partitioningBy(
        order -> order.amountMinor() > 5000,
        Collectors.counting()
    ));

// Result:
// {
//   true: 2,
//   false: 2
// }
```

**Kiedy używać partitioningBy?**
- Gdy chcesz split elements into two groups (true/false)
- Gdy masz binary predicate (np. amount > 5000, status == "CREATED")
- **Różnica od groupingBy:** partitioningBy zawsze zwraca 2 groups (true/false), groupingBy może zwrócić wiele groups

### 4.5 Optional<T> — Proper Usage

```java
// Optional w return types:
public Optional<PaymentOrderResponse> getOrder(UUID orderId) {
    return orderRepository.findById(orderId)
        .map(PaymentOrderMapper::toResponse);
}

// Optional w test assertions:
Optional<PaymentOrderResponse> order = paymentApi.getOrder(orderId);

// AssertJ:
assertThat(order).isPresent();
assertThat(order).isPresent().get().extracting(PaymentOrderResponse::amountMinor).isEqualTo(5000);

// JUnit:
assertTrue(order.isPresent(), "Expected payment order but was empty");
assertEquals(5000, order.get().amountMinor());

// Optional w conditional logic:
order.ifPresent(o -> {
    assertThat(o.amountMinor()).isPositive();
    assertThat(o.currency()).isEqualTo("PLN");
});

// Optional z default value:
PaymentOrderResponse order = paymentApi.getOrder(orderId)
    .orElseThrow(() -> new AssertionError("Expected payment order but was empty"));

// Optional z fallback:
PaymentOrderResponse order = paymentApi.getOrder(orderId)
    .orElse(defaultOrder);
```

**Kiedy używać Optional?**
- **Return types:** Gdy metoda może nie znaleźć rezultatu (np. `findById`, `getOrder`)
- **Test assertions:** Gdy chcesz explicit assert że value jest obecne
- **Conditional logic:** Gdy chcesz wykonać akcję tylko jeśli value jest obecne

**Kiedy NIE używać Optional?**
- **Parameters:** `void method(Optional<String> param)` — anti-pattern. Użyj overloaded methods: `void method()` i `void method(String param)`
- **Fields:** `private Optional<String> field` — anti-pattern. Użyj null i sprawdzaj w constructorze
- **Collections:** `Optional<List<T>>` — anti-pattern. Zwróć empty list zamiast Optional

## 5. Walkthrough — Od HashSet Do EnumSet

```
PRZED (HashSet):
1. Tworzysz HashSet<PaymentStatus>
2. Dodajesz elements: set.add(PaymentStatus.CREATED)
3. HashSet używa hash table (16 bytes per element + overhead)
4. contains() używa hashCode() + equals() (wolne)

PO (EnumSet):
1. Tworzysz EnumSet<PaymentStatus>
2. Dodajesz elements: EnumSet.of(PaymentStatus.CREATED, PaymentStatus.AUTHORIZED)
3. EnumSet używa bit vector (1 bit per enum value)
4. contains() używa bitwise AND (szybkie)
```

## 6. Learning Delta — Co Nowe vs Lessons 06-12

| Temat | Lesson 06-12 | Lesson 13 |
|---|---|---|
| Enum collections | Brak | EnumSet, EnumMap |
| Streams | filter, map, collect | groupingBy, partitioningBy, downstream collectors |
| Optional | Basic (Lesson 12) | Proper usage w return types, test assertions |

## 7. Typowe Błędy

1. **EnumSet dla mutable sets.** EnumSet jest immutable po creation. Jeśli potrzebujesz mutable set, użyj `EnumSet.copyOf()` aby stworzyć mutable copy.
2. **EnumMap z null keys.** EnumMap nie akceptuje null keys (rzuca NullPointerException). Sprawdź key przed `put()`.
3. **groupingBy bez downstream collector.** `groupingBy(keyExtractor)` zwraca `Map<K, List<T>>`. Jeśli chcesz count/sum, użyj downstream collector: `groupingBy(keyExtractor, counting())`.
4. **partitioningBy dla non-binary predicates.** partitioningBy wymaga binary predicate (true/false). Jeśli masz wiele categories, użyj groupingBy.
5. **Optional dla parameters.** `void method(Optional<String> param)` jest anti-pattern. Użyj overloaded methods.
6. **Optional dla fields.** `private Optional<String> field` jest anti-pattern. Użyj null i sprawdzaj w constructorze.
7. **Optional.get() bez isPresent().** `optional.get()` rzuca NoSuchElementException jeśli empty. Zawsze sprawdź `isPresent()` lub użyj `orElse()`, `orElseThrow()`.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Napisz EnumSet dla allowed payment statuses | 15 min |
| 2 | Napisz EnumMap dla status counts | 15 min |
| 3 | Porównaj memory usage: HashSet vs EnumSet (1000 elements) | 20 min |
| 4 | Napisz groupingBy dla orders by currency | 20 min |
| 5 | Napisz groupingBy z downstream collector (counting) | 20 min |
| 6 | Napisz partitioningBy dla high/low amount orders | 15 min |
| 7 | Napisz test z Optional w return type | 20 min |
| 8 | Napisz test z AssertJ Optional assertions | 15 min |

## 9. Pytania

1. Dlaczego EnumSet jest bardziej memory efficient niż HashSet?
2. Dlaczego EnumMap jest bardziej memory efficient niż HashMap?
3. Jaka jest różnica między groupingBy a partitioningBy?
4. Kiedy używać downstream collectors (counting, summingLong)?
5. Dlaczego Optional nie powinno być używane dla parameters?
6. Dlaczego Optional nie powinno być używane dla fields?
7. Jak AssertJ wspiera Optional assertions?
8. Czy EnumSet może być mutable?
9. Czy EnumMap akceptuje null keys?
10. Jak zaimplementować nested groupingBy (by currency then by status)?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `enumSetContainsAllowedStatuses` | EnumSet.of() works |
| `enumMapStoresStatusCounts` | EnumMap.put() works |
| `groupingByGroupsOrdersByCurrency` | Collectors.groupingBy works |
| `groupingByWithCountingCountsPerGroup` | Downstream collector works |
| `partitioningBySplitsOrdersByAmount` | Collectors.partitioningBy works |
| `optionalIsPresentWhenOrderExists` | Optional.isPresent() works |
| `optionalIsEmptyWhenOrderNotFound` | Optional.isEmpty() works |

## 11. Powiązane Notatki

- [[Lesson 08 - Java Records, Read-Only Services, and Input Validation]]
- [[Lesson 11 - Sealed Types, Defensive Copies, and Comparators]]
- [[Lesson 12 - Generics, Pattern Matching, and Text Blocks]]
- [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]]
- [[Senior SDET Competency Coverage Matrix]]
