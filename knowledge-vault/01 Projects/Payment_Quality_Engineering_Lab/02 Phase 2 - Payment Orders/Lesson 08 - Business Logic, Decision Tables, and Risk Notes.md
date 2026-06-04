---
type: lesson
status: ready
area: Payment Quality Engineering Lab — Phase 2
lesson: 08
module: Business Logic, Decision Tables, and Risk Notes
date: 2026-05-30
tags:
  - business-logic
  - decision-table
  - risk-analysis
  - tenant-isolation
  - bola-bfla
  - lesson-08
  - payment-order-summary
  - senior-sdet
---

# Lesson 08 — Business Logic, Decision Tables, and Risk Notes

> **Evidence link:**
> - `PaymentOrderController.java` — ownership enforcement, platform reader bypass
> - `PaymentOrderSummaryService.java` — validation, date semantics, null normalization
> - `PaymentOrderSummarySecurityTest.java` — security matrix verification
> - `specs/005-payment-order-summary/spec.md` — FR-301 do FR-326
>
> **Navigation:** [[Lesson 08 - Payment Aggregation Summary]] | [[Lesson Evidence Tracker]] | [[Current Sprint]]

## 1. Cel Lekcji

Zrozumieć reguły biznesowe stojące za endpointem summary:
- read model vs lifecycle model,
- tenant isolation i ownership enforcement,
- role matrix,
- date filter semantics i edge case behavior,
- `fromDate > toDate` jako legalne zapytanie (empty summary),
- ryzyka: BOLA, BFLA, validation drift.

## 2. Prerequisites

- Lesson 06: single-resource create/read, idempotency, masked 404.
- Lesson 07: list/filter, pagination, security matrix dla listy.
- Lesson 08: system implementation (summary endpoint, service, repo).
- Role model: `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, `platform:payments:read`.
- JWT claim `merchant_id`.

## 3. Code Reading Map

| Plik | Reguła biznesowa |
|---|---|
| `PaymentOrderController.java:154-162` | ownership: platform reader bypass, merchant reader claim check |
| `PaymentOrderController.java:171-175` | response: 200 + X-Correlation-ID, bez ETag |
| `PaymentOrderSummaryService.java:84-91` | currency validation: tylko PLN/EUR/USD |
| `PaymentOrderSummaryService.java:93-102` | status validation: tylko CREATED |
| `PaymentOrderSummaryService.java:104-128` | date parsing: ISO, start-of-day inclusive, end-of-day inclusive |
| `PaymentExceptionHandler.java:47-51` | IAE → 400 validation |
| `PaymentExceptionHandler.java:84-88` | AccessDenied → 403 forbidden |
| `SecurityConfig.java:40-42` | matcher ordering: /summary przed /* |

## 4. Decision Table — Security Matrix

| Actor | Role(s) | Own Merchant Summary | Other Merchant Summary |
|---|---|---|---|
| Unauthenticated | none | **401** | **401** |
| Denied identity | none | **403** | **403** |
| Creator only | `merchant:payments:create` | **403** | **403** |
| Operator only | `merchant:payments:operate` | **403** | **403** |
| Reader + own merchant | `merchant:payments:read` + matching `merchant_id` | **200** | — |
| Reader + other merchant | `merchant:payments:read` + different `merchant_id` | — | **403** |
| Platform reader | `platform:payments:read` | **200** | **200** |

**Kluczowa decyzja:** cross-tenant summary zwraca **403** (overt refusal), NIE masked 404. To jest świadomy wybór:
- Summary to operacja kolekcyjna/raportowa — odmowa nie ujawnia istnienia pojedynczego zasobu.
- Single-resource read (Lesson 06) maskuje 404, bo tam istnienie zasobu jest wrażliwą informacją.

## 5. Decision Table — Date Filter Semantics

| Input | Zachowanie | Uzasadnienie |
|---|---|---|
| `fromDate` tylko | orders `>= fromDate 00:00:00 UTC` | inclusive start-of-day |
| `toDate` tylko | orders `<= toDate 23:59:59.999999999 UTC` | inclusive end-of-day |
| obie daty | orders w przedziale `[from, to]` | naturalny zakres |
| `fromDate > toDate` | **200 z empty summary** (nie 400) | legalny zakres, zero wyników — to nie błąd walidacji |
| `fromDate` w przyszłości | 200 z empty summary | brak orderów w przyszłości |
| `toDate` w przeszłości | 200 z empty summary | brak orderów przed tą datą (jeśli merchant nowy) |
| brak obu dat | wszystkie ordery | brak filtru daty |

## 6. Decision Table — Filter Interactions

| Currency filter | Status filter | Date filter | Efekt |
|---|---|---|---|
| brak | brak | brak | wszystkie ordery merchanta |
| PLN | brak | brak | tylko PLN ordery |
| brak | CREATED | brak | wszystkie ordery (wszystkie mają CREATED) |
| PLN | CREATED | brak | tylko PLN z CREATED |
| EUR | brak | from 2026-01-01 | EUR ordery od 01.01.2026 |
| brak | brak | from > to | empty summary (zero results) |
| GBP | — | — | 400 validation |

## 7. Risk Notes (QA Architecture)

### 7.1 BOLA — Broken Object Level Authorization

**Ryzyko:** Merchant reader odczytuje summary innego merchanta.

**Mitigacja w kodzie:** `PaymentOrderController.java:157-161` — sprawdzenie `merchant_id` claim przed wywołaniem service.

**Weryfikacja:** `PaymentOrderSummarySecurityTest.merchantReaderCannotAccessOtherMerchantSummary` — 403.

### 7.2 BFLA — Broken Function Level Authorization

**Ryzyko:** Creator lub operator (bez `read` roli) uzyskuje dostęp do summary.

**Mitigacja w kodzie:**
- `SecurityConfig.java:40` — `/summary` wymaga `merchant:payments:read` lub `platform:payments:read`.
- Creator ma tylko `merchant:payments:create` — Spring Security odrzuca przed kontrolerem.

**Weryfikacja:** `PaymentOrderSummarySecurityTest.merchantCreateOnlyCannotAccessSummary` — 403.

### 7.3 Validation drift

**Ryzyko:** Walidacja w service (`IllegalArgumentException`) różni się od walidacji w przyszłym kontrolerze lub DTO — różne komunikaty, różne kody błędów.

**Mitigacja:** `PaymentExceptionHandler.handleIllegalArgument` mapuje wszystkie `IAE` na `400 validation` z jednolitym kształtem odpowiedzi.

**Weryfikacja:** `PaymentOrderSummaryRestAssuredTest.invalidCurrencyReturns400Validation` — sprawdza `error: "validation"` i konkretny `message`.

### 7.4 Null semantics regression

**Ryzyko:** `SUM(amount_minor)` zwraca `NULL` dla empty merchant → API zwraca `null` zamiast `0`.

**Mitigacja:** `PaymentOrderSummaryService.java:71` — `totals.getTotalAmountMinor() != null ? ... : 0L`.
Dodatkowo: `COALESCE(SUM(po.amountMinor), 0)` w JPQL (redundancja obronna).

**Weryfikacja:** `PaymentOrderSummaryRestAssuredTest.emptyMerchantSummaryReturnsZeroTotals` — `totalAmountMinor=0`.

### 7.5 Matcher ordering regression

**Ryzyko:** Dodanie nowego endpointu (np. `/export`) przesuwa `/summary` matcher w nieodpowiednie miejsce.

**Mitigacja:** Komentarz/konwencja w `SecurityConfig.java`: specificzne matchery przed wildcard.

**Weryfikacja:** Test integracyjny lub Modulith test — sprawdzenie, że `/summary` zwraca 200, nie 404.

## 8. Key Distinction — Read Model vs Lifecycle Model

| Aspekt | Read Model (Lesson 08) | Lifecycle Model (przyszłe) |
|---|---|---|
| Endpoint | `GET /.../summary` | `POST /.../{id}/authorize` |
| Modyfikuje dane? | Nie | Tak |
| Potrzebuje ETag? | Nie | Tak (optimistic concurrency) |
| Potrzebuje PSP? | Nie | Tak |
| Potrzebuje nowych statusów? | Nie — `CREATED` wystarcza | Tak — `AUTHORIZED`, `CAPTURED`... |
| Transaction | `@Transactional(readOnly = true)` | `@Transactional` |

**Decyzja:** Lesson 08 celowo NIE dodaje lifecycle. Read model jest bezpiecznym, testowalnym rozszerzeniem bez ryzyka mutacji danych.

## 9. Ćwiczenia

1. **Narysuj decision table** dla wszystkich kombinacji `currency` × `status` × `fromDate` × `toDate` — które dają 200, a które 400?
2. **Co się stanie** gdy merchant ma 0 orderów i platform reader wywoła summary? Jaki status?
3. **Co by się zmieniło** gdyby cross-tenant summary zwracało 404 zamiast 403? Jakie ryzyko?
4. **Zaprojektuj w myśli** test dla scenariusza: merchant A reader próbuje summary merchanta B — jakie asercje?
5. **Które ryzyko** jest najważniejsze z perspektywy audytu bezpieczeństwa i dlaczego?

### Odpowiedzi do ćwiczeń

1. Decision table powinna mieć poprawne kombinacje filtrów jako `200` i przypadki niepoprawnego zakresu dat jako `400`, np. `fromDate > toDate`. `currency` i `status` są poprawne tylko wtedy, gdy należą do dozwolonych wartości kontraktu.
2. Platform reader powinien dostać `200` z pustym/zerowym summary. Brak orderów to poprawny wynik agregacji, nie brak zasobu.
3. `404` mogłoby sugerować, że merchant albo summary nie istnieje, zamiast jawnie odmówić zakresu. Dla operacji raportowej `403` lepiej pokazuje problem ownership i role policy.
4. Test powinien użyć tokena merchant A i path z merchant B. Asercje: status `403`, brak danych summary i opcjonalnie error code `forbidden`.

```java
then()
    .statusCode(403)
    .body("code", equalTo("forbidden"));
```

5. Najważniejsze jest BOLA/cross-tenant data leak, bo ujawnia dane płatnicze innego merchanta. Audyt bezpieczeństwa traktuje to jako bezpośrednie naruszenie tenant isolation.

## 10. Pytania Kontrolne

1. Dlaczego cross-tenant summary zwraca 403, a nie 404?
2. Co się dzieje gdy `fromDate > toDate`?
3. Jaka jest różnica między `merchant:payments:read` a `platform:payments:read` dla summary?
4. Dlaczego creator nie może czytać summary?
5. Co robi `COALESCE(SUM(...), 0)` w kontekście empty merchant?

### Odpowiedzi kontrolne

1. Cross-tenant summary zwraca `403`, bo jest operacją kolekcyjną i jawna odmowa nie ujawnia konkretnego order ID. Single read maskuje `404`, żeby ograniczyć enumerację zasobów.
2. `fromDate > toDate` powinno dać `400 validation`, bo zakres czasu jest logicznie niepoprawny. Test powinien odróżnić to od pustego wyniku.
3. `merchant:payments:read` działa tylko w granicy własnego `merchant_id`. `platform:payments:read` może czytać summary wybranego merchanta bez merchant ownership claim.
4. Creator ma uprawnienie do tworzenia, nie do raportowania lub czytania danych. To realizuje least privilege.
5. `COALESCE(SUM(...), 0)` zamienia `NULL` z pustego zbioru na `0`. Dzięki temu API zwraca stabilny numeric contract dla empty merchant.

## 11. Next Links

- [[Lesson 08 - Payment Aggregation Summary]] — pełna notatka lekcji
- [[Lesson 08 - Aggregation Contract, Security, and Business Flow Tests]] — testy
- [[Lesson 08 - Summary Endpoint Contract, Status Codes, and Error Taxonomy]] — HTTP kontrakt
- [[Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries]] — SQL side
- [[Lesson Evidence Tracker]]
- [[Current Sprint]]
