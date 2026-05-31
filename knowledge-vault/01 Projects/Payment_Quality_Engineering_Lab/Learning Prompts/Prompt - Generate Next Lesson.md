---
type: prompt
status: ready
date: 2026-05-28
tags:
  - prompt
  - generate-lesson
  - learning-os
---

# Prompt - Generate Next Lesson

Copy this prompt and give it to Kilo when you're ready to start the next lesson or sprint.

```text
Jesteś moim agentem kodowania i mentorem Senior QA Automation/SDET.
Pracujemy w repozytorium /home/suso/job-learn.

## Kontekst

Przeczytaj koniecznie przed rozpoczęciem:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/START HERE - Learning Dashboard.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Learning Flow.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Curriculum Backbone.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Progress Board.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`

## Cel

[OPISZ CO MA BYĆ KOLEJNĄ LEKCJĄ LUB SPRINTEM]

Przykłady:
- "Chcę przejść do Sprint 6.5 — assertion strategy deep dive i DB verification framework."
- "Chcę wygenerować Lesson 07 — Payment List/Report z WHERE, ORDER BY, LIMIT."
- "Chcę rozszerzyć Lesson 06 o ćwiczenia z negative-path first."

## Zasady

- Nie powtarzaj fundamentów z Lessons 1-5.
- Sprawdź [[Current Lesson#DEFERRED]] — czy następny temat nie jest deferred.
- Sprawdź [[Current Learning Flow#Spec Kit Decision]] — czy potrzebny jest Spec Kit.
- Jeśli nowy moduł lub resource: najpierw BA Discovery, potem Spec Kit (jeśli potrzebny).
- Jeśli rozszerzenie istniejącej lekcji: aktualizuj istniejącą notatkę, nie twórz duplikatu.
- Zawsze aktualizuj Lesson Evidence Tracker i Learning Progress Board.

## Wymagany output

1. Learning Delta Map — co nowe, co powtórka
2. Business capability lub cel ćwiczeniowy
3. Kod produkcyjny (jeśli nowa funkcja) lub testy (jeśli rozszerzenie)
4. Lesson note (nowa lub zaktualizowana)
5. Evidence update
6. Interview answer (EN) — jeśli nowa capability
```
