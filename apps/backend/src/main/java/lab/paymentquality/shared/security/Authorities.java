package lab.paymentquality.shared.security;

/**
 * Compile-time authority catalog for all enforced fine-grained authorities.
 *
 * <p>Constants are {@code public static final String} so they can be used both in Java
 * ({@code hasAuthority(Authorities.MERCHANTS_CREATE)}) and inside SpEL annotation strings
 * via concatenation
 * ({@code @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_CREATE + "')")}).
 *
 * <p>Exactly 19 enforced authorities are declared here. The known-but-unenforced
 * {@code merchant:payments:operate} authority is intentionally excluded; it lives as a
 * documented constant local to {@link KeycloakRealmRoleConverter}.
 */
public final class Authorities {

    // -------------------------------------------------------------------------
    // Merchant registry (platform-scoped)
    // -------------------------------------------------------------------------

    public static final String MERCHANTS_CREATE           = "platform:merchants:create";
    public static final String MERCHANTS_READ             = "platform:merchants:read";
    public static final String MERCHANTS_UPDATE_STATUS    = "platform:merchants:update-status";
    public static final String MERCHANTS_UPDATE_RISK_FLAG = "platform:merchants:update-risk-flag";

    // -------------------------------------------------------------------------
    // Payment orders (merchant-scoped)
    // -------------------------------------------------------------------------

    public static final String MERCHANT_PAYMENTS_CREATE    = "merchant:payments:create";
    public static final String MERCHANT_PAYMENTS_READ      = "merchant:payments:read";
    public static final String MERCHANT_PAYMENTS_LIFECYCLE = "merchant:payments:lifecycle";

    // -------------------------------------------------------------------------
    // Payment orders (platform-scoped)
    // -------------------------------------------------------------------------

    public static final String PLATFORM_PAYMENTS_READ         = "platform:payments:read";
    public static final String PLATFORM_PAYMENTS_LIFECYCLE    = "platform:payments:lifecycle";
    public static final String PLATFORM_PAYMENTS_AUDIT        = "platform:payments:audit";
    public static final String PLATFORM_PAYMENT_NOTES_READ    = "platform:payments:notes:read";
    public static final String PLATFORM_PAYMENT_NOTES_CREATE  = "platform:payments:notes:create";

    // -------------------------------------------------------------------------
    // Global audit log reads
    // -------------------------------------------------------------------------

    public static final String PLATFORM_AUDIT_READ = "platform:audit:read";
    public static final String TENANT_AUDIT_READ   = "tenant:audit:read";

    // -------------------------------------------------------------------------
    // Tenant settings (platform-scoped)
    // -------------------------------------------------------------------------

    public static final String TENANT_SETTINGS_READ   = "platform:tenant:settings:read";
    public static final String TENANT_SETTINGS_UPDATE = "platform:tenant:settings:update";

    // -------------------------------------------------------------------------
    // User management (platform-scoped)
    // -------------------------------------------------------------------------

    public static final String PLATFORM_USERS_READ         = "platform:users:read";
    public static final String PLATFORM_USERS_CREATE       = "platform:users:create";
    public static final String PLATFORM_USERS_UPDATE       = "platform:users:update";
    public static final String PLATFORM_USERS_ASSIGN_ROLES = "platform:users:assign-roles";

    // -------------------------------------------------------------------------
    // User management (tenant-scoped)
    // -------------------------------------------------------------------------

    public static final String TENANT_USERS_READ         = "tenant:users:read";
    public static final String TENANT_USERS_CREATE       = "tenant:users:create";
    public static final String TENANT_USERS_UPDATE       = "tenant:users:update";
    public static final String TENANT_USERS_ASSIGN_ROLES = "tenant:users:assign-roles";

    private Authorities() {}
}
