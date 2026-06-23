/**
 * JUnit extension and lifecycle support — Phase 6 (deferred).
 *
 * <p>Will contain:
 * <ul>
 *   <li>{@code ApiTest} — meta-annotation composing all JUnit extensions</li>
 *   <li>{@code ApiStackExtension} — starts Testcontainers stack once per session</li>
 *   <li>{@code SeedLifecycleExtension} — calls {@code POST /api/test/reset} then seed between tests</li>
 *   <li>{@code ScopedContextExtension} — wraps test execution in {@code ScopedValue.where(...).run(...)}</li>
 *   <li>{@code Eventually} — Awaitility wrapper for async audit assertions</li>
 * </ul>
 */
package lab.paymentquality.apitest.support;
