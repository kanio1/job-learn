package lab.paymentquality.shared.security;

import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests for {@link KeycloakRealmRoleConverter}.
 *
 * <p>These tests pin the CURRENT converter behavior before any R2 refactor. Each
 * property must be GREEN against the existing heuristic implementation.
 *
 * <p>Properties:
 * <ul>
 *   <li>Property 1: Known roles map to their documented authorities (task 3.2)</li>
 *   <li>Property 2: Unknown-role current behavior pin (task 3.4)</li>
 *   <li>Property 3: Absent or malformed realm_access/roles yields no authorities (task 3.3)</li>
 * </ul>
 *
 * <p>Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@Tag("backend-authority-refactor")
class KeycloakRealmRoleConverterTest {

    private static final KeycloakRealmRoleConverter CONVERTER = new KeycloakRealmRoleConverter();

    /** All 18 known realm role names mapped to their expected authority strings. */
    private static final Map<String, String> KNOWN_ROLE_MAPPING = Map.ofEntries(
            Map.entry("merchants:create",            "platform:merchants:create"),
            Map.entry("merchants:read",              "platform:merchants:read"),
            Map.entry("merchants:update-status",     "platform:merchants:update-status"),
            Map.entry("merchant:payments:create",    "merchant:payments:create"),
            Map.entry("merchant:payments:read",      "merchant:payments:read"),
            Map.entry("merchant:payments:operate",   "merchant:payments:operate"),
            Map.entry("merchant:payments:lifecycle", "merchant:payments:lifecycle"),
            Map.entry("platform:payments:read",      "platform:payments:read"),
            Map.entry("platform:payments:lifecycle", "platform:payments:lifecycle"),
            Map.entry("platform:payments:audit",     "platform:payments:audit"),
            Map.entry("platform:users:read",         "platform:users:read"),
            Map.entry("platform:users:create",       "platform:users:create"),
            Map.entry("platform:users:update",       "platform:users:update"),
            Map.entry("platform:users:assign-roles", "platform:users:assign-roles"),
            Map.entry("tenant:users:read",           "tenant:users:read"),
            Map.entry("tenant:users:create",         "tenant:users:create"),
            Map.entry("tenant:users:update",         "tenant:users:update"),
            Map.entry("tenant:users:assign-roles",   "tenant:users:assign-roles")
    );

    private static final Set<String> KNOWN_ROLES = KNOWN_ROLE_MAPPING.keySet();

    // -------------------------------------------------------------------------
    // JWT builder helpers
    // -------------------------------------------------------------------------

    /**
     * Build a minimal valid Jwt with the given claims map merged into the token.
     */
    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600));
        claims.forEach(builder::claim);
        return builder.build();
    }

    /**
     * Build a Jwt with {@code realm_access: {roles: <roleList>}}.
     */
    private static Jwt jwtWithRoles(List<?> roles) {
        return jwtWithClaims(Map.of("realm_access", Map.of("roles", roles)));
    }

    /**
     * Build a Jwt with {@code realm_access} set to an arbitrary non-Map object.
     */
    private static Jwt jwtWithRealmAccessValue(Object realmAccessValue) {
        return jwtWithClaims(Map.of("realm_access", realmAccessValue));
    }

    /**
     * Build a Jwt with {@code realm_access} as a map but with {@code roles} set to
     * an arbitrary non-Collection value.
     */
    private static Jwt jwtWithRolesValue(Object rolesValue) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", rolesValue);
        return jwtWithClaims(Map.of("realm_access", realmAccess));
    }

    /**
     * Build a Jwt with no claims at all — {@code realm_access} is absent.
     */
    private static Jwt jwtWithNoRealmAccess() {
        return jwtWithClaims(Map.of());
    }

    /**
     * Build a Jwt where {@code realm_access} is a map but has no {@code roles} key.
     */
    private static Jwt jwtWithRealmAccessButNoRoles() {
        return jwtWithClaims(Map.of("realm_access", Map.of("other_key", "some_value")));
    }

    // =========================================================================
    // Property 1 — Known roles map to their documented authorities
    //
    // Feature: backend-authority-refactor, Property 1: known-role mapping
    // Validates: Requirements 2.2, 2.6
    // =========================================================================

    // --- Example-based: each known role in isolation ---

    @Test
    void known_roles_each_produce_their_documented_authority() {
        for (var entry : KNOWN_ROLE_MAPPING.entrySet()) {
            Collection<GrantedAuthority> result = CONVERTER.convert(jwtWithRoles(List.of(entry.getKey())));
            assertThat(result)
                    .as("Role '%s' should produce authority '%s'", entry.getKey(), entry.getValue())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly(entry.getValue());
        }
    }

    @Test
    void all_known_roles_together_produce_all_authorities() {
        List<String> allRoles = new ArrayList<>(KNOWN_ROLES);
        Collection<GrantedAuthority> result = CONVERTER.convert(jwtWithRoles(allRoles));
        assertThat(result)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrderElementsOf(KNOWN_ROLE_MAPPING.values());
    }

    /**
     * Property 1 (PBT): for any non-empty subset of the known roles (any order),
     * the converter produces exactly the set of authorities those roles map to —
     * no extras, no missing.
     *
     * <p><b>Validates: Requirements 2.2, 2.6</b>
     */
    @Property(tries = 30)
    @Label("Feature: backend-authority-refactor, Property 1: known-role mapping")
    @net.jqwik.api.Tag("backend-authority-refactor")
    void property1_known_roles_produce_exactly_their_documented_authorities(
            @ForAll("nonEmptyKnownRoleSubsets") List<String> roles) {

        Set<String> expectedAuthorities = roles.stream()
                .map(KNOWN_ROLE_MAPPING::get)
                .collect(Collectors.toSet());

        Collection<GrantedAuthority> result = CONVERTER.convert(jwtWithRoles(roles));

        assertThat(result)
                .as("Known roles %s should produce exactly %s", roles, expectedAuthorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrderElementsOf(expectedAuthorities);
    }

    /**
     * Generates a non-empty subset of the known role names as a list (any order).
     */
    @Provide
    Arbitrary<List<String>> nonEmptyKnownRoleSubsets() {
        List<String> allRoles = new ArrayList<>(KNOWN_ROLES);
        return Arbitraries.subsetOf(allRoles)
                .filter(s -> !s.isEmpty())
                .map(ArrayList::new);
    }

    // =========================================================================
    // Property 2 — Unknown roles are ignored (fail-closed)
    //
    // Feature: backend-authority-refactor, Property 2: unknown-role handling
    // Validates: Requirements 2.7, 3.5
    //
    // After the R2 allowlist refactor (task 4.1 + 4.2), unknown roles produce
    // NO authority — the converter is fail-closed. The previous characterization
    // pinned "platform:<name>" but that behavior is now intentionally gone.
    // =========================================================================

    // --- Example-based companion tests (updated for fail-closed behavior) ---

    @Test
    void unknown_role_PLATFORM_ADMIN_produces_no_authority() {
        Collection<GrantedAuthority> result = CONVERTER.convert(jwtWithRoles(List.of("PLATFORM_ADMIN")));
        assertThat(result).isEmpty();
    }

    @Test
    void unknown_role_mixed_with_known_role_only_known_authority_appears() {
        Collection<GrantedAuthority> result = CONVERTER.convert(
                jwtWithRoles(List.of("merchants:create", "SOME_UNKNOWN_ROLE")));
        assertThat(result)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("platform:merchants:create");
    }

    /**
     * Property 2 (PBT): an unknown role produces NO authority (fail-closed).
     *
     * <p>For any role name not in the Known_Roles — mixed with any subset of Known_Roles —
     * the converter produces authorities only for the Known_Roles present and produces no
     * authority derived from the unknown name (in particular, never {@code platform:<unknown>}).
     *
     * <p><b>Validates: Requirements 2.7, 3.5</b>
     */
    @Property(tries = 30)
    @Label("Feature: backend-authority-refactor, Property 2: unknown-role handling")
    @net.jqwik.api.Tag("backend-authority-refactor")
    void property2_unknown_role_is_ignored_fail_closed(
            @ForAll("unknownRoleNames") String unknownRole,
            @ForAll("knownRoleSubsetsIncludingEmpty") List<String> knownRoles) {

        // Build input: the unknown role mixed with a (possibly empty) subset of known roles
        List<String> allRoles = new ArrayList<>(knownRoles);
        allRoles.add(unknownRole);

        // Expected: only the authorities for the known roles present — unknown contributes nothing
        Set<String> expectedAuthorities = knownRoles.stream()
                .map(KNOWN_ROLE_MAPPING::get)
                .collect(Collectors.toCollection(HashSet::new));

        Collection<GrantedAuthority> result = CONVERTER.convert(jwtWithRoles(allRoles));
        Set<String> actual = result.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Known roles must produce their exact mapped authority
        for (String knownRole : knownRoles) {
            String expectedAuth = KNOWN_ROLE_MAPPING.get(knownRole);
            assertThat(actual)
                    .as("Known role '%s' should produce authority '%s'", knownRole, expectedAuth)
                    .contains(expectedAuth);
        }

        // Unknown role must produce NO authority — never platform:<unknownRole>
        String platformPrefixed = "platform:" + unknownRole;
        assertThat(actual)
                .as("Unknown role '%s' must produce no authority (fail-closed); 'platform:%s' must not appear",
                        unknownRole, unknownRole)
                .doesNotContain(platformPrefixed);

        // The total set must equal exactly the known-role authorities — no extras
        assertThat(actual)
                .as("Authorities for input %s must equal exactly %s", allRoles, expectedAuthorities)
                .containsExactlyInAnyOrderElementsOf(expectedAuthorities);
    }

    /**
     * Generates role name strings that are NOT in the 10 known roles.
     * Uses lowercase alpha strings so they are clearly in the "unknown" bucket
     * and would previously have been prefixed with {@code platform:}.
     */
    @Provide
    Arbitrary<String> unknownRoleNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !KNOWN_ROLES.contains(s))
                .filter(s -> !s.startsWith("merchant:"))
                .filter(s -> !s.startsWith("platform:"))
                .filter(s -> !s.startsWith("tenant:"));
    }

    /**
     * Generates an optional (possibly empty) subset of known roles to mix with the unknown role.
     */
    @Provide
    Arbitrary<List<String>> knownRoleSubsetsIncludingEmpty() {
        List<String> allRoles = new ArrayList<>(KNOWN_ROLES);
        return Arbitraries.subsetOf(allRoles).map(ArrayList::new);
    }

    // =========================================================================
    // Property 3 — Absent or malformed realm_access/roles yields no authorities
    //
    // Feature: backend-authority-refactor, Property 3: malformed claim yields empty
    // Validates: Requirements 2.4, 2.5
    // =========================================================================

    // --- Explicit @Test for each guard case (R2.4 / R2.5) ---

    @Test
    void realmAccessAbsent_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithNoRealmAccess())).isEmpty();
    }

    @Test
    void realmAccessIsString_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithRealmAccessValue("not-a-map"))).isEmpty();
    }

    @Test
    void realmAccessIsInteger_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithRealmAccessValue(42))).isEmpty();
    }

    @Test
    void realmAccessIsList_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithRealmAccessValue(List.of("roles", "some-role")))).isEmpty();
    }

    @Test
    void rolesKeyAbsent_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithRealmAccessButNoRoles())).isEmpty();
    }

    @Test
    void rolesIsString_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithRolesValue("merchants:create"))).isEmpty();
    }

    @Test
    void rolesIsInteger_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithRolesValue(99))).isEmpty();
    }

    @Test
    void rolesIsMap_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithRolesValue(Map.of("merchants:create", true)))).isEmpty();
    }

    @Test
    void realmAccessIsEmptyMap_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithClaims(Map.of("realm_access", Map.of())))).isEmpty();
    }

    @Test
    void rolesContainsOnlyNonStringElements_yieldsEmptyAuthorities() {
        assertThat(CONVERTER.convert(jwtWithRoles(List.of(1, 2, 3, 42)))).isEmpty();
    }

    /**
     * Property 3 (PBT): for any non-Collection value in the {@code roles} claim,
     * the converter returns an empty authority collection.
     *
     * <p><b>Validates: Requirements 2.4, 2.5</b>
     */
    @Property(tries = 30)
    @Label("Feature: backend-authority-refactor, Property 3: malformed claim yields empty")
    @net.jqwik.api.Tag("backend-authority-refactor")
    void property3_nonCollectionRolesValue_yieldsEmptyAuthorities(
            @ForAll("nonCollectionRolesValues") Object badRolesValue) {

        Collection<GrantedAuthority> authorities = CONVERTER.convert(jwtWithRolesValue(badRolesValue));

        assertThat(authorities)
                .as("roles = %s (non-Collection) must produce no authorities", badRolesValue)
                .isEmpty();
    }

    /**
     * Property 3 (PBT): for any non-Map value in the {@code realm_access} claim,
     * the converter returns an empty authority collection.
     *
     * <p><b>Validates: Requirements 2.4, 2.5</b>
     */
    @Property(tries = 30)
    @Label("Feature: backend-authority-refactor, Property 3: malformed claim yields empty")
    @net.jqwik.api.Tag("backend-authority-refactor")
    void property3_nonMapRealmAccessValue_yieldsEmptyAuthorities(
            @ForAll("nonMapRealmAccessValues") Object badRealmAccess) {

        Collection<GrantedAuthority> authorities = CONVERTER.convert(jwtWithRealmAccessValue(badRealmAccess));

        assertThat(authorities)
                .as("realm_access = %s (non-Map) must produce no authorities", badRealmAccess)
                .isEmpty();
    }

    // -------------------------------------------------------------------------
    // Arbitraries / generators for Property 3
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Object> nonCollectionRolesValues() {
        Arbitrary<Object> strings = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(30)
                .map(s -> (Object) s);

        Arbitrary<Object> integers = Arbitraries.integers()
                .map(i -> (Object) i);

        Arbitrary<Object> booleans = Arbitraries.of(Boolean.TRUE, Boolean.FALSE)
                .map(b -> (Object) b);

        Arbitrary<Object> maps = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(15)
                .flatMap(key ->
                        Arbitraries.strings()
                                .withCharRange('a', 'z')
                                .ofMinLength(1)
                                .ofMaxLength(15)
                                .map(val -> (Object) Map.of(key, val)));

        return Arbitraries.oneOf(strings, integers, booleans, maps);
    }

    @Provide
    Arbitrary<Object> nonMapRealmAccessValues() {
        Arbitrary<Object> strings = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(30)
                .map(s -> (Object) s);

        Arbitrary<Object> integers = Arbitraries.integers()
                .map(i -> (Object) i);

        Arbitrary<Object> booleans = Arbitraries.of(Boolean.TRUE, Boolean.FALSE)
                .map(b -> (Object) b);

        Arbitrary<Object> lists = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(15)
                .list()
                .ofMinSize(0)
                .ofMaxSize(5)
                .map(l -> (Object) l);

        return Arbitraries.oneOf(strings, integers, booleans, lists);
    }
}
