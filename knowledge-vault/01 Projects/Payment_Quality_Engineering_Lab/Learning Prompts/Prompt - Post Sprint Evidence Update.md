---
type: prompt
status: ready
date: 2026-05-28
tags:
  - prompt
  - evidence-update
  - learning-os
---

# Prompt - Post Sprint Evidence Update

Copy this prompt and give it to Kilo after completing a learning sprint.

```text
Jesteś moim agentem kodowania i mentorem.
Pracujemy w repozytorium /home/suso/job-learn.

## Kontekst

Przeczytaj:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Progress Board.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Senior SDET Competency Coverage Matrix.md`

## Cel

Zaktualizuj wszystkie trackery po ukończeniu sprintu: [NAZWA SPRINTU].

## Co zostało zrobione

[OPISZ CO ZOSTAŁO ZREALIZOWANE W SPRINCIE]

## Co zaktualizować

1. **Current Sprint** — oznacz jako complete, zaktualizuj status zadań
2. **Current Lesson** — przenieś COVERED itemy, zaktualizuj statusy
3. **Lesson Evidence Tracker** — dodaj pełny wpis dla sprintu:
   - Production code evidence
   - Test code evidence
   - Vault notes
   - Spec Kit artifacts (jeśli były)
   - Commands run (i ich wynik)
   - Competency matrix updates
   - Open risks
   - Interview answer EN
   - Next sprint handoff
4. **Learning Progress Board** — zaktualizuj:
   - Lesson progress (status, interview ready)
   - Competency progress (Covered, Introduced)
   - Sprint progress (completion %)
   - Weekly goals
5. **Learning Coverage Backlog** — przenieś zaadresowane gapy z High/Medium do odpowiedniej sekcji
6. **Senior SDET Competency Coverage Matrix** — zmień statusy na `Practiced` / `Evidence Strong`

## Co NIE ruszać

- Nie zmieniaj statusów dla tematów DEFERRED
- Nie usuwaj z Coverage Backlog tematów, które nadal nie są pokryte
- Nie kasuj dowodów z poprzednich lekcji
```
