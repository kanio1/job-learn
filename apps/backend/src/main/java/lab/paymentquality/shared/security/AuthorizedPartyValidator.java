package lab.paymentquality.shared.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class AuthorizedPartyValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID =
        new OAuth2Error("invalid_token",
            "Required authorized party (azp) is missing or not allowed", null);

    private final String expectedAzp;

    public AuthorizedPartyValidator(String expectedAzp) {
        this.expectedAzp = expectedAzp;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        return expectedAzp.equals(jwt.getClaimAsString("azp"))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(INVALID);
    }
}
