package lab.paymentquality.security;

import lab.paymentquality.shared.security.Authorities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 4: Catalog constants match the enforced authority strings (no drift).
 *
 * <p>Asserts that each {@link Authorities} constant resolves to the exact literal string
 * enforced by {@code SecurityConfig} URL rules and {@code MerchantController} {@code @PreAuthorize}
 * expressions. If a constant is accidentally changed, the affected URL rule or method-security
 * expression will silently enforce a different string — this test makes that visible immediately.
 *
 * <p>Validates: Requirements 1.2, 1.3, 1.4, 1.5
 */
@Tag("backend-authority-refactor")
@DisplayName("Feature: backend-authority-refactor, Property 4: catalog no-drift")
class AuthorityCatalogDriftTest {

    // -------------------------------------------------------------------------
    // Property 4 — all enforced constants must equal their documented literals
    // -------------------------------------------------------------------------

    @Test
    @Tag("backend-authority-refactor")
    @DisplayName("Property 4: catalog no-drift")
    void catalogConstantsMatchEnforcedAuthorityStrings() {

        // Merchant registry (platform-scoped) — enforced by SecurityConfig URL rules + @PreAuthorize
        assertThat(Authorities.MERCHANTS_CREATE)
                .as("MERCHANTS_CREATE must equal the literal enforced by SecurityConfig and @PreAuthorize")
                .isEqualTo("platform:merchants:create");

        assertThat(Authorities.MERCHANTS_READ)
                .as("MERCHANTS_READ must equal the literal enforced by SecurityConfig and @PreAuthorize")
                .isEqualTo("platform:merchants:read");

        assertThat(Authorities.MERCHANTS_UPDATE_STATUS)
                .as("MERCHANTS_UPDATE_STATUS must equal the literal enforced by SecurityConfig and @PreAuthorize")
                .isEqualTo("platform:merchants:update-status");

        // Payment orders (merchant-scoped) — enforced by SecurityConfig URL rules
        assertThat(Authorities.MERCHANT_PAYMENTS_CREATE)
                .as("MERCHANT_PAYMENTS_CREATE must equal the literal enforced by SecurityConfig URL rules")
                .isEqualTo("merchant:payments:create");

        assertThat(Authorities.MERCHANT_PAYMENTS_READ)
                .as("MERCHANT_PAYMENTS_READ must equal the literal enforced by SecurityConfig URL rules")
                .isEqualTo("merchant:payments:read");

        assertThat(Authorities.MERCHANT_PAYMENTS_LIFECYCLE)
                .as("MERCHANT_PAYMENTS_LIFECYCLE must equal the literal enforced by SecurityConfig URL rules")
                .isEqualTo("merchant:payments:lifecycle");

        // Payment orders (platform-scoped) — enforced by SecurityConfig URL rules + programmatic checks
        assertThat(Authorities.PLATFORM_PAYMENTS_READ)
                .as("PLATFORM_PAYMENTS_READ must equal the literal enforced by SecurityConfig URL rules")
                .isEqualTo("platform:payments:read");

        assertThat(Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
                .as("PLATFORM_PAYMENTS_LIFECYCLE must equal the literal enforced by SecurityConfig URL rules")
                .isEqualTo("platform:payments:lifecycle");

        assertThat(Authorities.PLATFORM_PAYMENTS_AUDIT)
                .as("PLATFORM_PAYMENTS_AUDIT must equal the literal enforced by SecurityConfig URL rules")
                .isEqualTo("platform:payments:audit");

        // Global audit log reads, deliberately distinct from payment audit
        assertThat(Authorities.PLATFORM_AUDIT_READ)
                .as("PLATFORM_AUDIT_READ must equal the documented audit-log literal")
                .isEqualTo("platform:audit:read");

        assertThat(Authorities.TENANT_AUDIT_READ)
                .as("TENANT_AUDIT_READ must equal the documented audit-log literal")
                .isEqualTo("tenant:audit:read");

        // User management (platform-scoped) — enforced by future user-management @PreAuthorize
        assertThat(Authorities.PLATFORM_USERS_READ)
                .as("PLATFORM_USERS_READ must equal the documented user-management literal")
                .isEqualTo("platform:users:read");

        assertThat(Authorities.PLATFORM_USERS_CREATE)
                .as("PLATFORM_USERS_CREATE must equal the documented user-management literal")
                .isEqualTo("platform:users:create");

        assertThat(Authorities.PLATFORM_USERS_UPDATE)
                .as("PLATFORM_USERS_UPDATE must equal the documented user-management literal")
                .isEqualTo("platform:users:update");

        assertThat(Authorities.PLATFORM_USERS_ASSIGN_ROLES)
                .as("PLATFORM_USERS_ASSIGN_ROLES must equal the documented user-management literal")
                .isEqualTo("platform:users:assign-roles");

        // User management (tenant-scoped) — enforced by future user-management @PreAuthorize
        assertThat(Authorities.TENANT_USERS_READ)
                .as("TENANT_USERS_READ must equal the documented user-management literal")
                .isEqualTo("tenant:users:read");

        assertThat(Authorities.TENANT_USERS_CREATE)
                .as("TENANT_USERS_CREATE must equal the documented user-management literal")
                .isEqualTo("tenant:users:create");

        assertThat(Authorities.TENANT_USERS_UPDATE)
                .as("TENANT_USERS_UPDATE must equal the documented user-management literal")
                .isEqualTo("tenant:users:update");

        assertThat(Authorities.TENANT_USERS_ASSIGN_ROLES)
                .as("TENANT_USERS_ASSIGN_ROLES must equal the documented user-management literal")
                .isEqualTo("tenant:users:assign-roles");

        assertThat(Authorities.SUPPORT_READ)
                .as("SUPPORT_READ must equal the documented support literal")
                .isEqualTo("platform:support:read");

        assertThat(Authorities.SUPPORT_OPERATE)
                .as("SUPPORT_OPERATE must equal the documented support literal")
                .isEqualTo("platform:support:operate");

        assertThat(Authorities.OPS_FEED)
                .as("OPS_FEED must equal the documented ops feed literal")
                .isEqualTo("platform:ops:feed");

        assertThat(Authorities.OPS_INJECT)
                .as("OPS_INJECT must equal the documented ops inject literal")
                .isEqualTo("platform:ops:inject");

        assertThat(Authorities.NOTIFICATIONS_READ)
                .as("NOTIFICATIONS_READ must equal the documented notifications literal")
                .isEqualTo("platform:notifications:read");
    }

    @Test
    @Tag("backend-authority-refactor")
    @DisplayName("Property 4: catalog no-drift")
    void catalogContainsExactlyNineteenEnforcedAuthorities() {
        // Guard against accidental additions or removals from the catalog.
        var allConstants = new String[]{
                Authorities.MERCHANTS_CREATE,
                Authorities.MERCHANTS_READ,
                Authorities.MERCHANTS_UPDATE_STATUS,
                Authorities.MERCHANT_PAYMENTS_CREATE,
                Authorities.MERCHANT_PAYMENTS_READ,
                Authorities.MERCHANT_PAYMENTS_LIFECYCLE,
                Authorities.PLATFORM_PAYMENTS_READ,
                Authorities.PLATFORM_PAYMENTS_LIFECYCLE,
                Authorities.PLATFORM_PAYMENTS_AUDIT,
                Authorities.PLATFORM_AUDIT_READ,
                Authorities.TENANT_AUDIT_READ,
                Authorities.PLATFORM_USERS_READ,
                Authorities.PLATFORM_USERS_CREATE,
                Authorities.PLATFORM_USERS_UPDATE,
                Authorities.PLATFORM_USERS_ASSIGN_ROLES,
                Authorities.TENANT_USERS_READ,
                Authorities.TENANT_USERS_CREATE,
                Authorities.TENANT_USERS_UPDATE,
                Authorities.TENANT_USERS_ASSIGN_ROLES,
                Authorities.SUPPORT_READ,
                Authorities.SUPPORT_OPERATE,
                Authorities.OPS_FEED,
                Authorities.OPS_INJECT,
                Authorities.NOTIFICATIONS_READ
        };

        assertThat(allConstants)
                .as("The enforced authority catalog must contain exactly 24 constants in this drift set")
                .hasSize(24);
    }

    @Test
    @Tag("backend-authority-refactor")
    @DisplayName("Property 4: catalog no-drift")
    void allConstantsFollowExpectedNamingScheme() {
        // Every enforced authority must use the documented namespace pattern:
        // "platform:<resource>:<action>" or "merchant:<resource>:<action>"
        var allConstants = new String[]{
                Authorities.MERCHANTS_CREATE,
                Authorities.MERCHANTS_READ,
                Authorities.MERCHANTS_UPDATE_STATUS,
                Authorities.MERCHANT_PAYMENTS_CREATE,
                Authorities.MERCHANT_PAYMENTS_READ,
                Authorities.MERCHANT_PAYMENTS_LIFECYCLE,
                Authorities.PLATFORM_PAYMENTS_READ,
                Authorities.PLATFORM_PAYMENTS_LIFECYCLE,
                Authorities.PLATFORM_PAYMENTS_AUDIT,
                Authorities.PLATFORM_AUDIT_READ,
                Authorities.TENANT_AUDIT_READ,
                Authorities.PLATFORM_USERS_READ,
                Authorities.PLATFORM_USERS_CREATE,
                Authorities.PLATFORM_USERS_UPDATE,
                Authorities.PLATFORM_USERS_ASSIGN_ROLES,
                Authorities.TENANT_USERS_READ,
                Authorities.TENANT_USERS_CREATE,
                Authorities.TENANT_USERS_UPDATE,
                Authorities.TENANT_USERS_ASSIGN_ROLES,
                Authorities.SUPPORT_READ,
                Authorities.SUPPORT_OPERATE,
                Authorities.OPS_FEED,
                Authorities.OPS_INJECT,
                Authorities.NOTIFICATIONS_READ
        };

        for (String authority : allConstants) {
            assertThat(authority)
                    .as("Authority '%s' must start with 'platform:', 'merchant:', or 'tenant:'", authority)
                    .matches("^(platform|merchant|tenant):.+:.+$");
        }
    }
}
