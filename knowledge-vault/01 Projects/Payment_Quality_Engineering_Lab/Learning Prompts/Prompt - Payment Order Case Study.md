---
type: prompt
status: ready
date: 2026-05-28
tags:
  - prompt
  - case-study
  - payment-order
  - learning-os
---

# Prompt - Payment Order Case Study

Copy this prompt and give it to Kilo when you need to study a specific behavior within the Payment Order vertical slice.

```text
Jesteś moim mentorem Senior QA Automation/SDET.
Pracujemy w repozytorium /home/suso/job-learn na branchu 004-payment-order-create-read.

## Kontekst

Przeczytaj:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/START HERE - Learning Dashboard.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Curriculum Backbone.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 06 - Payment Order Create Read Foundation.md`
- `specs/003-payment-order-access-lifecycle/plan.md`

## Cel

Przeprowadź case study na konkretnym zachowaniu Payment Order: [OPISZ ZACHOWANIE]

Przykłady:
- "Idempotent create: jak Idempotency-Key i request fingerprint chronią przed duplikacją?"
- "Tenant isolation: jak merchant_id claim i role matrix blokują cross-tenant dostęp?"
- "HTTP headers w Payment Order: Location, ETag, X-Correlation-ID — jak działają i jak je testować?"
- "DB constraints: jak payment_orders, idempotency_records i status_history razem chronią integralność?"
- "Security matrix: jak 7 ról współpracuje z tenant ownership?"
- "REST Assured assertions: jak testować contract API dla create/read/replay/conflict?"

## Zasady

- To jest case study istniejącego kodu — NIE dodawaj nowych endpointów ani funkcji.
- Nie powtarzaj fundamentów REST Assured z Lessons 1-5.
- Skup się na jednym zachowaniu: kod produkcyjny → testy → SQL → security → ryzyka.
- Każdy przykład musi pochodzić z realnego kodu w repo.

## Wymagany output

1. Wyjaśnienie zachowania — co system robi i dlaczego
2. Kod produkcyjny — gdzie to zachowanie jest zaimplementowane
3. Testy — jakie testy chronią to zachowanie i na której warstwie
4. SQL/DB — jakie constraints lub migracje wspierają to zachowanie
5. Security — jakie role/claims kontrolują dostęp
6. Ryzyka — co może pójść źle, edge cases
7. Decision table — tabela decyzyjna dla wariantów zachowania
8. Ćwiczenie — jedno ćwiczenie do samodzielnego wykonania
9. Interview answer EN — jak wytłumaczyć to zachowanie na rozmowie

## Tematy do pokrycia w case study

W zależności od wybranego zachowania, pokryj odpowiednie technologie:

| Zachowanie | Java | REST Assured | HTTP | SQL | Security | Test Design |
|---|---|---|---|---|---|---|
| Idempotent create | IdempotencyKey, RequestFingerprint | replay/conflict tests | Idempotency-Key, 201/200/409 | unique constraint | merchant scope | decision table |
| Tenant isolation | — | cross-tenant tests | 403 vs 404 | merchant_id FK | role matrix, claim | BOLA/BFLA |
| HTTP headers | — | header assertions | Location, ETag, X-Correlation-ID | — | — | contract testing |
| DB constraints | PaymentAmount, CurrencyCode | — | — | check, FK, unique | — | constraint testing |
| Security matrix | — | security tests | 401/403 | — | 7 roles × create/read | auth matrix |

Po zakończeniu zaktualizuj [[Prompt - Mark Lesson Progress]], żeby oznaczyć postęp.
```
