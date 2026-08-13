package lab.paymentquality.checkoutlab.internal.config;

import lab.paymentquality.checkoutlab.internal.application.CheckoutLabAccessTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
class CheckoutLabSecurityConfiguration {

    @Bean
    CheckoutLabBearerAuthenticationFilter checkoutLabBearerAuthenticationFilter(
            CheckoutLabAccessTokenService accessTokenService) {
        return new CheckoutLabBearerAuthenticationFilter(accessTokenService);
    }

    @Bean
    FilterRegistrationBean<CheckoutLabBearerAuthenticationFilter> checkoutLabBearerFilterRegistration(
            CheckoutLabBearerAuthenticationFilter filter) {
        FilterRegistrationBean<CheckoutLabBearerAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns(
                "/api/checkout-lab/sessions",
                "/api/checkout-lab/sessions/*",
                "/api/checkout-lab/bookings",
                "/api/checkout-lab/bookings/*",
                "/api/checkout-lab/anomalies",
                "/api/checkout-lab/reset",
                "/api/checkout-lab/clock",
                "/api/checkout-lab/reconcile");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registration.setName("checkoutLabBearerAuthenticationFilter");
        return registration;
    }
}
