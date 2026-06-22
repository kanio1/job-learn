---
type: lesson
status: ready
area: Java 25 For SDET
lesson: 08
module: Records, Read-Only Services, Input Validation
date: 2026-05-30
tags:
  - java-25
  - records
  - dto
  - transactional
  - validation
  - lesson-08
  - payment-order-summary
  - senior-sdet
---

# Lesson 08 — Java Records, Read-Only Services, and Input Validation

> **Evidence link:** `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryRequest.java`, `PaymentOrderSummaryResponse.java`, `PaymentOrderSummaryService.java`
>
> **Navigation:** [[Java 25 For SDET MOC]] | [[Lesson 08 - Payment Aggregation Summary]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się czytać i projektować:
- **immutable DTO** przez Java `record` (request + response summary),
- **read-only serwis** z `@Transactional(readOnly = true)`,
- **walidację wejścia** z poprawnym mapowaniem błędów na `400 validation`,
- **mapowanie SQL projection → DTO** z normalizacją `null`.

Wszystko na realnym kodzie Lesson 08 — `PaymentOrderSummaryRequest`, `PaymentOrderSummaryResponse`, `PaymentOrderSummaryService`.

## 2. Prerequisites

- Java `record` składnia (brak setterów, `equals`/`hashCode`/`toString` generowane automatycznie).
- `@Service` + konstruktor injection (bez `@Autowired`).
- Podstawy `@Transactional` (READ_COMMITTED, JDBC connection).
- Enum `PaymentStatus` i jego użycie w systemie.
- Koncept `IllegalArgumentException` i `DateTimeParseException`.

## 3. Code Reading Map

| Plik | Czego uczy |
|---|---|
| `PaymentOrderSummaryRequest.java` | record z nullable polami, bez walidacji — walidacja delegowana do service |
| `PaymentOrderSummaryResponse.java` | record z nested records (`CurrencySummary`, `StatusSummary`), `long` zamiast `double` dla kwot |
| `PaymentOrderSummaryService.java` | `@Transactional(readOnly)`, parsing dat (ISO → `Instant`), walidacja `currency`/`status`, normalizacja `null` z projekcji SQL |

## 4. Kluczowe Pojęcia

### 4.1 Record jako immutable DTO

```java
public record PaymentOrderSummaryRequest(
        String currency,
        String status,
        String fromDate,
        String toDate
) {}
```

- Wszystkie pola są `private final` — kompilator generuje konstruktor, gettery (`currency()`, nie `getCurrency()`), `equals`, `hashCode`, `toString`.
- **Nie mutowalne** — idealne jako request/response DTO.
- Brak adnotacji `@Valid` na request — walidacja świadomie robiona w service (nie przez Bean Validation), bo logika walidacji jest bardziej złożona niż proste `@NotNull`.

### 4.2 Nested records

```java
public record PaymentOrderSummaryResponse(
        long totalOrders,
        long totalAmountMinor,
        List<CurrencySummary> byCurrency,
        List<StatusSummary> byStatus
) {
    public record CurrencySummary(String currency, long orderCount, long totalAmountMinor) {}
    public record StatusSummary(String status, long orderCount, long totalAmountMinor) {}
}
```

- Nested records są pełnoprawnymi typami — mogą być używane w AssertJ `extracting(...).contains(tuple(...))`.
- `List<CurrencySummary>` — niemutowalna referencja do listy (sama lista może być mutowalna, ale record jej nie zmienia).
- **Dlaczego nested w recordzie a nie osobne pliki?** Bo `CurrencySummary` i `StatusSummary` istnieją tylko w kontekście `PaymentOrderSummaryResponse`.

### 4.3 @Transactional(readOnly = true)

```java
@Service
@Transactional(readOnly = true)
public class PaymentOrderSummaryService { ... }
```

- `readOnly = true` — hint dla Hibernate/PostgreSQL: brak dirty checking, brak flush, optymalizacja connection.
- Wszystkie metody w klasie dziedziczą `readOnly`.
- **Nie wolno** wywoływać `repository.save()` wewnątrz readOnly transakcji.

### 4.4 Null-to-zero normalization

```java
row.getTotalAmountMinor() != null ? row.getTotalAmountMinor() : 0L
```

- PostgreSQL `SUM(amount_minor)` zwraca `NULL` gdy nie ma pasujących wierszy.
- Java `Long` (boxed) może być `null`, `long` (primitive) nie.
- Service normalizuje `null` → `0` przed włożeniem do response DTO.

### 4.5 Input validation pattern

```java
private void validateCurrency(String currency) {
    if (currency == null) return;          // optional filter — pomiń
    if (!SUPPORTED_CURRENCIES.contains(currency))
        throw new IllegalArgumentException("currency must be PLN, EUR, or USD");
}
```

- Walidacja w service, nie w kontrolerze — pozwala na bogatszą logikę niż adnotacje.
- `IllegalArgumentException` łapany przez `PaymentExceptionHandler` i mapowany na `400 validation`.

### 4.6 Date parsing

```java
private Instant toFromInclusive(String fromDate) {
    if (fromDate == null || fromDate.isBlank()) return null;
    LocalDate parsed = LocalDate.parse(fromDate);                        // ISO: YYYY-MM-DD
    return parsed.atStartOfDay(ZoneOffset.UTC).toInstant();              // 00:00:00 UTC
}
```

- `LocalDate.parse` akceptuje wyłącznie `YYYY-MM-DD` — `2026/05/30` rzuci `DateTimeParseException`.
- `toFromInclusive` zwraca `Instant` reprezentujący początek dnia UTC.
- `toToInclusive` zwraca koniec dnia: `23:59:59.999999999`.

## 5. Walkthrough Krok Po Kroku

1. Kontroler odbiera query params jako `String` → tworzy `PaymentOrderSummaryRequest`.
2. Kontroler wywołuje `summarize(merchantId, request)`.
3. Service normalizuje puste stringi do `null` (`normalizeOptional`).
4. Service waliduje `currency` (whitelist `PLN/EUR/USD`) i `status` (enum `CREATED`).
5. Service parsuje `fromDate`/`toDate` do `Instant` (UTC, start-of-day / end-of-day).
6. Service deleguje do repozytorium — trzy osobne zapytania agregacyjne.
7. Repozytorium zwraca projekcje: `SummaryTotalsProjection`, `CurrencySummaryProjection`, `StatusSummaryProjection`.
8. Service mapuje projekcje na DTO, normalizując `null` sums → `0L`.
9. Service zwraca `PaymentOrderSummaryResponse`.

## 6. Delta vs Lesson 07

| Aspekt | Lesson 07 | Lesson 08 |
|---|---|---|
| Request DTO | `PaymentOrderListRequest` — 10 pól | `PaymentOrderSummaryRequest` — 4 pola |
| Walidacja | W kontrolerze (`BindException` + `@Valid`) | W service (`IllegalArgumentException`) |
| Response DTO | `PaymentOrderListResponse` — Page wrapper | `PaymentOrderSummaryResponse` — agregat z nested records |
| Transakcja | `@Transactional(readOnly)` — ten sam pattern | TEN SAM reuse |
| Instancja dat | `LocalDate.parse` przez Specification | `LocalDate.parse` → `Instant` przez service |
| Enum mapping | `PaymentStatus.valueOf()` — ten sam pattern | TEN SAM reuse |
| Nowe | — | `record` z nested records, SQL projection, null-to-zero |

## 7. Typowe Błędy i Antywzorce

| Błąd | Dlaczego zły | Poprawnie |
|---|---|---|
| `double` dla kwot (`totalAmountMinor`) | floating-point rounding | `long` w minor units |
| Walidacja tylko przez Bean Validation `@NotNull` | nie pokryje logiki "GBP nie jest dozwolone" | service-layer validation |
| Pobieranie wszystkich encji i sumowanie w Javie | N+1, memory pressure | `GROUP BY` z `SUM` w SQL |
| `@Transactional` bez `readOnly` na read-only endpoint | dirty checking overhead | `@Transactional(readOnly = true)` |
| Rzucanie własnego wyjątku zamiast `IllegalArgumentException` | łamie istniejący handler `PaymentExceptionHandler` | `IAE` mapowany na `400` |
| `record` z setterami | nie da się — record jest niemutowalny | konstruktor kanoniczny |

## 8. Ćwiczenia

1. **Znajdź w kodzie** wszystkie miejsca gdzie `null` jest mapowane na wartość domyślną (0, pusta lista). Zapisz je w notatce.
2. **Dodaj nowy filter** (w myśli): `?minAmount=1000` — co trzeba zmienić w `PaymentOrderSummaryRequest`, service i repozytorium?
3. **Przetestuj w głowie**: co się stanie gdy `fromDate` to `""` (empty string)?
4. **Porównaj** `PaymentOrderSummaryRequest` z `PaymentOrderListRequest` — która ma więcej pól i dlaczego?
5. **Napisz test jednostkowy** w myśli dla `validateCurrency("GBP")` — jakie exception, jakie message?

## 9. Pytania Kontrolne

1. Dlaczego `PaymentOrderSummaryRequest` nie ma adnotacji `@Valid`?
2. Co robi `@Transactional(readOnly = true)` na poziomie PostgreSQL?
3. Dlaczego `totalAmountMinor` jest `long`, a nie `Long`?
4. Co się stanie gdy `SUM(amount_minor)` zwróci `NULL` z PostgreSQL?
5. Jak `PaymentExceptionHandler` mapuje `IllegalArgumentException` na odpowiedź HTTP?

## 10. Jak To Testować

- Test jednostkowy service: mock repozytorium, podaj różne kombinacje `null`/puste/valid params, zweryfikuj response shape.
- Test kontraktowy REST: sprawdź `400` dla invalid `currency`, `status`, `fromDate`.
- Anti-flakiness: każdy test tworzy własnego merchanta i własne dane.
- Oracle: jawnie policzone expected totals z seed data, nie z response.

## 11. Next Links

- [[Lesson 08 - Payment Aggregation Summary]] — pełna notatka lekcji
- [[Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries]] — SQL side
- [[Lesson 08 - Aggregation Contract, Security, and Business Flow Tests]] — testy REST Assured
- [[Java 25 For SDET MOC]]
- [[Lesson Evidence Tracker]]
