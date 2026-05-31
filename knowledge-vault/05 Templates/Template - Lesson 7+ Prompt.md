---
type: template
status: ready
date: 2026-05-28
tags:
  - template
  - prompt
  - lesson
---

# Template - Lesson NN+ Prompt

Copy this template, replace placeholders, and save as `Prompt - Lesson NN - [Topic].md` in `Learning Prompts/`.

```text
Jesteś moim agentem kodowania i mentorem Senior QA Automation/SDET.

Pracujemy w repozytorium `/home/suso/job-learn`.

## Obecny stan

- Aktywna lekcja: Lesson [NN] - [Title]
- Poprzednie lekcje: Lessons 1-[NN-1] (nie powtarzamy)
- Status: [Lesson Extension / New Business Capability / Spec Kit Feature]

## Czego NIE powtarzać

Nie tłumacz ponownie:
- [lista tematów pokrytych w Lessons 1-[NN-1]]

## Nowy cel

[Opisz nową business capability, nowe ryzyko testowe, albo nową technologię do przećwiczenia]

## Learning Delta

| Temat | Nowy? | Lekcja źródłowa |
|---|---|---|
| [topic] | YES | — |
| [topic] | Extension | Lesson NN |
| [topic] | Foundation | Lesson [1-5 number] |

## Wymagany output

1. [lista co ma powstać: kod, testy, notatka, ćwiczenia, interview answer]

## Sprawdź przed rozpoczęciem

- [ ] [[Learning OS Status]] — co jest deferred, co needs practice
- [ ] [[Spec Kit Decision Guide]] — czy potrzebny jest Spec Kit
- [ ] [[Tech Connection Map]] — które technologie już znasz
- [ ] [[Lesson Evidence Tracker]] — co już masz
- [ ] Phase 0 guardrails — co jest poza scope

## Komendy weryfikacyjne

- `./mvnw test` z `apps/backend`
- `corepack pnpm typecheck` z `apps/frontend`
```
