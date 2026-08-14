package lab.paymentquality.testsupport;

import lab.paymentquality.shared.security.AuthorizedPartyValidator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * JwtDecoder that validates {@code iss} against the TLS-lab issuer string while still
 * using the in-memory test RSA key. Teaches that {@code issuer-uri} is an exact match:
 * HTTP Keycloak {@code iss} is not accepted when the resource server is on {@code tls-lab}.
 */
@TestConfiguration
public class TlsLabIssuerJwtTestConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestJwtSupport.publicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(TestJwtSupport.TLS_LAB_ISSUER),
                new AuthorizedPartyValidator(TestJwtSupport.EXPECTED_AZP)));
        return decoder;
    }
}
