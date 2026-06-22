---
type: moc
status: ready
project: Payment Quality Engineering Lab
area: Learning Governance
date: 2026-05-27
tags:
  - learning-governance
  - sdet
  - qa-architecture
  - competency-tracking
---

# Learning Governance - MOC

Ten folder pilnuje, żeby ścieżka nauki i rozbudowa aplikacji nie rozjechały się na luźne lekcje, przypadkowe technologie albo sprinty bez testowalnej wartości biznesowej.

> **Start here:** [[START HERE - Learning Dashboard]]
>
> **Reference:** [[Current Learning Flow]] | [[Curriculum Backbone]] | [[Current Lesson]]

## Główne Pliki

- `Senior SDET Competency Coverage Matrix.md` - lista kompetencji i status pokrycia przez aplikację.
- `Lesson Evidence Tracker.md` - ewidencja per lekcja: co zrobione, co deferred, what NOT to touch.
- `Skill Orchestration Runbook.md` - uproszczony flow: Path A (extension, no Spec Kit) vs Path B (new capability, with Spec Kit).
- `Expert Gap Analysis - Senior SDET Coverage.md` - analiza luk i rekomendacje eksperta.

## Learning OS Files

- [[START HERE - Learning Dashboard]] — dashboard, pierwszy plik do otwarcia
- [[Current Learning Flow]] — proces nauki (Path A / Path B)
- [[Current Lesson]] — co robić TERAZ
- [[Current Sprint]] — status sprintu
- [[Curriculum Backbone]] — mapa technologia ↔ lekcja
- [[How To Use This Vault]] — instrukcja vaulta

## Zasada

Od Lesson 6 każda lekcja lub sprint musi zostawić trzy ślady:

1. `Competency Evidence` - które kompetencje zostały realnie przećwiczone w kodzie, testach albo analizie.
2. `Learning Delta` - co było nowe względem poprzednich lekcji, bez powtarzania fundamentów.
3. `Skill Trace` - jakie skills powinny zostać uruchomione i jaki output miały dostarczyć.

## Gdzie Trafiają Rzeczy

| Artefakt | Lokalizacja |
|---|---|
| Prompt do lekcji/sprintu | `../Learning Prompts/` |
| Strategia product-learning | `../Strategy/` |
| Competency tracking | `Senior SDET Competency Coverage Matrix.md` |
| Evidence per lekcja/sprint | `Lesson Evidence Tracker.md` |
| Skill runbook | `Skill Orchestration Runbook.md` |
| Szczegółowa lekcja REST Assured | `../../02 Areas/Technical Learning/JUnit REST Assured/` |
| Wiedza Java/SQL/Spring/Security | `../../02 Areas/Technical Learning/` odpowiedni subfolder |
| Interview story | `../../02 Areas/Interview Capital/` |

## Definition Of Done Dla Lekcji Od Lesson 6

- [ ] Jest wskazana business capability albo jawna decyzja, że lekcja jest tylko analizą.
- [ ] Jest `Learning Delta Map` bez długiego powtarzania Lessons 1-5.
- [ ] Jest link do promptu.
- [ ] Są linki do kodu produkcyjnego i testów albo informacja, że to dopiero Spec Kit input.
- [ ] Są zaktualizowane kompetencje w matrixie.
- [ ] Jest ewidencja w trackerze lekcji.
- [ ] Są wskazane skills i ich oczekiwane outputy.
- [ ] Jest test strategy i evidence command, nawet jeśli komenda nie została uruchomiona.
- [ ] Jest pytanie interview po angielsku.
