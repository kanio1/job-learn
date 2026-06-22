package lab.paymentquality.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    // Known realm role retained for behavior-preserving conversion (see "operate role status").
    // No SecurityConfig rule or @PreAuthorize references this authority; it is intentionally
    // NOT part of the enforced Authorities catalog.
    static final String MERCHANT_PAYMENTS_OPERATE = "merchant:payments:operate";

    private static final Map<String, String> KNOWN_ROLES = Map.ofEntries(
        Map.entry("merchants:create",            Authorities.MERCHANTS_CREATE),
        Map.entry("merchants:read",              Authorities.MERCHANTS_READ),
        Map.entry("merchants:update-status",     Authorities.MERCHANTS_UPDATE_STATUS),
        Map.entry("merchant:payments:create",    Authorities.MERCHANT_PAYMENTS_CREATE),
        Map.entry("merchant:payments:read",      Authorities.MERCHANT_PAYMENTS_READ),
        Map.entry("merchant:payments:operate",   MERCHANT_PAYMENTS_OPERATE),
        Map.entry("merchant:payments:lifecycle", Authorities.MERCHANT_PAYMENTS_LIFECYCLE),
        Map.entry("platform:payments:read",      Authorities.PLATFORM_PAYMENTS_READ),
        Map.entry("platform:payments:lifecycle", Authorities.PLATFORM_PAYMENTS_LIFECYCLE),
        Map.entry("platform:payments:audit",     Authorities.PLATFORM_PAYMENTS_AUDIT),
        Map.entry("platform:audit:read",         Authorities.PLATFORM_AUDIT_READ),
        Map.entry("tenant:audit:read",           Authorities.TENANT_AUDIT_READ),
        Map.entry("platform:users:read",         Authorities.PLATFORM_USERS_READ),
        Map.entry("platform:users:create",       Authorities.PLATFORM_USERS_CREATE),
        Map.entry("platform:users:update",       Authorities.PLATFORM_USERS_UPDATE),
        Map.entry("platform:users:assign-roles", Authorities.PLATFORM_USERS_ASSIGN_ROLES),
        Map.entry("tenant:users:read",           Authorities.TENANT_USERS_READ),
        Map.entry("tenant:users:create",         Authorities.TENANT_USERS_CREATE),
        Map.entry("tenant:users:update",         Authorities.TENANT_USERS_UPDATE),
        Map.entry("tenant:users:assign-roles",   Authorities.TENANT_USERS_ASSIGN_ROLES));

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        var realmAccessClaim = jwt.getClaims().get("realm_access");
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return List.of();
        }
        var rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(KNOWN_ROLES::get)
                .filter(Objects::nonNull)
                .distinct()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }
}
