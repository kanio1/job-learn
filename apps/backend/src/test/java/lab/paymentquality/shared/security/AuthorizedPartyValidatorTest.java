package lab.paymentquality.shared.security;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 5: The azp validator accepts only the expected authorized party, composed with defaults.
 * Tag: Feature: backend-authority-refactor, Property 5: azp accept/reject
 * Validates: Requirements 5.2, 5.3, 5.4
 */
@Tag("Feature: backend-authority-refactor")
class AuthorizedPartyValidatorTest {

    private static final String EXPECTED = "payment-quality-dashboard";
    private final AuthorizedPartyValidator validator = new AuthorizedPartyValidator(EXPECTED);

    // -----------------------------------------------------------------------
    // Property 5: azp validator accepts only the expected authorized party
    // -----------------------------------------------------------------------

    @Property(tries = 100)
    @Tag("Property 5: azp accept/reject")
    void property5_azpValidatorAcceptsOnlyExpectedParty(@ForAll String azpValue) {
        Jwt jwt = buildJwtWithAzp(azpValue);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        if (EXPECTED.equals(azpValue)) {
            assertThat(result.hasErrors())
                    .as("Expected azp should be accepted")
                    .isFalse();
        } else {
            assertThat(result.hasErrors())
                    .as("Non-expected azp '%s' should be rejected", azpValue)
                    .isTrue();
            assertThat(result.getErrors())
                    .extracting("errorCode")
                    .containsExactly("invalid_token");
        }
    }

    // -----------------------------------------------------------------------
    // Example-based unit tests
    // -----------------------------------------------------------------------

    @Test
    void acceptsExactExpectedAzp() {
        Jwt jwt = buildJwtWithAzp(EXPECTED);
        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsDifferentAzp() {
        Jwt jwt = buildJwtWithAzp("some-other-client");
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).extracting("errorCode").containsExactly("invalid_token");
    }

    @Test
    void rejectsAbsentAzpClaim() {
        // No azp claim at all — getClaimAsString returns null
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://issuer.example")
                .subject("user")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).extracting("errorCode").containsExactly("invalid_token");
    }

    @Test
    void rejectsEmptyAzp() {
        Jwt jwt = buildJwtWithAzp("");
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Jwt buildJwtWithAzp(String azp) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://issuer.example")
                .subject("user")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("azp", azp)
                .build();
    }
}
