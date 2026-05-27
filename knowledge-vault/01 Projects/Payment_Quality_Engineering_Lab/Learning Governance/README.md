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

## Główne Pliki

- `Senior SDET Competency Coverage Matrix.md` - lista kompetencji Java 25, HTTP, REST, REST Assured, AssertJ, JUnit, Spring, SQL, security, framework architecture i status pokrycia przez aplikację.
- `Lesson Evidence Tracker.md` - ewidencja per lekcja/sprint: co zostało przerobione, gdzie jest kod, testy, notatki, prompt, skill output i dowód opanowania.
- `Skill Orchestration Runbook.md` - kiedy i w jakiej kolejności uruchamiać skills dla Business Analysis, architektury, Spec Kit, implementacji i QA Architecture.

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
