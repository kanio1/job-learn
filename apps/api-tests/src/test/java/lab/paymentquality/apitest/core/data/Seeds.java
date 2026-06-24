package lab.paymentquality.apitest.core.data;

/**
 * Deterministic seed data identifiers for live contract tests.
 *
 * <p>These constants mirror the backend's {@code Fixtures} class in the {@code testing} module,
 * without importing backend classes. They represent the fixed UUIDs and reference strings that
 * {@link lab.paymentquality.apitest.api.SeedApi#seed()} loads into the Testcontainers database.
 *
 * <p><strong>Why deterministic IDs?</strong> Stable, known UUIDs let contract tests assert against
 * seeded resources without first creating them via POST. This removes the auth complexity of
 * creation-first setup for read-only contract tests (e.g. GET, LIST, HEAD on payment orders).
 * The tradeoff is coupling: if {@code Fixtures} changes its IDs, these constants must be updated.
 *
 * <p><strong>SDET learning:</strong> fixed-seed pattern vs. API-created pattern.
 * Fixed-seed → fast, simple, no auth setup; brittle if fixture changes.
 * API-created → realistic, resilient; requires valid create credentials (which may not exist).
 * Phase 7A uses fixed-seed because payment order create credentials are not yet in the realm.
 *
 * <p>Category: data/test infrastructure.
 */
public final class Seeds {

    // -------------------------------------------------------------------------
    // Tenants
    // -------------------------------------------------------------------------

    /** Fixed UUID for PLATFORM_TENANT (all platform-scoped operations belong here). */
    public static final String PLATFORM_TENANT_ID  = "00000000-0000-0000-0000-0000000000a1";

    /** Fixed UUID for TENANT_ALPHA (standard tenant; owns MERCHANT_ALPHA_001 and _002). */
    public static final String TENANT_ALPHA_ID     = "00000000-0000-0000-0000-0000000000a2";

    // -------------------------------------------------------------------------
    // Merchants (all seeded in ACTIVE status)
    // -------------------------------------------------------------------------

    /** MERCHANT_ALPHA_001 — ACTIVE merchant owned by TENANT_ALPHA; has seeded payment orders. */
    public static final String MERCHANT_ALPHA_001_ID = "00000000-0000-0000-0000-0000000000b1";

    /** MERCHANT_ALPHA_002 — ACTIVE merchant owned by TENANT_ALPHA; has seeded payment orders. */
    public static final String MERCHANT_ALPHA_002_ID = "00000000-0000-0000-0000-0000000000b2";

    /** MERCHANT_BETA_001 — ACTIVE merchant owned by PLATFORM_TENANT; has one seeded payment order. */
    public static final String MERCHANT_BETA_001_ID  = "00000000-0000-0000-0000-0000000000b3";

    // -------------------------------------------------------------------------
    // Payment orders for MERCHANT_ALPHA_001
    // -------------------------------------------------------------------------

    /**
     * Payment order in CREATED status; 1100 PLN; clientOrderReference = "SEED-ALPHA-001-CREATED".
     * ETag: {@code "v0"} (version=0 → no state transitions applied).
     */
    public static final String PAYMENT_ORDER_ALPHA_001_CREATED_ID =
            "00000000-0000-0000-0000-0000000000c1";

    /**
     * Payment order in AUTHORIZED status; 2200 EUR; clientOrderReference = "SEED-ALPHA-001-AUTHORIZED".
     * ETag: {@code "v1"} (version=1 → one transition applied).
     */
    public static final String PAYMENT_ORDER_ALPHA_001_AUTHORIZED_ID =
            "00000000-0000-0000-0000-0000000000c2";

    /**
     * Payment order in CAPTURED status; 3300 USD; clientOrderReference = "SEED-ALPHA-001-CAPTURED".
     * ETag: {@code "v2"} (version=2 → two transitions applied).
     */
    public static final String PAYMENT_ORDER_ALPHA_001_CAPTURED_ID =
            "00000000-0000-0000-0000-0000000000c3";

    // -------------------------------------------------------------------------
    // Payment orders for MERCHANT_ALPHA_002
    // -------------------------------------------------------------------------

    /**
     * Payment order in CANCELLED status; 4400 PLN; clientOrderReference = "SEED-ALPHA-002-CANCELLED".
     */
    public static final String PAYMENT_ORDER_ALPHA_002_CANCELLED_ID =
            "00000000-0000-0000-0000-0000000000c4";

    /**
     * Payment order in REFUNDED status; 5500 EUR; clientOrderReference = "SEED-ALPHA-002-REFUNDED".
     */
    public static final String PAYMENT_ORDER_ALPHA_002_REFUNDED_ID =
            "00000000-0000-0000-0000-0000000000c5";

    // -------------------------------------------------------------------------
    // Payment orders for MERCHANT_BETA_001
    // -------------------------------------------------------------------------

    /**
     * Payment order in CREATED status; 6600 PLN; clientOrderReference = "SEED-BETA-001-CREATED".
     */
    public static final String PAYMENT_ORDER_BETA_001_CREATED_ID =
            "00000000-0000-0000-0000-0000000000c6";

    private Seeds() {}
}
