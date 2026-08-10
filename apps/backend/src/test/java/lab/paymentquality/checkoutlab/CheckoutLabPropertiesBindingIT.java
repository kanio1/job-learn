package lab.paymentquality.checkoutlab;

import lab.paymentquality.checkoutlab.internal.config.CheckoutLabProperties;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CheckoutLabPropertiesBindingIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_properties_binding_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    CheckoutLabProperties properties;

    @Test
    void testProfileBindsCheckoutLabDefaultsWithoutRealSecrets() {
        assertThat(properties.enabled()).isFalse();
        assertThat(properties.oauthClientId()).isEqualTo("checkout-lab-merchant");
        assertThat(properties.oauthClientSecret()).isEqualTo("test-oauth-secret");
        assertThat(properties.hmacSecret()).isEqualTo("test-hmac-secret");
        assertThat(properties.signatureToleranceSeconds()).isEqualTo(300L);
        assertThat(properties.oauthClientSecret()).isNotEqualTo(properties.hmacSecret());
    }
}
