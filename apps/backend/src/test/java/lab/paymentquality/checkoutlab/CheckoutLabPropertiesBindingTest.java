package lab.paymentquality.checkoutlab;

import lab.paymentquality.checkoutlab.internal.config.CheckoutLabProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutLabPropertiesBindingTest {

    @TestConfiguration
    @EnableConfigurationProperties(CheckoutLabProperties.class)
    static class CheckoutLabPropertiesTestConfiguration {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CheckoutLabPropertiesTestConfiguration.class);

    @Test
    void bindsExplicitCheckoutLabSecrets() {
        contextRunner
                .withPropertyValues(
                        "app.checkout-lab.enabled=true",
                        "app.checkout-lab.oauth-client-id=checkout-lab-merchant",
                        "app.checkout-lab.oauth-client-secret=custom-oauth",
                        "app.checkout-lab.hmac-secret=custom-hmac",
                        "app.checkout-lab.signature-tolerance-seconds=120")
                .run(context -> {
                    CheckoutLabProperties properties = context.getBean(CheckoutLabProperties.class);
                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.oauthClientId()).isEqualTo("checkout-lab-merchant");
                    assertThat(properties.oauthClientSecret()).isEqualTo("custom-oauth");
                    assertThat(properties.hmacSecret()).isEqualTo("custom-hmac");
                    assertThat(properties.signatureToleranceSeconds()).isEqualTo(120L);
                    assertThat(properties.oauthClientSecret()).isNotEqualTo(properties.hmacSecret());
                });
    }

    @Test
    void appliesSafeDefaultsWhenOptionalFieldsAreOmitted() {
        contextRunner
                .withPropertyValues("app.checkout-lab.enabled=false")
                .run(context -> {
                    CheckoutLabProperties properties = context.getBean(CheckoutLabProperties.class);
                    assertThat(properties.enabled()).isFalse();
                    assertThat(properties.oauthClientId()).isEqualTo("checkout-lab-merchant");
                    assertThat(properties.oauthClientSecret()).isEqualTo("change-me");
                    assertThat(properties.hmacSecret()).isEqualTo("change-me-too");
                    assertThat(properties.signatureToleranceSeconds()).isEqualTo(300L);
                });
    }
}
