# Review Checklist

Use this checklist for precise reviews of Java REST API tests, REST Assured clients, Spring tests, Mockito tests, AssertJ assertions, DTOs, builders, and test support utilities.

## Code Review Checklist

- Has the repository build tool and dependency set been inspected before recommending version-sensitive changes?
- Does the code use existing package, naming, and test-layer conventions?
- Are DTOs, request objects, response objects, validation errors, and events immutable where appropriate?
- Are records used for value-like models where they fit?
- Are mutable internals protected with defensive copies?
- Is visibility minimal?
- Are utilities final with private constructors?
- Is constructor injection preferred over hidden static dependencies?
- Are raw types and unchecked warnings eliminated?
- Are enums used instead of string/int constants for closed sets?
- Is `ordinal()` avoided for business meaning?
- Are `EnumSet` and `EnumMap` used for enum flags/mappings where useful?
- Are streams readable, side-effect-free, and not overused?
- Are loops used when they make control flow clearer?

## Test Review Checklist

- Does each test verify behavior rather than implementation details?
- Is the test name a business scenario or clear technical contract?
- Are given/when/then phases readable?
- Are expected and actual values clear?
- Are assertions precise enough to fail diagnostically?
- Is test data isolated per test or immutable?
- Are negative cases explicit?
- Are edge cases covered where risk justifies them?
- Is the test deterministic in parallel CI?
- Does the test avoid sleeps, timing assumptions, and shared mutable state?
- Does the test layer match the risk being tested?

## REST API Contract Checklist

- Is the status code asserted?
- Is the meaningful response body asserted?
- Are relevant headers asserted?
- Is validation error shape or problem-details envelope asserted?
- Are request and response DTO fields aligned with the contract?
- Are serialization/deserialization assumptions visible?
- Are authentication and authorization boundaries tested where relevant?
- Are idempotency and correlation/trace IDs tested when part of the contract?
- Are pagination, sorting, and filtering tested when exposed?
- Is the distinction between contract, integration, and E2E scope clear?

## Spring Test Checklist

- Is `@WebMvcTest` used for focused controller/web-layer tests?
- Is `@SpringBootTest` used only when a fuller context is justified?
- Is `MockMvc`, `WebTestClient`, or `TestRestClient` chosen according to project conventions?
- Are external boundaries mocked at the right layer?
- Are slice tests free from unnecessary infrastructure?
- Are integration tests realistic but focused?
- Is `ObjectMapper` used consistently with application configuration?
- Are validation and exception handling assertions present where relevant?

## Mockito Checklist

- Are external boundaries mocked instead of simple value objects?
- Are immutable arguments used so `eq(...)` and captors are stable?
- Is `ArgumentCaptor<T>` typed?
- Is `argThat` short and readable?
- Are complex matchers extracted to named methods?
- Are only meaningful observable collaborations verified?
- Is over-verification avoided?
- Does the test remain understandable without knowing implementation internals?

## AssertJ Checklist

- Are fluent assertions used consistently?
- Is `isEqualTo` used when `equals` is meaningful?
- Is `extracting` used for collection fields where clearer?
- Is `containsExactly` used only when order matters?
- Is `containsExactlyInAnyOrder` used when order does not matter?
- Are `allSatisfy` and `anySatisfy` used for nested checks when clearer?
- Is recursive comparison used sparingly and deliberately?
- Would a domain-specific assertion helper remove repetition?

## Security / Logging Checklist

- Are tokens, passwords, API keys, Authorization headers, PAN, CVV, and personal data absent from files?
- Are sensitive values masked in logs and `toString` output?
- Are REST Assured logging filters configured safely?
- Do tests avoid printing full request/response payloads containing secrets?
- Are failure messages diagnostic without leaking sensitive data?
- Are test credentials avoided or injected through safe local configuration when needed?

## JDK 25 Language Feature Checklist

- Are records used for immutable value-like DTOs and test data where appropriate?
- Are compact record constructors used for validation and defensive copies?
- Are sealed interfaces plus records used for closed result hierarchies where useful?
- Are `List.of`, `Set.of`, and `Map.of` used for small immutable test data?
- Are `List.copyOf`, `Set.copyOf`, and `Map.copyOf` used to protect inputs?
- Is `Optional<T>` used for return values only when absence is meaningful?
- Are standard functional interfaces preferred over custom ones?
- Are method references used only when clearer than lambdas?
- Are raw types, generic arrays, and unchecked warnings avoided?
