package lab.paymentquality.apitest.core.context;

/**
 * Per-thread holder for the current {@link TestContext}.
 *
 * <p>Usage:
 * <pre>{@code
 *   // In @BeforeEach or JUnit extension:
 *   Ctx.set(TestContext.of(Identities.platformAdmin()));
 *
 *   // In AuthFilter / CorrelationFilter (called by REST Assured on each request):
 *   TestContext ctx = Ctx.currentOrNull(); // null-safe for tests that skip context
 *
 *   // In @AfterEach or extension afterEach:
 *   Ctx.clear();
 * }</pre>
 *
 * <p>Design note: {@code ThreadLocal} is used instead of {@code ScopedValue<TestContext>}
 * because {@code ScopedValue} requires wrapping the test-method invocation body inside
 * {@code ScopedValue.where(...).run(...)}, which needs a JUnit {@code InvocationInterceptor}.
 * That adds wiring complexity deferred to Phase 6 ({@code ScopedContextExtension}).
 * {@code ThreadLocal} is safe for JUnit 6's default single-threaded test execution.
 *
 * <p>SDET learning: the ThreadLocal pattern is a controlled form of implicit context passing —
 * filters can read it without scenarios having to pass it through every method call.
 */
public final class Ctx {

    private static final ThreadLocal<TestContext> HOLDER = new ThreadLocal<>();

    /** Sets the context for the current thread. Call in {@code @BeforeEach} or a JUnit extension. */
    public static void set(TestContext context) {
        if (context == null) throw new IllegalArgumentException("TestContext must not be null");
        HOLDER.set(context);
    }

    /**
     * Returns the current context.
     *
     * @throws IllegalStateException if no context has been set on this thread
     */
    public static TestContext current() {
        TestContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new IllegalStateException(
                    "No TestContext set on current thread. " +
                    "Call Ctx.set(TestContext.of(...)) in @BeforeEach or a JUnit extension.");
        }
        return ctx;
    }

    /**
     * Returns the current context or {@code null} if none is set.
     * Filters use this to degrade gracefully when called outside a test context.
     */
    public static TestContext currentOrNull() {
        return HOLDER.get();
    }

    /** Clears the context for the current thread. Call in {@code @AfterEach} or after test cleanup. */
    public static void clear() {
        HOLDER.remove();
    }

    /** Returns {@code true} if a context is currently set on this thread. */
    public static boolean isSet() {
        return HOLDER.get() != null;
    }

    private Ctx() {}
}
