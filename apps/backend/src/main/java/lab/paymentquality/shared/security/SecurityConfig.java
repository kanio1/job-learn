package lab.paymentquality.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain statusFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/status")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain testEndpointPassThroughFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(new OrRequestMatcher(
                        PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/test/reset"),
                        PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/test/seed")))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/status").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders").hasAuthority(Authorities.MERCHANT_PAYMENTS_CREATE)
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/authorize").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/capture").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/cancel").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/refund").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/evidence").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
                        .requestMatchers(HttpMethod.PATCH, "/api/merchants/*/payment-orders/*").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*/history").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_AUDIT, Authorities.MERCHANT_PAYMENTS_READ, Authorities.PLATFORM_PAYMENTS_READ)
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*/evidence").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_LIFECYCLE, Authorities.PLATFORM_PAYMENTS_AUDIT, Authorities.MERCHANT_PAYMENTS_READ, Authorities.PLATFORM_PAYMENTS_READ)
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/summary").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_READ, Authorities.PLATFORM_PAYMENTS_READ)
                        .requestMatchers(HttpMethod.HEAD, "/api/merchants/*/payment-orders/*").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_READ, Authorities.PLATFORM_PAYMENTS_READ)
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_READ, Authorities.PLATFORM_PAYMENTS_READ)
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders").hasAnyAuthority(Authorities.MERCHANT_PAYMENTS_READ, Authorities.PLATFORM_PAYMENTS_READ)
                        .requestMatchers(HttpMethod.POST, "/api/merchants").hasAuthority(Authorities.MERCHANTS_CREATE)
                        .requestMatchers(HttpMethod.GET, "/api/merchants").hasAuthority(Authorities.MERCHANTS_READ)
                        .requestMatchers(HttpMethod.GET, "/api/merchants/**").hasAuthority(Authorities.MERCHANTS_READ)
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/activate").hasAuthority(Authorities.MERCHANTS_UPDATE_STATUS)
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/suspend").hasAuthority(Authorities.MERCHANTS_UPDATE_STATUS)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return jwt -> {
            AbstractAuthenticationToken token = delegate.convert(jwt);
            String name = jwt.getClaimAsString("preferred_username");
            if (name == null || name.isBlank()) {
                name = jwt.getSubject(); // safe fallback, no error
            }
            return new JwtAuthenticationToken(jwt, token.getAuthorities(), name);
        };
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${payment-quality.security.authorized-party}") String expectedAzp) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new AuthorizedPartyValidator(expectedAzp)));
        return decoder;
    }

    @Bean
    @Profile({"dev", "test"})
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type",
                "Idempotency-Key", "If-Match", "If-None-Match",
                "X-Correlation-ID"));
        config.setExposedHeaders(List.of(
                "ETag", "Cache-Control", "Vary", "X-Correlation-ID",
                "Location", "Allow", "Accept-Patch",
                "Retry-After", "WWW-Authenticate", "Idempotency-Replayed", "Last-Modified"));
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
