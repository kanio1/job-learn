# Exercise Template

Use this template for Exercise Mode responses. Respond in Polish by default, but keep code, identifiers, and suggested file names in English.

## Goal

State the exact Java testing/design skill the learner will practice and why it matters for REST API automation.

## Starter Code

```java
// Provide minimal starter code only when useful.
// Do not provide the final full solution unless the user asks for it.
```

## Tasks

### Beginner Task

Describe a small, safe task that builds the core concept.

### Intermediate Task

Describe a task that adds realistic REST API, Spring, Mockito, AssertJ, or builder complexity.

### Senior QA Automation Task

Describe a task focused on API contract risk, maintainability, deterministic CI behavior, test data isolation, or review quality.

### REST API Or Spring Test Version

Describe how to practice the same idea with REST Assured, MockMvc, `@WebMvcTest`, or `@SpringBootTest`.

### Mockito Or AssertJ Version

Describe how to practice the same idea with a typed captor, readable matcher, fluent assertion, extracting, filtering, or domain-specific assertion helper.

## Guiding Questions

- Before coding, what behavior are you proving?
- What is the narrowest useful test layer?
- Which data should be immutable?
- Which assumptions belong in the test name or assertion?
- What failure message would help you debug this in CI?

## Edge Cases

- Missing field.
- Null value.
- Empty collection.
- Duplicate value.
- Unexpected enum/status value.
- Order-sensitive vs order-insensitive response.
- Sensitive value that must not appear in logs.

## Tests To Write

- Happy path test.
- Validation or negative contract test.
- Edge-case test.
- Mockito collaboration test when a boundary matters.
- AssertJ collection/assertion readability test when collections are involved.

## Suggested Files

- `src/test/java/.../<Scenario>Test.java`
- `src/test/java/.../support/<Domain>TestDataBuilder.java`
- `src/test/java/.../support/<Domain>ApiClient.java`
- `src/main/java/.../<RequestOrResponse>.java` only when production DTO changes are part of the exercise.

## Expected Outcome

Describe what the learner should be able to explain, implement, and review after the exercise.

## Interview Explanation

Provide a short English explanation the learner should be able to say in an interview after completing the exercise.
