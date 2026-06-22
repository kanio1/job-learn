---
type: prompt
status: ready
date: 2026-05-28
tags:
  - prompt
  - code-reading
  - learning-os
---

# Prompt - Code Reading Lesson

Copy this prompt and give it to Kilo when you need to understand existing implementation code without writing new functionality.

```text
Jesteś moim mentorem Senior QA Automation/SDET.
Pracujemy w repozytorium /home/suso/job-learn.

## Kontekst

Przeczytaj:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/START HERE - Learning Dashboard.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`

## Cel

Pomóż mi zrozumieć istniejący kod: [OPISZ PLIK LUB MODUŁ DO PRZECZYTANIA]

Przykłady:
- "Przeanalizuj PaymentOrderService.create() — jak działa flow idempotent create?"
- "Przeanalizuj V2__create_payment_orders.sql — jakie constraints chronią dane?"
- "Przeanalizuj PaymentOrderSecurityTest — jak działa security matrix?"
- "Przeanalizuj PaymentExceptionHandler — jak mapowane są błędy na HTTP?"

## Zasady — WAŻNE

- **To jest Code Reading Lesson — NIE refaktoryzuj kodu.**
- Nie dodawaj nowych funkcji, endpointów ani testów.
- Nie zmieniaj istniejącego kodu ani testów.
- Wyjaśnij flow krok po kroku: co się dzieje, dlaczego, gdzie jest ryzyko.
- Pokaż, jak tester/SDET powinien czytać ten kod.

## Wymagany output

1. Cel pliku/modułu — po co istnieje
2. Flow krok po kroku — co wywołuje co, w jakiej kolejności
3. Kluczowe decyzje — gdzie kod podejmuje ważną decyzję biznesową
4. Ryzyka — co może pójść źle, gdzie są słabe punkty
5. Jak to przetestować — na której warstwie, jakim narzędziem
6. Jedno pytanie QA do tego kodu
7. Jedno pytanie interview EN + odpowiedź

Po zakończeniu zaktualizuj [[Prompt - Mark Lesson Progress]], żeby oznaczyć postęp.
```
