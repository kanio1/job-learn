---
type: lesson
status: planned
area: Java 25 For SDET
lesson: 12
module: Generics, Pattern Matching, and Text Blocks
date: 2026-05-31
tags:
  - java-25
  - generics
  - pattern-matching
  - text-blocks
  - lesson-12
  - senior-sdet
---

# Lesson 12 — Generics, Pattern Matching, and Text Blocks

> **Evidence link:** `PaymentOrderParameterizedTest.java` (planned), `PaymentOrderJsonFixtures.java` (planned)
>
> **Navigation:** [[Java 25 For SDET MOC]] | [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się zaawansowanych Java 25 features w kontekście test automation:
- **Generics** (bounded wildcards, PECS principle)
- **Pattern matching instanceof** (JDK 16+)
- **Text blocks** (multi-line strings dla JSON fixtures)
- **Optional<T>** w return types

Wszystko na realnym kodzie Lesson 12 — parameterized tests, JSON fixtures, type-safe helpers.

## 2. Prerequisites

- Java generics basics (`List<T>`, `Map<K, V>`) — Lesson 05.
- Lambda expressions i method references — Lesson 06-07.
- `instanceof` operator — podstawy Java.
- String concatenation — podstawy Java.

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `PaymentOrderParameterizedTest.java` | @ParameterizedTest z generics helpers |
| `PaymentOrderJsonFixtures.java` | Text blocks dla JSON test data |
| `PaymentOrderAssertions.java` | Custom AbstractAssert z pattern matching |
| `PaymentOrderApi.java` | API client z Optional<T> return types |

## 4. Kluczowe Pojęcia

### 4.1 Generics: Bounded Wildcards (PECS)

```java
// PECS: Producer Extends, Consumer Super

// Producer (wyciągasz dane z kolekcji):
public static <T extends PaymentOrderResponse> void assertAllCreated(List<T> orders) {
    assertThat(orders).allMatch(o -> o.status().equals("CREATED"));
}

// Consumer (wkładasz dane do kolekcji):
public static <T super CreatePaymentOrderRequest> void addDefaultOrder(List<T> requests) {
    requests.add(new CreatePaymentOrderRequest(1000, "PLN", "DEFAULT"));
}

// Dlaczego PECS?
// - `List<? extends T>` — możesz czytać T, ale nie możesz dodawać (nie wiesz jaki podtyp)
// - `List<? super T>` — możesz dodawać T, ale nie możesz czytać (nie wiesz jaki nadtyp)
```

**Kiedy używać bounded wildcards?**
- **Producer Extends:** Gdy metoda czyta dane z kolekcji i przetwarza je (np. `assertAllCreated(List<? extends PaymentOrderResponse>)`)
- **Consumer Super:** Gdy metoda dodaje dane do kolekcji (np. `addDefaultOrder(List<? super CreatePaymentOrderRequest>)`)
- **Bez wildcards:** Gdy metoda zarówno czyta jak i dodaje (np. `sort(List<T>)`)

### 4.2 Pattern Matching instanceof (JDK 16+)

```java
// PRZED (explicit casting):
if (response instanceof PaymentOrderResponse) {
    PaymentOrderResponse order = (PaymentOrderResponse) response;
    assertThat(order.amountMinor()).isPositive();
}

// PO (pattern matching):
if (response instanceof PaymentOrderResponse order) {
    assertThat(order.amountMinor()).isPositive();
}

// W switch (JDK 21+):
String describe(Object obj) {
    return switch (obj) {
        case PaymentOrderResponse order -> "Payment order: " + order.paymentOrderId();
        case PaymentOrderListResponse list -> "Payment list: " + list.totalElements();
        case PaymentErrorResponse error -> "Error: " + error.error();
        default -> "Unknown: " + obj.getClass().getSimpleName();
    };
}
```

**Dlaczego pattern matching?**
- Eliminuje explicit casting (mniej boilerplate)
- Type-safe (compiler sprawdza typ)
- Czytelne (zmienna `order` jest od razu dostępna)
- W switch: exhaustive (compiler wymaga default lub wszystkich cases)

### 4.3 Text Blocks (JDK 15+)

```java
// PRZED (string concatenation):
String json = "{\n" +
    "  \"amountMinor\": 5000,\n" +
    "  \"currency\": \"PLN\",\n" +
    "  \"clientOrderReference\": \"TEST-001\"\n" +
    "}";

// PO (text block):
String json = """
    {
        "amountMinor": 5000,
        "currency": "PLN",
        "clientOrderReference": "TEST-001"
    }
    """;
```

**Dlaczego text blocks?**
- Czytelność (multi-line strings wyglądają jak actual JSON)
- Brak escape sequences (`\"` → `"`)
- Automatic indentation (compiler usuwa common leading whitespace)
- Idealne dla JSON fixtures, SQL queries, HTML templates

**Text blocks w test fixtures:**
```java
public final class PaymentOrderJsonFixtures {
    
    public static final String VALID_PAYMENT_ORDER_REQUEST = """
        {
            "amountMinor": 5000,
            "currency": "PLN",
            "clientOrderReference": "TEST-001"
        }
        """;
    
    public static final String INVALID_PAYMENT_ORDER_REQUEST_NEGATIVE_AMOUNT = """
        {
            "amountMinor": -100,
            "currency": "PLN",
            "clientOrderReference": "TEST-002"
        }
        """;
    
    public static final String INVALID_PAYMENT_ORDER_REQUEST_UNKNOWN_CURRENCY = """
        {
            "amountMinor": 5000,
            "currency": "GBP",
            "clientOrderReference": "TEST-003"
        }
        """;
}
```

### 4.4 Optional<T> w Return Types

```java
// PRZED (null checks):
PaymentOrderResponse order = paymentApi.getOrder(merchantId, orderId, token);
if (order != null) {
    assertThat(order.amountMinor()).isPositive();
}

// PO (Optional):
Optional<PaymentOrderResponse> order = paymentApi.getOrderOptional(merchantId, orderId, token);
order.ifPresent(o -> assertThat(o.amountMinor()).isPositive());

// Lub z AssertJ:
assertThat(order).isPresent().get().extracting(PaymentOrderResponse::amountMinor).isEqualTo(5000);

// Lub z orElseThrow:
PaymentOrderResponse order = paymentApi.getOrderOptional(merchantId, orderId, token)
    .orElseThrow(() -> new AssertionError("Expected payment order but was empty"));
```

**Dlaczego Optional?**
- Explicit (widać że metoda może zwrócić empty)
- Type-safe (compiler wymusza obsługę empty case)
- Fluent API (`ifPresent`, `orElse`, `orElseThrow`)
- AssertJ wspiera Optional (`assertThat(optional).isPresent()`)

**Kiedy używać Optional?**
- Return types (gdy metoda może nie znaleźć rezultatu)
- **NIE** dla parameters (użyj overloaded methods zamiast tego)
- **NIE** dla fields (użyj null checks w constructorze)

## 5. Walkthrough — Od String Concatenation Do Text Block

```
PRZED:
1. Test definiuje JSON jako string concatenation
2. Każdy line wymaga `\n` i `+`
3. Każdy field wymaga escape `\"`
4. Indentation jest manual (spacje w string)
5. Trudne do czytania i maintainowania

PO:
1. Test definiuje JSON jako text block
2. Multi-line string bez `\n` i `+`
3. Fields używają normalnych `"` (nie escaped)
4. Indentation jest automatic (compiler usuwa common whitespace)
5. Czytelne jak actual JSON file
```

## 6. Learning Delta — Co Nowe vs Lessons 06-11

| Temat | Lesson 06-11 | Lesson 12 |
|---|---|---|
| Generics | `List<T>`, `Map<K, V>` | Bounded wildcards (`<? extends T>`, `<? super T>`), PECS |
| instanceof | Explicit casting | Pattern matching (`instanceof Type var`) |
| switch | Traditional (break, fall-through) | Pattern matching switch (JDK 21+) |
| Strings | String concatenation, `+` | Text blocks (`"""..."""`) |
| Null handling | Null checks (`if (obj != null)`) | Optional<T> (`ifPresent`, `orElse`) |

## 7. Typowe Błędy

1. **Nadużywanie bounded wildcards.** Jeśli metoda nie potrzebuje flexibility, użyj `<T>` zamiast `<? extends T>`.
2. **Pattern matching bez exhaustive check.** W switch, compiler wymaga default lub wszystkich cases. Nie zapomnij o default.
3. **Text blocks z wrong indentation.** Compiler usuwa common leading whitespace. Jeśli pierwszy line ma inną indentation, wynik będzie unexpected.
4. **Optional dla parameters.** `void method(Optional<String> param)` jest anti-pattern. Użyj overloaded methods: `void method()` i `void method(String param)`.
5. **Optional dla fields.** `private Optional<String> field` jest anti-pattern. Użyj null i sprawdzaj w constructorze.
6. **Zapominanie o `orElseThrow`.** Jeśli Optional jest empty i nie obsługujesz tego, test failuje z `NoSuchElementException` zamiast clear error message.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Napisz helper method z bounded wildcard: `<T extends PaymentOrderResponse> void assertAllHaveStatus(List<T>, String)` | 20 min |
| 2 | Zrefaktoruj `if (obj instanceof PaymentOrderResponse)` aby używał pattern matching | 15 min |
| 3 | Napisz switch z pattern matching dla 3 typów response | 20 min |
| 4 | Zrefaktoruj JSON string concatenation aby używał text block | 15 min |
| 5 | Napisz `PaymentOrderJsonFixtures` z 5 text blocks dla różnych scenarios | 30 min |
| 6 | Zrefaktoruj API client aby zwracał `Optional<PaymentOrderResponse>` dla getOrder | 20 min |
| 7 | Napisz test używający `assertThat(optional).isPresent().get().extracting(...)` | 15 min |

## 9. Pytania

1. Kiedy używać `<? extends T>` vs `<? super T>`?
2. Co to jest PECS principle?
3. Jak pattern matching instanceof różni się od explicit casting?
4. Kiedy pattern matching switch jest lepsze niż if-else chain?
5. Dlaczego text blocks są lepsze niż string concatenation dla JSON?
6. Jak compiler oblicza indentation dla text blocks?
7. Kiedy używać Optional vs null checks?
8. Dlaczego Optional nie powinno być używane dla parameters?
9. Dlaczego Optional nie powinno być używane dla fields?
10. Jak AssertJ wspiera Optional assertions?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `boundedWildcardAcceptsSubtypes` | `List<? extends PaymentOrderResponse>` accepts subclasses |
| `patternMatchingInstanceofEliminatesCasting` | No explicit cast needed |
| `patternMatchingSwitchIsExhaustive` | Compiler requires all cases |
| `textBlockPreservesJsonFormatting` | JSON is readable and valid |
| `optionalIsPresentWhenOrderExists` | Optional contains value |
| `optionalIsEmptyWhenOrderNotFound` | Optional is empty |

## 11. Powiązane Notatki

- [[Lesson 08 - Java Records, Read-Only Services, and Input Validation]]
- [[Lesson 11 - Sealed Types, Defensive Copies, and Comparators]]
- [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [[Senior SDET Competency Coverage Matrix]]
