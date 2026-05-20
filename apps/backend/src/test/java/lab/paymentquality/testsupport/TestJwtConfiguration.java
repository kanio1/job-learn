package lab.paymentquality.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@TestConfiguration
public class TestJwtConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestJwtSupport.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TestJwtSupport.ISSUER));
        return decoder;
    }
}
