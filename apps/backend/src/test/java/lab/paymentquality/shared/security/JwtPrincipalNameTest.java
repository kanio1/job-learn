package lab.paymentquality.shared.security;

import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based and example tests for the {@code jwtAuthenticationConverter()} logic
 * defined in {@link SecurityConfig}.
 *
 * <p>The converter is a pure function (no Spring context needed) — it is instantiated
 * directly, exactly as {@code SecurityConfig} constructs it.
 *
 * <p><strong>Property 6: Principal name is {@code preferred_username} with a safe
 * {@code sub} fallback, authorities unchanged.</strong>
 * <ul>
 *   <li>When {@code preferred_username} is present and non-blank → principal name equals that value.</li>
 *   <li>When {@code preferred_username} is absent → principal name equals {@code sub}.</li>
 *   <li>When {@code preferred_username} is blank/whitespace → principal name equals {@code sub}.</li>
 *   <li>Authorities are always exactly what {@link KeycloakRealmRoleConverter} produces —
 *       independent of the principal-name configuration.</li>
 * </ul>
 *
 * <p><strong>Validates: Requirements 4.2, 4.3, 4.4</strong>
 */
@Tag("Feature: backend-authority-refactor, Property 6: principal name derivation")
class JwtPrincipalNameTest {

    // -------------------------------------------------------------------------
    // SUT — instantiated exactly as SecurityConfig does it
    // -------------------------------------------------------------------------

    /**
     * Builds the same converter that {@code SecurityConfig.jwtAuthenticationConverter()} returns,
     * without involving the Spring context.
     */
    private static Converter<Jwt, AbstractAuthenticationToken> buildConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return jwt -> {
            AbstractAuthenticationToken token = delegate.convert(jwt);
            String name = jwt.getClaimAsString("preferred_username");
            if (name == null || name.isBlank()) {
                name = jwt.getSubject();
            }
            return new JwtAuthenticationToken(jwt, token.getAuthorities(), name);
        };
    }

    private static final Converter<Jwt, AbstractAuthenticationToken> CONVERTER = buildConverter();
    private static final KeycloakRealmRoleConverter ROLE_CONVERTER = new KeycloakRealmRoleConverter();

    /** A small fixed set of realm roles used to populate test tokens. */
    private static final List<String> SOME_ROLES =
            List.of("merchants:create", "merchant:payments:read", "platform:payments:audit");

    // -------------------------------------------------------------------------
    // JWT builder helpers
    // -------------------------------------------------------------------------

    private static Jwt.Builder baseBuilder(String sub) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(sub)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600));
    }

    /** Build a Jwt with {@code preferred_username} present. */
    private static Jwt jwtWithPreferredUsername(String sub, String preferredUsername, List<String> roles) {
        return baseBuilder(sub)
                .claim("preferred_username", preferredUsername)
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    /** Build a Jwt WITHOUT the {@code preferred_username} claim. */
    private static Jwt jwtWithoutPreferredUsername(String sub, List<String> roles) {
        return baseBuilder(sub)
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    /** Build a Jwt with {@code preferred_username} set to a blank/whitespace-only string. */
    private static Jwt jwtWithBlankPreferredUsername(String sub, String blankValue, List<String> roles) {
        return baseBuilder(sub)
                .claim("preferred_username", blankValue)
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    /**
     * Helper to compute the expected business authorities independently using the role converter.
     * Excludes any {@link FactorGrantedAuthority} instances, which are added by
     * {@code JwtAuthenticationConverter} in Spring Security 7 as part of MFA factor tracking
     * (e.g. {@code FACTOR_BEARER}) and are not part of the authorization logic.
     */
    private static Set<String> businessAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .filter(a -> !(a instanceof FactorGrantedAuthority))
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /** Helper to compute the expected authorities independently using the role converter. */
    private static Set<String> expectedAuthorities(List<String> roles) {
        Jwt jwt = jwtWithoutPreferredUsername("any-sub", roles);
        return ROLE_CONVERTER.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // =========================================================================
    // Property 6a — preferred_username present and non-blank → used as principal name
    //
    // Feature: backend-authority-refactor, Property 6: principal name derivation
    // Validates: Requirements 4.2, 4.4
    // =========================================================================

    /**
     * Property 6a: when {@code preferred_username} is present and non-blank,
     * the principal name equals {@code preferred_username}, and authorities
     * equal {@link KeycloakRealmRoleConverter} output (unchanged by name config).
     *
     * <p><b>Validates: Requirements 4.2, 4.4</b>
     */
    @Property(tries = 100)
    @Label("Feature: backend-authority-refactor, Property 6: principal name derivation")
    @net.jqwik.api.Tag("Feature: backend-authority-refactor, Property 6: principal name derivation")
    void property6a_preferredUsernamePresent_usedAsPrincipalName(
            @ForAll("nonBlankStrings") String sub,
            @ForAll("nonBlankStrings") String preferredUsername,
            @ForAll("realmRoleLists") List<String> roles) {

        Jwt jwt = jwtWithPreferredUsername(sub, preferredUsername, roles);
        AbstractAuthenticationToken token = CONVERTER.convert(jwt);

        assertThat(token.getName())
                .as("Principal name must equal preferred_username when present and non-blank")
                .isEqualTo(preferredUsername);

        Set<String> actualAuthorities = businessAuthorities(token.getAuthorities());
        Set<String> expected = expectedAuthorities(roles);

        assertThat(actualAuthorities)
                .as("Authorities must equal KeycloakRealmRoleConverter output, unaffected by name config")
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    // =========================================================================
    // Property 6b — preferred_username absent → falls back to sub
    //
    // Feature: backend-authority-refactor, Property 6: principal name derivation
    // Validates: Requirements 4.3, 4.4
    // =========================================================================

    /**
     * Property 6b: when {@code preferred_username} is absent,
     * the principal name equals {@code sub} (safe fallback, no error),
     * and authorities are unchanged.
     *
     * <p><b>Validates: Requirements 4.3, 4.4</b>
     */
    @Property(tries = 100)
    @Label("Feature: backend-authority-refactor, Property 6: principal name derivation")
    @net.jqwik.api.Tag("Feature: backend-authority-refactor, Property 6: principal name derivation")
    void property6b_preferredUsernameAbsent_fallsBackToSub(
            @ForAll("nonBlankStrings") String sub,
            @ForAll("realmRoleLists") List<String> roles) {

        Jwt jwt = jwtWithoutPreferredUsername(sub, roles);
        AbstractAuthenticationToken token = CONVERTER.convert(jwt);

        assertThat(token.getName())
                .as("Principal name must equal sub when preferred_username is absent")
                .isEqualTo(sub);

        Set<String> actualAuthorities = businessAuthorities(token.getAuthorities());
        Set<String> expected = expectedAuthorities(roles);

        assertThat(actualAuthorities)
                .as("Authorities must equal KeycloakRealmRoleConverter output, unaffected by fallback")
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    // =========================================================================
    // Property 6c — preferred_username blank/whitespace → falls back to sub
    //
    // Feature: backend-authority-refactor, Property 6: principal name derivation
    // Validates: Requirements 4.3, 4.4
    // =========================================================================

    /**
     * Property 6c: when {@code preferred_username} is present but blank (empty string
     * or whitespace only), the principal name equals {@code sub} (safe fallback),
     * and authorities are unchanged.
     *
     * <p><b>Validates: Requirements 4.3, 4.4</b>
     */
    @Property(tries = 100)
    @Label("Feature: backend-authority-refactor, Property 6: principal name derivation")
    @net.jqwik.api.Tag("Feature: backend-authority-refactor, Property 6: principal name derivation")
    void property6c_preferredUsernameBlank_fallsBackToSub(
            @ForAll("nonBlankStrings") String sub,
            @ForAll("blankStrings") String blankPreferredUsername,
            @ForAll("realmRoleLists") List<String> roles) {

        Jwt jwt = jwtWithBlankPreferredUsername(sub, blankPreferredUsername, roles);
        AbstractAuthenticationToken token = CONVERTER.convert(jwt);

        assertThat(token.getName())
                .as("Principal name must equal sub when preferred_username is blank ('%s')",
                        blankPreferredUsername)
                .isEqualTo(sub);

        Set<String> actualAuthorities = businessAuthorities(token.getAuthorities());
        Set<String> expected = expectedAuthorities(roles);

        assertThat(actualAuthorities)
                .as("Authorities must equal KeycloakRealmRoleConverter output, unaffected by blank fallback")
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    // =========================================================================
    // @Example — explicit fallback: no preferred_username → sub used as name
    //
    // Validates: Requirements 4.3
    // =========================================================================

    /**
     * Explicit example: a token with no {@code preferred_username} claim uses
     * {@code sub} as the principal name without throwing.
     *
     * <p><b>Validates: Requirement 4.3</b>
     */
    @Example
    void example_noPreferredUsername_subUsedAsPrincipalName() {
        String sub = "user-id-12345";
        Jwt jwt = jwtWithoutPreferredUsername(sub, SOME_ROLES);

        AbstractAuthenticationToken token = CONVERTER.convert(jwt);

        assertThat(token.getName())
                .as("sub must be used as principal name when preferred_username is absent")
                .isEqualTo(sub);
    }

    // =========================================================================
    // @Example — authorities equal KeycloakRealmRoleConverter output independently
    //
    // Validates: Requirements 4.4
    // =========================================================================

    /**
     * Explicit example: authorities produced by the full converter equal exactly what
     * {@link KeycloakRealmRoleConverter} produces for the same roles, proving that
     * the principal-name configuration does not alter the authority set.
     *
     * <p><b>Validates: Requirement 4.4</b>
     */
    @Example
    void example_authoritiesEqualKeycloakRealmRoleConverterOutput_independentOfPrincipalName() {
        String sub = "user-id-99";
        String preferredUsername = "alice";
        List<String> roles = SOME_ROLES;

        // Compute expected authorities directly from KeycloakRealmRoleConverter
        Jwt baseJwt = jwtWithoutPreferredUsername(sub, roles);
        Collection<GrantedAuthority> expectedGrantedAuthorities = ROLE_CONVERTER.convert(baseJwt);
        Set<String> expectedAuthorityStrings = expectedGrantedAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Convert via the full jwtAuthenticationConverter with preferred_username set
        Jwt fullJwt = jwtWithPreferredUsername(sub, preferredUsername, roles);
        AbstractAuthenticationToken token = CONVERTER.convert(fullJwt);

        assertThat(businessAuthorities(token.getAuthorities()))
                .as("Authorities must equal KeycloakRealmRoleConverter output independently of principal name")
                .containsExactlyInAnyOrderElementsOf(expectedAuthorityStrings);

        assertThat(token.getName())
                .as("Principal name must be preferred_username when present and non-blank")
                .isEqualTo(preferredUsername);
    }

    // =========================================================================
    // Arbitraries / generators
    // =========================================================================

    /**
     * Generates non-blank strings for subjects and {@code preferred_username} values.
     * Restricted to printable ASCII to avoid JWT claim encoding corner cases.
     */
    @Provide
    Arbitrary<String> nonBlankStrings() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(40)
                .filter(s -> !s.isBlank());
    }

    /**
     * Generates blank strings: empty string or strings containing only whitespace characters.
     */
    @Provide
    Arbitrary<String> blankStrings() {
        Arbitrary<String> empty = Arbitraries.just("");
        Arbitrary<String> whitespaceOnly = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(10);
        return Arbitraries.oneOf(empty, whitespaceOnly);
    }

    /**
     * Generates lists of known realm role names (any non-empty subset of the 10 known roles,
     * plus the empty list) to verify authority preservation.
     */
    @Provide
    Arbitrary<List<String>> realmRoleLists() {
        List<String> knownRoles = List.of(
                "merchants:create",
                "merchants:read",
                "merchants:update-status",
                "merchant:payments:create",
                "merchant:payments:read",
                "merchant:payments:operate",
                "merchant:payments:lifecycle",
                "platform:payments:read",
                "platform:payments:lifecycle",
                "platform:payments:audit");
        return Arbitraries.subsetOf(knownRoles).map(java.util.ArrayList::new);
    }
}
