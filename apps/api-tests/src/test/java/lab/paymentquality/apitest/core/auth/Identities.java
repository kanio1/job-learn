package lab.paymentquality.apitest.core.auth;

import java.util.List;

/**
 * Standard test personas for the payment API.
 *
 * <p>This is an Object Mother for {@link Identity} instances. Each persona maps to a real
 * Keycloak user that will exist in {@code payment-quality-realm.json} (Phase 6).
 *
 * <p>Token minting is deferred: all non-anonymous personas use {@link TokenFactory#placeholder()}
 * which returns a non-secret placeholder string. Replace with a real {@code KeycloakTokenFactory}
 * in Phase 6 — scenarios need no changes, only the factory binding changes.
 *
 * <p>SDET learning: standard personas cover the security matrix for authorization tests.
 * Each scenario declares which persona acts; {@link lab.paymentquality.apitest.core.http.AuthFilter}
 * injects the token transparently.
 *
 * <p><strong>Phase 6 TODO:</strong> inject a {@code KeycloakTokenFactory} instance here so
 * {@code token()} returns a real JWT. The simplest approach: replace {@code placeholder()} calls
 * with a factory obtained from the Testcontainers stack.
 */
public final class Identities {

    /** No Authorization header — for {@code GET /api/status}, seed/reset endpoints. */
    public static final Identity ANONYMOUS = Identity.anonymous();

    /**
     * Platform operator — Keycloak role: {@code platform:admin}.
     * Can manage tenants, view all merchants, all payment orders across tenants.
     */
    public static Identity platformAdmin() {
        return Identity.of("platform-admin",
                List.of("platform:admin"),
                null,
                TokenFactory.placeholder());
    }

    /**
     * Tenant admin for the given tenant — Keycloak role: {@code platform:tenant_admin}.
     * Can manage merchants within their tenant boundary.
     */
    public static Identity tenantAdmin(String tenantId) {
        return Identity.of("tenant-admin@" + tenantId,
                List.of("platform:tenant_admin"),
                tenantId,
                TokenFactory.placeholder());
    }

    /**
     * Merchant reader — read-only access to a specific merchant's payment orders.
     * Keycloak role: {@code platform:merchant_reader}.
     */
    public static Identity merchantReader(String merchantId) {
        return Identity.of("merchant-reader@" + merchantId,
                List.of("platform:merchant_reader"),
                null,
                TokenFactory.placeholder());
    }

    /**
     * Payment creator — can create payment orders for a merchant.
     * Keycloak role: {@code platform:payment_creator}.
     */
    public static Identity paymentCreator(String merchantId) {
        return Identity.of("payment-creator@" + merchantId,
                List.of("platform:payment_creator"),
                null,
                TokenFactory.placeholder());
    }

    /**
     * Payment lifecycle operator — can authorize, capture, cancel, refund.
     * Keycloak role: {@code platform:payment_lifecycle}.
     */
    public static Identity paymentLifecycleOperator(String merchantId) {
        return Identity.of("payment-lifecycle@" + merchantId,
                List.of("platform:payment_lifecycle"),
                null,
                TokenFactory.placeholder());
    }

    private Identities() {}
}
