package lab.paymentquality.apitest.core.context;

import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.auth.Identity;
import lab.paymentquality.apitest.core.data.CorrelationIds;

import java.util.Objects;

/**
 * Immutable value object carrying per-test execution context.
 *
 * <p>Contains the correlation ID and identity for one test. {@link Ctx} stores the current
 * instance per thread; REST Assured filters read it to inject HTTP headers automatically.
 *
 * <p>SDET learning: binding correlation ID to the test context (not generated per-request)
 * means all requests within a single test share the same correlation ID — making backend log
 * searches trivial: one grep by correlation ID shows the full test execution trace.
 */
public record TestContext(String correlationId, Identity identity, String scenarioName) {

    public TestContext {
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(identity, "identity");
    }

    /** Creates a context with a generated correlation ID for the given identity. */
    public static TestContext of(Identity identity) {
        return new TestContext(
                CorrelationIds.generate(identity.logicalName()),
                identity,
                null);
    }

    /** Creates a context with an explicit correlation ID and identity. */
    public static TestContext of(String correlationId, Identity identity) {
        return new TestContext(correlationId, identity, null);
    }

    /** Creates a fully specified context including a scenario name for debugging. */
    public static TestContext of(String correlationId, Identity identity, String scenarioName) {
        return new TestContext(correlationId, identity, scenarioName);
    }

    /** Convenience: anonymous context for public endpoints (no Authorization header needed). */
    public static TestContext anonymous() {
        return of(CorrelationIds.generate("anon"), Identities.ANONYMOUS);
    }
}
