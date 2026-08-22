# Playwright method playbook (4 role)

Jak projektować business flows → use cases → test cases na żywym POM (`tests-pom`) i REST przez BFF.

**Nie** jest kopią `live-pom-wave-2` 01–10. Tam są ID UC/SCN. Tutaj: która rola decyduje i która klasa w `tests-pom/methods/` to trzyma.

| Plik | Głos |
|---|---|
| [01-pm-business-flows.md](01-pm-business-flows.md) | Product manager |
| [02-tech-lead-layers.md](02-tech-lead-layers.md) | Tech-lead — warstwy + trzy kategorie API |
| [03-test-architect-techniques.md](03-test-architect-techniques.md) | Test architect — ISTQB |
| [04-principal-typescript.md](04-principal-typescript.md) | Principal / SDET — foldery, wzorce |
| [05-combinations.md](05-combinations.md) | Dlaczego techniki się łączą |
| [06-scenario-catalog.md](06-scenario-catalog.md) | Indeks SCN → klasa / spec |
| [07-advanced-techniques-beyond-metamorphic.md](07-advanced-techniques-beyond-metamorphic.md) | Poza FL: CTAL-TA, RST, książki (interview) |

Kod: `apps/frontend/tests-pom/methods/`. Learner: kopiujesz sam do `tests-pom-learner`. Agent tam nie pisze.

**Zakaz ETL/SQL learning:** żadnego `POST /api/test/seed-learning` ani `/api/test/etl/payments/*` w POM.
