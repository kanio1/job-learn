/**
 * Problem/error contract layer — Phase 4.
 *
 * <p>Contains the test-side representation of {@code application/problem+json} responses:
 * <ul>
 *   <li>{@code ProblemDetail} — local record mirroring the backend error shape</li>
 *   <li>{@code ProblemCodes} — known {@code error} code constants</li>
 *   <li>{@code ProblemAssert} — custom AssertJ assertion over a REST Assured {@code Response}</li>
 * </ul>
 *
 * <p>No Spring {@code ProblemDetail} or backend DTO is imported here.
 *
 * <p>SDET concept: a custom {@code AbstractAssert} turns scattered status/header/body assertions
 * into a single fluent chain with readable failure messages — the classic answer to
 * "how do you handle error-response assertions across 40 tests?"
 */
package lab.paymentquality.apitest.core.problem;
