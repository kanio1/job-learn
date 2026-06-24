package lab.paymentquality.apitest.scenarios;

import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.http.RequestSpecs;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Security smoke: verifies the backend enforces authentication and authorization on merchant endpoints.
 *
 * <p>These tests exercise the security filter chain WITHOUT touching the database or creating
 * any business data:
 * <ul>
 *   <li>Unauthenticated request → 401 (no Authorization header → authentication fails).</li>
 *   <li>{@code merchant.denied} user → 403 ({@code @PreAuthorize} fires before any service
 *       method; {@code TenantResolverService.resolve()} is never called).</li>
 * </ul>
 *
 * <p>HTTP concept: 401 vs 403 — the distinction matters.
 * 401 means "you have not identified yourself." 403 means "you are identified but not permitted."
 * The backend uses {@code Bearer} challenge for 401; the standard RFC 9110 mapping applies.
 *
 * <p>SDET learning: testing auth smoke without seed data is only safe when the security check
 * fires before the service layer. Here, Spring Security's {@code authorizeHttpRequests} filter
 * and {@code @PreAuthorize} both run before {@code MerchantController} method body executes.
 * This is a key invariant of the Spring Security filter chain order.
 */
@ApiTest
@DisplayName("Security smoke")
class SecuritySmokeSpec {

    @AfterEach
    void clearContext() {
        Ctx.clear();
    }

    @Test
    @DisplayName("GET /api/merchants without auth → 401")
    void unauthenticated_GET_merchants_returns_401() {
        RequestSpecs.anonymous()
                .when().get("/api/merchants")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("GET /api/merchants with denied user (no roles) → 403")
    void denied_user_GET_merchants_returns_403() {
        Ctx.set(TestContext.of(Identities.denied()));

        RequestSpecs.base()
                .when().get("/api/merchants")
                .then().statusCode(403);
    }
}
