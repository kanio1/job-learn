package lab.paymentquality.apitest.core.auth;

import java.util.List;
import java.util.Objects;

/**
 * Standard test personas for the payment API.
 *
 * <p>This is an Object Mother for {@link Identity} instances. Each persona maps to a real
 * Keycloak user in {@code payment-quality-realm.json}.
 *
 * <p>Token minting is strategy-driven. In offline unit tests (no containers), no factory is
 * installed and all personas fall back to {@link TokenFactory#placeholder()}. In live integration
 * tests, {@link #install(KeycloakTokenFactory)} is called by {@code ApiStack} before any spec
 * runs, causing all subsequent {@link Identity#token()} calls to return real JWTs via ROPC.
 *
 * <p><strong>Keycloak role note:</strong> The backend's {@code KeycloakRealmRoleConverter} maps
 * LEAF roles from the JWT's {@code realm_access.roles} to Spring Security authorities (e.g.,
 * {@code merchants:read → platform:merchants:read}). Composite role names like {@code PLATFORM_ADMIN}
 * are expanded by Keycloak into their leaf roles before embedding in the JWT — so the composite
 * names appear in the Identities logical description but not in the bearer token itself.
 *
 * <p>SDET learning: the personas in {@code roles()} reflect the composite role name assigned to
 * the Keycloak user (useful for readability in tests). The effective Spring Security authorities
 * used in {@code @PreAuthorize} checks are the mapped leaf roles.
 */
public final class Identities {

    /** Injected by {@code ApiStack} before live specs run. {@code null} in offline mode. */
    static volatile KeycloakTokenFactory KEYCLOAK_FACTORY;

    /**
     * Installs the live token factory. Called once per test run by {@code ApiStack.startContainers()}.
     * All persona token lookups after this call mint real JWTs from Keycloak via ROPC.
     */
    public static void install(KeycloakTokenFactory factory) {
        KEYCLOAK_FACTORY = Objects.requireNonNull(factory);
    }

    /** No Authorization header — for {@code GET /api/status} and public endpoints. */
    public static final Identity ANONYMOUS = Identity.anonymous();

    /**
     * Platform admin — Keycloak user: {@code platform.admin}.
     * Composite role {@code PLATFORM_ADMIN} expands to leaf roles covering all platform operations.
     */
    public static Identity platformAdmin() {
        return Identity.of("platform-admin",
                List.of("PLATFORM_ADMIN"),
                "PLATFORM_TENANT",
                keycloakOrPlaceholder("platform.admin", "platform.admin"));
    }

    /**
     * Tenant admin for the given tenant — Keycloak user: {@code tenant.admin}.
     * Composite role {@code TENANT_ADMIN} grants merchant management within a tenant boundary.
     *
     * <p>Note: in live mode the real Keycloak user has tenant {@code TENANT_ALPHA}; the
     * {@code tenantId} parameter is used only for the logical name and {@code tenantId()} metadata.
     * Tests that require exact tenant binding need dedicated realm users (Phase 7+).
     */
    public static Identity tenantAdmin(String tenantId) {
        return Identity.of("tenant-admin@" + tenantId,
                List.of("TENANT_ADMIN"),
                tenantId,
                keycloakOrPlaceholder("tenant.admin", "tenant.admin"));
    }

    /**
     * Support agent — Keycloak user: {@code support.agent}.
     * Composite role {@code SUPPORT_AGENT} grants read-only access across tenants.
     */
    public static Identity supportAgent() {
        return Identity.of("support-agent",
                List.of("SUPPORT_AGENT"),
                null,
                keycloakOrPlaceholder("support.agent", "support.agent"));
    }

    /**
     * Merchant manager — Keycloak user: {@code merchant.manager}.
     * Composite role {@code MERCHANT_MANAGER} grants payment order creation and lifecycle ops
     * for the assigned merchant.
     */
    public static Identity merchantManager() {
        return Identity.of("merchant-manager",
                List.of("MERCHANT_MANAGER"),
                null,
                keycloakOrPlaceholder("merchant.manager", "merchant.manager"));
    }

    /**
     * Payment order creator scoped to seeded MERCHANT_ALPHA_001 — Keycloak user:
     * {@code merchant.alpha.creator}.
     *
     * <p>Composite role {@code MERCHANT_MANAGER} expands to {@code merchant:payments:create},
     * {@code merchant:payments:read}, and {@code merchant:payments:lifecycle}.
     * The Keycloak user's {@code merchant_id} attribute is
     * {@code "00000000-0000-0000-0000-0000000000b1"} (the exact UUID of seeded MERCHANT_ALPHA_001),
     * which satisfies the controller's {@code merchant_id} JWT claim check for that merchant.
     *
     * <p>This persona returns 403 if used against any other merchant UUID (e.g. MERCHANT_ALPHA_002).
     * Use it only for {@code POST /api/merchants/MERCHANT_ALPHA_001_ID/payment-orders} tests.
     * Added in Phase 7B to unblock payment order create.
     */
    public static Identity seededMerchantCreator() {
        return Identity.of("seeded-merchant-alpha-001-creator",
                List.of("MERCHANT_MANAGER"),
                null,
                keycloakOrPlaceholder("merchant.alpha.creator", "merchant.alpha.creator"));
    }

    /**
     * Platform payment reader — Keycloak user: {@code platform.payment.reader}.
     * Direct leaf role {@code platform:payments:read} grants cross-tenant payment order reads.
     *
     * <p>Used as a read-only persona for payment order GET tests where any tenant is acceptable.
     */
    public static Identity merchantReader(String merchantId) {
        return Identity.of("merchant-reader@" + merchantId,
                List.of("platform:payments:read"),
                null,
                keycloakOrPlaceholder("platform.payment.reader", "platform.payment.reader"));
    }

    /**
     * Payment lifecycle operator — Keycloak user: {@code merchant.payment.lifecycle}.
     * Direct roles: {@code merchant:payments:read}, {@code merchant:payments:create},
     * {@code merchant:payments:lifecycle}.
     *
     * <p>Used for payment creator and lifecycle tests. Note: the Keycloak user has placeholder
     * tenant/merchant IDs — live tests with real merchant IDs require Phase 7 seed data.
     */
    public static Identity paymentCreator(String merchantId) {
        return Identity.of("payment-creator@" + merchantId,
                List.of("merchant:payments:create"),
                null,
                keycloakOrPlaceholder("merchant.payment.lifecycle", "merchant.payment.lifecycle"));
    }

    /**
     * Payment lifecycle operator — same Keycloak user as {@link #paymentCreator}.
     */
    public static Identity paymentLifecycleOperator(String merchantId) {
        return Identity.of("payment-lifecycle@" + merchantId,
                List.of("merchant:payments:lifecycle"),
                null,
                keycloakOrPlaceholder("merchant.payment.lifecycle", "merchant.payment.lifecycle"));
    }

    /**
     * Denied user — Keycloak user: {@code merchant.denied}.
     * No realm roles assigned. Valid JWT, but all protected endpoints return 403.
     *
     * <p>Safe for auth smoke tests: {@code @PreAuthorize} fires before any service logic,
     * so no tenant resolution or database access occurs.
     */
    public static Identity denied() {
        return Identity.of("merchant-denied",
                List.of(),
                null,
                keycloakOrPlaceholder("merchant.denied", "merchant.denied"));
    }

    private static TokenFactory keycloakOrPlaceholder(String username, String password) {
        KeycloakTokenFactory factory = KEYCLOAK_FACTORY;
        if (factory != null) {
            return factory.forUser(username, password);
        }
        return TokenFactory.placeholder();
    }

    private Identities() {}
}
