/**
 * Test context layer — Phase 3.
 *
 * <p>{@link lab.paymentquality.apitest.core.context.TestContext} is an immutable value object
 * carrying the current correlation ID and identity for one test execution.
 * {@link lab.paymentquality.apitest.core.context.Ctx} is the per-thread holder that
 * {@link lab.paymentquality.apitest.core.http.AuthFilter} and
 * {@link lab.paymentquality.apitest.core.http.CorrelationFilter} read to inject headers.
 *
 * <p>Design note: {@code ThreadLocal} is used instead of {@code ScopedValue} for Phase 3 because
 * {@code ScopedValue} requires wrapping test-method invocation through a JUnit
 * {@code InvocationInterceptor}, which increases wiring complexity. {@code ScopedValue} is the
 * target design and will be introduced in the {@code ScopedContextExtension} (Phase 6/support).
 *
 * <p>SDET concept: tests set context once per test; filters read it transparently.
 * Scenarios never touch Authorization or X-Correlation-ID headers directly.
 */
package lab.paymentquality.apitest.core.context;
