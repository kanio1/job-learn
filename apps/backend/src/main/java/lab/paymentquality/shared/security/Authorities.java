package lab.paymentquality.shared.security;

/**
 * Compile-time authority catalog for all enforced fine-grained authorities.
 *
 * <p>Constants are {@code public static final String} so they can be used both in Java
 * ({@code hasAuthority(Authorities.MERCHANTS_CREATE)}) and inside SpEL annotation strings
 * via concatenation
 * ({@code @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_CREATE + "')")}).
 *
 * <p>Exactly 9 enforced authorities are declared here. The known-but-unenforced
 * {@code merchant:payments:operate} authority is intentionally excluded; it lives as a
 * documented constant local to {@link KeycloakRealmRoleConverter}.
 */
public final class Authorities {

    // -------------------------------------------------------------------------
    // Merchant registry (platform-scoped)
    // -------------------------------------------------------------------------

    public static final String MERCHANTS_CREATE        = "platform:merchants:create";
    public static final String MERCHANTS_READ          = "platform:merchants:read";
    public static final String MERCHANTS_UPDATE_STATUS = "platform:merchants:update-status";

    // -------------------------------------------------------------------------
    // Payment orders (merchant-scoped)
    // -------------------------------------------------------------------------

    public static final String MERCHANT_PAYMENTS_CREATE    = "merchant:payments:create";
    public static final String MERCHANT_PAYMENTS_READ      = "merchant:payments:read";
    public static final String MERCHANT_PAYMENTS_LIFECYCLE = "merchant:payments:lifecycle";

    // -------------------------------------------------------------------------
    // Payment orders (platform-scoped)
    // -------------------------------------------------------------------------

    public static final String PLATFORM_PAYMENTS_READ      = "platform:payments:read";
    public static final String PLATFORM_PAYMENTS_LIFECYCLE = "platform:payments:lifecycle";
    public static final String PLATFORM_PAYMENTS_AUDIT     = "platform:payments:audit";

    private Authorities() {}
}
