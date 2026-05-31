---
type: template
status: ready
date: 2026-05-28
tags:
  - template
  - lesson
---

# Template - Lesson NN+ Note

Copy this template, replace placeholders, and save in the appropriate phase folder.

```markdown
---
type: lesson
status: planned
project: Payment Quality Engineering Lab
phase: [phase number]
lesson: [lesson number]
area: [area name]
module: [module name]
date: YYYY-MM-DD
tags:
  - lesson
  - lesson-[NN]
  - payment-quality-lab
  - [tech tags]
---

# Lesson [NN] - [Title]

## 1. Cel Lekcji

[1-2 zdania o celu]

## 2. Co Zbudowaliśmy / Co Ćwiczymy

[Aktorzy, flow, główne decyzje]

## 3. Learning Delta Względem Poprzednich Lekcji

| Temat | Status (Nowy / Rozszerzenie / Nie powtarzamy) |
|---|---|
| [topic] | New |
| [topic] | Extension of Lesson XX |
| [topic] | Foundation from Lessons 1-5 |

## 4. Mapa Kodu

[Pliki produkcyjne i testowe z krótkim opisem "po co istnieje"]

## 5. Architecture Walkthrough

[Module boundaries, transaction boundaries, security decisions]

## 6. HTTP I REST API

[Endpointy, status codes, headers, curl przykłady]

## 7. Java 25 I Java Code Reading

[Value objects, entities, enums, records — co czytać i dlaczego]

## 8. SQL, PostgreSQL I Flyway

[Tabele, constraints, migracje, ćwiczenia SQL]

## 9. Security I Tenant Isolation

[Security matrix, role decisions, 401/403/404 decisions]

## 10. REST Assured Learning Path

[Tabela testów: #, nazwa, cel, ryzyko, expected]

## 11. Assertion Strategy

[Kiedy REST Assured body, kiedy AssertJ, kiedy DB query]

## 12. Test Data Ownership

[Strategia: per-test, namespacing, cleanup, parallel safety]

## 13. Pytania Do Samodzielnej Odpowiedzi

[5-25 pytań pokrywających HTTP, RA, Java, SQL, Security]

## 14. Zadania Praktyczne

| Zadanie | Files | Command | Expected |
|---|---|---|---|
| [name] | [files] | [cmd] | [outcome] |

## 15. Mini Interview Prep

[3-8 pytań + odpowiedzi EN]

## 16. Verification Commands

```
./mvnw test                 # all backend
./mvnw -Dtest=XxxTest test  # specific test
corepack pnpm typecheck      # frontend
```

## 17. Learning Outcome Checklist

Po tej lekcji umiem:
- [ ] [outcome]
- [ ] [outcome]

## 18. Powiązane Notatki W Vault

- [[Learning OS Status]]
- [[Tech Connection Map]]
- [[Lesson Evidence Tracker]]
- [[Spec Kit Decision Guide]]
- [inne notatki]
```
