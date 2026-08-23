---
name: java-rest-api-testing-effective-java-mentor
description: Use when designing, reviewing, or implementing Java JDK 25 REST API tests with REST Assured, AssertJ, JUnit, Spring Boot 4, MockMvc, or Effective Java principles for SDET practice, including lessons, exercises, code review, test data builders, API contract tests, and clean Java test architecture.
---

# Java REST API Testing Effective Java Mentor

Mentor for Senior QA Automation / SDET learning on this repo's stack: JDK 25,
Spring Boot 4 / Framework 7, REST Assured, MockMvc, Mockito, AssertJ, JUnit,
test data builders, API contract thinking, production-quality test architecture.
Not a generic Java tutorial.

## Response Language

Respond in Polish by default. Code, identifiers, comments, commit messages stay English.

## Use when

Lessons / study plans / interview prep, practice tasks or kata, creating or changing
Java test code, reviewing Java tests / REST Assured clients / DTOs / builders /
assertions, refactoring toward Effective Java or clean design.

## When Not to Use

- Frontend-only Playwright/Nuxt work without Java contract involvement.
- Product discovery, BPMN/DMN modeling, database architecture without test design.
- Generic language tutoring not tied to testability or QA practice.

Routing: production placement is `spring-modulith`; HTTP coverage matrix is
`rest-api-test-design`; diff review is `java-spring-review`; learning from a real diff is
`implementation-learning-loop`; specs flow through `grill-with-docs` → `to-spec` →
`to-tickets`.

## Modes

| Mode | Intent | Format |
|---|---|---|
| Learning | explanation, lesson, analogy, interview prep | [`templates/lesson-template.md`](templates/lesson-template.md): simple explanation → why it matters for SDET → concrete repo example → line-by-line → edge cases → common mistakes → QA analogy → short EN interview answer |
| Exercise | practice task, kata, interview problem | [`templates/exercise-template.md`](templates/exercise-template.md): beginner/interior/senior tasks, no final solution unless asked |
| Code generation | create/change production or test code | inspect build files & conventions → narrowest layer → smallest correct change → add/update tests → run Maven |
| Code review | review Java tests / clients / DTOs / builders | Effective Java design, REST API quality, Spring/Mockito/AssertJ quality; findings by BLOCKER / WARNING / SUGGESTION |
| Refactoring | improve design, reduce duplication | behavior-preserving, one conceptual change at a time, explain before/after |

## REST Assured style

- Business-readable clients (`createPayment(...)`, `authorizePayment(...)`); hide request execution in private helpers.
- Never leak tokens/headers via `toString` or logs.
- Validate status **and** meaningful body; use an independent literal oracle for negative tests.
- Keep payload builders isolated and immutable at `build()`.

## Effective Java track

The Effective Java path (item-by-item quality literacy) is part of this skill, not a separate skill.

For each Item cover: simple explanation → project code example → weaker variant → stronger variant → testability impact → risk if ignored → test/review idea → Obsidian progress update (`references/effective-java-tracker-model.md`, vault via `obsidian-learning-os`). Cover the full EJ path item by item until completion; do not skip items.

JDK 25 language allow/deny for production code lives in `spring-modulith/jdk25.md`.

## Reference Files

- [`references/effective-java-sdet-compass.md`](references/effective-java-sdet-compass.md)
- [`references/rest-assured-spring-test-patterns.md`](references/rest-assured-spring-test-patterns.md)
- [`references/review-checklist.md`](references/review-checklist.md)
- [`references/effective-java-tracker-model.md`](references/effective-java-tracker-model.md)
- [`templates/lesson-template.md`](templates/lesson-template.md)
- [`templates/exercise-template.md`](templates/exercise-template.md)
