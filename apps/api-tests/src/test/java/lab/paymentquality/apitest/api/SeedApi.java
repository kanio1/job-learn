package lab.paymentquality.apitest.api;

import io.restassured.response.Response;
import lab.paymentquality.apitest.core.http.RequestSpecs;

/**
 * Thin client for {@code /api/test/seed} and {@code /api/test/reset}.
 *
 * <p>These endpoints are gated by {@code @ConditionalOnProperty(app.testing.enabled=true)}
 * and {@code @Profile("!prod")} on the backend. They are active in the Testcontainers stack
 * because {@code BackendSupport} sets {@code APP_TESTING_ENABLED=true} and
 * {@code SPRING_PROFILES_ACTIVE=dev}.
 *
 * <p>Both endpoints are {@code permitAll} in {@code SecurityConfig} — no Authorization header required.
 * {@link RequestSpecs#anonymous()} is used deliberately; never use the authenticated spec here.
 *
 * <p>Seeded data (fixed UUIDs):
 * <ul>
 *   <li>Tenants: {@code PLATFORM_TENANT} (UUID ending in {@code a1}), {@code TENANT_ALPHA} ({@code a2}),
 *       {@code PLACEHOLDER_TENANT} ({@code a3}).</li>
 *   <li>Merchants: {@code MERCHANT_ALPHA_001} ({@code b1}), {@code MERCHANT_ALPHA_002} ({@code b2}),
 *       {@code MERCHANT_BETA_001} ({@code b3}) — all ACTIVE.</li>
 *   <li>Payment orders: 6 fixed + 98 pagination fixtures under MERCHANT_ALPHA_001 and MERCHANT_ALPHA_002.</li>
 * </ul>
 *
 * <p>Usage pattern in contract specs:
 * <pre>{@code
 *   @BeforeAll
 *   static void seedDb() { SeedApi.seed(); }
 *
 *   @AfterAll
 *   static void cleanUp() { SeedApi.reset(); }
 * }</pre>
 *
 * <p>SDET learning: calling seed/reset in {@code @BeforeAll}/{@code @AfterAll} is a deliberate
 * "database-as-fixture" strategy. Individual tests create unique entities (via {@link lab.paymentquality.apitest.core.data.UniqueReferences})
 * so they don't interfere with each other between spec methods.
 * Reset after the spec cleans up created records to prevent bleed between spec classes.
 */
public final class SeedApi {

    private SeedApi() {}

    /**
     * {@code POST /api/test/seed} — clears the database and loads the deterministic fixture dataset.
     *
     * <p>Idempotent: calling seed twice is safe; the second call clears and re-seeds.
     */
    public static void seed() {
        Response response = RequestSpecs.anonymous()
                .when()
                .post("/api/test/seed");
        int status = response.statusCode();
        if (status != 200) {
            throw new IllegalStateException(
                    "POST /api/test/seed returned " + status + " — backend may not have testing support enabled. " +
                    "Check APP_TESTING_ENABLED=true and SPRING_PROFILES_ACTIVE != prod in BackendSupport.");
        }
    }

    /**
     * {@code POST /api/test/reset} — clears all test data from the database.
     *
     * <p>Removes all payment orders, merchants, and tenants. Leaves the schema intact.
     */
    public static void reset() {
        Response response = RequestSpecs.anonymous()
                .when()
                .post("/api/test/reset");
        int status = response.statusCode();
        if (status != 200) {
            throw new IllegalStateException(
                    "POST /api/test/reset returned " + status + " — backend testing support may not be active.");
        }
    }
}
