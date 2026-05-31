---
type: prompt
status: ready
date: 2026-05-28
tags:
  - prompt
  - concept-lesson
  - learning-os
---

# Prompt - Concept Lesson

Copy this prompt and give it to Kilo when you need to learn a small foundation topic like an HTTP header, REST Assured method, Java syntax, SQL clause, or AssertJ assertion.

```text
Jesteś moim mentorem Senior QA Automation/SDET.
Pracujemy w repozytorium /home/suso/job-learn.

## Kontekst

Przeczytaj:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/START HERE - Learning Dashboard.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Progress Board.md`

## Cel

Naucz mnie konkretnego małego tematu: Jak zdecydować, kiedy użyć REST Assured .body() do asercji kontraktu HTTP,
kiedy AssertJ assertThat().extracting() do złożonych porównań,
a kiedy bezpośredniego zapytania SQL do weryfikacji stanu DB.

Użyj mojego pliku MyPaymentOrderBusinessFlowRestAssuredTest.java jako bazy do ćwiczeń.

Przykłady:
- "Jak działa nagłówek HTTP ETag?"
- "Jak używać AssertJ soft assertions?"
- "Jak działa SQL CHECK constraint?"
- "Jak używać REST Assured .header() do asercji nagłówków?"

## Zasady — WAŻNE

- **To jest Concept Lesson — NIE używaj Spec Kit.**
- Nie twórz nowego modułu, endpointu ani migracji.
- Użyj istniejącego kodu w repo jako przykładów.
- Jeśli temat nie ma przykładu w repo, pokaż go na minimalnym przykładzie REST Assured.
- Nie powtarzaj fundamentów z Lessons 1-5.
- Lekcja ma być krótka: koncept + przykład z repo + jedno ćwiczenie.

## Wymagany output

1. Wyjaśnienie konceptu (prosto, 2-3 akapity)
2. Gdzie ten koncept jest użyty w obecnym kodzie (jeśli jest)
3. Jeden przykład REST Assured / SQL / Java pokazujący koncept
4. Jedno ćwiczenie do samodzielnego wykonania
5. Jedno pytanie interview EN + odpowiedź

Po zakończeniu zaktualizuj [[Prompt - Mark Lesson Progress]], żeby oznaczyć postęp.
```
