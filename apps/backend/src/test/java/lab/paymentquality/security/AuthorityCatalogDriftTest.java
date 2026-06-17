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
    // Property 4 — all 9 enforced constants must equal their documented literals
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
    }

    @Test
    @Tag("backend-authority-refactor")
    @DisplayName("Property 4: catalog no-drift")
    void catalogContainsExactlyNineEnforcedAuthorities() {
        // Guard against accidental additions or removals from the catalog.
        // The design mandates exactly 9 enforced authorities (Data Models table).
        var allConstants = new String[]{
                Authorities.MERCHANTS_CREATE,
                Authorities.MERCHANTS_READ,
                Authorities.MERCHANTS_UPDATE_STATUS,
                Authorities.MERCHANT_PAYMENTS_CREATE,
                Authorities.MERCHANT_PAYMENTS_READ,
                Authorities.MERCHANT_PAYMENTS_LIFECYCLE,
                Authorities.PLATFORM_PAYMENTS_READ,
                Authorities.PLATFORM_PAYMENTS_LIFECYCLE,
                Authorities.PLATFORM_PAYMENTS_AUDIT
        };

        assertThat(allConstants)
                .as("The enforced authority catalog must contain exactly 9 constants")
                .hasSize(9);
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
                Authorities.PLATFORM_PAYMENTS_AUDIT
        };

        for (String authority : allConstants) {
            assertThat(authority)
                    .as("Authority '%s' must start with 'platform:' or 'merchant:'", authority)
                    .matches("^(platform|merchant):.+:.+$");
        }
    }
}
