---
type: research-note
status: draft
area: JUnit REST Assured
module: Versioned Documentation
date: 2026-05-21
tags:
  - context7
  - rest-assured
  - junit
  - java-25
  - spring-mvc
---

# Context7 Research Notes

## Purpose

This note records the external documentation checks used while designing the REST Assured learning path. It is intentionally short and decision-oriented: what was checked, what it confirmed, and which lessons use the result.

## REST Assured

Context7 library used:

- `/rest-assured/rest-assured`

Topics checked:

- `given / when / then` DSL.
- `RequestSpecification`, `RequestSpecBuilder`.
- `ResponseSpecification`, `ResponseSpecBuilder`.
- Logging only when validation fails.
- Header blacklisting for sensitive values such as `Authorization`.
- OAuth2 bearer auth through `.auth().oauth2(token)`.
- Body assertions with Hamcrest matchers.
- Extraction through `extract().response()` and `extract().path(...)`.
- Object mapping, serialization and deserialization.

Confirmed points:

- The fluent `given().when().then()` style is a documented REST Assured usage pattern.
- Reusable request and response specs are supported through spec builders.
- REST Assured can log request/response data only when validation fails, which supports cleaner CI logs.
- REST Assured `LogConfig` supports blacklisting headers, so lessons can teach debugability without leaking bearer tokens into CI logs.
- REST Assured supports Java object mapping to request bodies and deserialization from JSON responses when JSON mapping libraries are on the classpath.
- Extraction APIs are appropriate for multi-step tests such as POST then GET by created id.

Lessons using this research:

- Lessons 1-8 for foundational syntax, body, assertions and extraction.
- Lessons 13-15 for reusable specs and logging.
- Lesson 15 specifically uses logging-on-validation-failure and header blacklisting.
- Lesson 16 for object mapping and test data choices.

## JUnit Jupiter

Context7 library used:

- `/websites/junit_current`

Topics checked:

- `@Test`.
- `@BeforeEach` and lifecycle callbacks.
- `@Nested` and test organization concepts.
- `@ParameterizedTest` display-name placeholders.
- Deprecation note around older parameterized placeholder constants.

Confirmed points:

- JUnit Jupiter lifecycle annotations provide setup/teardown hooks before and after each test or test class.
- `@Test` remains the core marker for test methods.
- Parameterized tests are useful for validation matrices, but deprecated placeholder constants should not be copied from old examples.
- Lifecycle callbacks are useful, but API tests should not hide important Given/When/Then intent behind excessive setup.

Lessons using this research:

- Lesson 10 for validation matrix thinking.
- Lesson 11 for test levels and lifecycle placement.
- Lesson 12 for structure and naming.
- Lesson 21 for review criteria.

## Java 25 and Mockito Tooling

Context7 libraries used:

- `/websites/openjdk_projects_jdk_25`
- `/mockito/mockito/v5.20.0`

Topics checked:

- JDK 25 release context and deprecation signal areas.
- JDK 25 as a long-term-support generation in vendor distributions.
- Dynamic-agent-related build hygiene as a tester concern.
- JDK 25 project notes around stronger integrity-by-default direction.
- Mockito documentation available through Context7 was limited for the exact self-attaching agent recommendation, so this curriculum treats the Mockito `-javaagent` change as a project case study rather than a full JVM internals lesson.

Confirmed points:

- JDK 25 is a modern LTS-era target and build logs/warnings matter because platform behavior evolves.
- JDK 25 documentation highlights continued platform work around integrity by default, reinforcing the lesson that warning-driven build hygiene matters.
- Deprecation and removal signals should be treated as future-failure risks, not ignored noise.
- For a tester/SDET, the important lesson is not JVM internals; it is recognizing warning-driven build hygiene and preferring explicit test-tool configuration over implicit dynamic behavior.

Lessons using this research:

- Lesson 5 for `Map.of` and immutable request payload thinking.
- Lesson 16 for records/DTOs/builders as test-data structures.
- Lesson 20 for Java 25 warning and Mockito agent awareness.

## Spring MVC and Spring Test

Context7 library used:

- `/websites/spring_io_spring-framework_reference`

Topics checked:

- `@RequestBody` conversion.
- `@Valid` on request bodies.
- Default validation failure behavior through `MethodArgumentNotValidException` and 400 responses.
- Controller method argument binding, including `@PathVariable`.
- Validation at controller method boundaries.

Confirmed points:

- Spring MVC converts request bodies through message converters and can validate them with `@Valid`.
- Validation failure at the request-body boundary results in a bad request path by default unless explicitly handled differently.
- Controller method arguments are framework-bound values, so malformed path-variable behavior is better validated through HTTP/web tests than through a hand-written unit test of parsing code when parsing is delegated to Spring binding.

Lessons using this research:

- Lesson 10 for malformed input and validation response thinking.
- Lesson 11 for the unit vs HTTP test-level decision.
- Lesson 17 for controller/application/domain boundaries.
- Lesson 19 for API-edge validation vs domain validation.

## Decision Impact Summary

| Research area | Curriculum decision |
|---|---|
| REST Assured DSL | Start with `given/when/then` before any reusable helper |
| REST Assured specs | Teach `RequestSpecification` only after the learner understands repeated setup |
| REST Assured logging | Include logging-on-validation-failure and header secrecy as professional practice |
| JUnit lifecycle | Teach lifecycle hooks as optional structure, not as a place to hide test intent |
| JUnit parameterized tests | Mention them for validation matrices, but avoid deprecated placeholder examples |
| Java 25 | Treat warnings and explicit test-tool configuration as SDET build hygiene |
| Spring MVC binding | Explain why malformed UUID belongs in HTTP/web-layer tests when using `@PathVariable UUID` |
