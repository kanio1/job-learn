package lab.paymentquality.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/status").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders").hasAuthority("merchant:payments:create")
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/authorize").hasAnyAuthority("merchant:payments:lifecycle", "platform:payments:lifecycle")
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/capture").hasAnyAuthority("merchant:payments:lifecycle", "platform:payments:lifecycle")
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/cancel").hasAnyAuthority("merchant:payments:lifecycle", "platform:payments:lifecycle")
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders/*/refund").hasAnyAuthority("merchant:payments:lifecycle", "platform:payments:lifecycle")
                        .requestMatchers(HttpMethod.PATCH, "/api/merchants/*/payment-orders/*").hasAnyAuthority("merchant:payments:lifecycle", "platform:payments:lifecycle")
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*/history").hasAnyAuthority("merchant:payments:lifecycle", "platform:payments:lifecycle", "platform:payments:audit", "merchant:payments:read", "platform:payments:read")
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/summary").hasAnyAuthority("merchant:payments:read", "platform:payments:read")
                        .requestMatchers(HttpMethod.HEAD, "/api/merchants/*/payment-orders/*").hasAnyAuthority("merchant:payments:read", "platform:payments:read")
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*").hasAnyAuthority("merchant:payments:read", "platform:payments:read")
                        .requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders").hasAnyAuthority("merchant:payments:read", "platform:payments:read")
                        .requestMatchers(HttpMethod.POST, "/api/merchants").hasAuthority("platform:merchants:create")
                        .requestMatchers(HttpMethod.GET, "/api/merchants").hasAuthority("platform:merchants:read")
                        .requestMatchers(HttpMethod.GET, "/api/merchants/**").hasAuthority("platform:merchants:read")
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/activate").hasAuthority("platform:merchants:update-status")
                        .requestMatchers(HttpMethod.POST, "/api/merchants/*/suspend").hasAuthority("platform:merchants:update-status")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    @Bean
    @Profile({"dev", "test"})
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "If-Match", "X-Correlation-ID"));
        config.setExposedHeaders(List.of("ETag", "Cache-Control", "Vary", "X-Correlation-ID", "Location"));
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
