package lab.paymentquality.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    // Known realm role retained for behavior-preserving conversion (see "operate role status").
    // No SecurityConfig rule or @PreAuthorize references this authority; it is intentionally
    // NOT part of the enforced Authorities catalog.
    static final String MERCHANT_PAYMENTS_OPERATE = "merchant:payments:operate";

    private static final Map<String, String> KNOWN_ROLES = Map.ofEntries(
        Map.entry("merchants:create",             Authorities.MERCHANTS_CREATE),
        Map.entry("merchants:read",              Authorities.MERCHANTS_READ),
        Map.entry("merchants:update-status",     Authorities.MERCHANTS_UPDATE_STATUS),
        Map.entry("merchants:update-risk-flag",  Authorities.MERCHANTS_UPDATE_RISK_FLAG),
        Map.entry("merchant:payments:create",    Authorities.MERCHANT_PAYMENTS_CREATE),
        Map.entry("merchant:payments:read",      Authorities.MERCHANT_PAYMENTS_READ),
        Map.entry("merchant:payments:operate",   MERCHANT_PAYMENTS_OPERATE),
        Map.entry("merchant:payments:lifecycle", Authorities.MERCHANT_PAYMENTS_LIFECYCLE),
        Map.entry("platform:payments:read",          Authorities.PLATFORM_PAYMENTS_READ),
        Map.entry("platform:payments:lifecycle",     Authorities.PLATFORM_PAYMENTS_LIFECYCLE),
        Map.entry("platform:payments:audit",         Authorities.PLATFORM_PAYMENTS_AUDIT),
        Map.entry("platform:payments:notes:read",    Authorities.PLATFORM_PAYMENT_NOTES_READ),
        Map.entry("platform:payments:notes:create",  Authorities.PLATFORM_PAYMENT_NOTES_CREATE),
        Map.entry("platform:audit:read",                Authorities.PLATFORM_AUDIT_READ),
        Map.entry("tenant:audit:read",                  Authorities.TENANT_AUDIT_READ),
        Map.entry("platform:tenant:settings:read",   Authorities.TENANT_SETTINGS_READ),
        Map.entry("platform:tenant:settings:update", Authorities.TENANT_SETTINGS_UPDATE),
        Map.entry("platform:users:read",         Authorities.PLATFORM_USERS_READ),
        Map.entry("platform:users:create",       Authorities.PLATFORM_USERS_CREATE),
        Map.entry("platform:users:update",       Authorities.PLATFORM_USERS_UPDATE),
        Map.entry("platform:users:assign-roles", Authorities.PLATFORM_USERS_ASSIGN_ROLES),
        Map.entry("tenant:users:read",           Authorities.TENANT_USERS_READ),
        Map.entry("tenant:users:create",         Authorities.TENANT_USERS_CREATE),
        Map.entry("tenant:users:update",         Authorities.TENANT_USERS_UPDATE),
        Map.entry("tenant:users:assign-roles",   Authorities.TENANT_USERS_ASSIGN_ROLES),
        Map.entry("platform:support:read",       Authorities.SUPPORT_READ),
        Map.entry("platform:support:operate",    Authorities.SUPPORT_OPERATE),
        Map.entry("platform:ops:feed",           Authorities.OPS_FEED),
        Map.entry("platform:ops:inject",         Authorities.OPS_INJECT),
        Map.entry("platform:notifications:read", Authorities.NOTIFICATIONS_READ));

    /**
     * Keycloak may put only the composite name on the access token. Expand to the
     * same leaf authorities as {@code payment-quality-realm.json}.
     */
    private static final Map<String, List<String>> COMPOSITE_AUTHORITIES = Map.ofEntries(
        Map.entry("PLATFORM_ADMIN", List.of(
                Authorities.MERCHANTS_CREATE,
                Authorities.MERCHANTS_READ,
                Authorities.MERCHANTS_UPDATE_STATUS,
                Authorities.MERCHANTS_UPDATE_RISK_FLAG,
                Authorities.PLATFORM_PAYMENTS_READ,
                Authorities.PLATFORM_PAYMENTS_LIFECYCLE,
                Authorities.PLATFORM_PAYMENTS_AUDIT,
                Authorities.PLATFORM_AUDIT_READ,
                Authorities.PLATFORM_USERS_READ,
                Authorities.PLATFORM_USERS_CREATE,
                Authorities.PLATFORM_USERS_UPDATE,
                Authorities.PLATFORM_USERS_ASSIGN_ROLES,
                Authorities.TENANT_SETTINGS_READ,
                Authorities.TENANT_SETTINGS_UPDATE,
                Authorities.PLATFORM_PAYMENT_NOTES_READ,
                Authorities.PLATFORM_PAYMENT_NOTES_CREATE,
                Authorities.SUPPORT_READ,
                Authorities.SUPPORT_OPERATE,
                Authorities.OPS_FEED,
                Authorities.OPS_INJECT,
                Authorities.NOTIFICATIONS_READ)),
        Map.entry("TENANT_ADMIN", List.of(
                Authorities.MERCHANTS_CREATE,
                Authorities.MERCHANTS_READ,
                Authorities.MERCHANTS_UPDATE_STATUS,
                Authorities.MERCHANT_PAYMENTS_READ,
                Authorities.TENANT_AUDIT_READ,
                Authorities.TENANT_USERS_READ,
                Authorities.TENANT_USERS_CREATE,
                Authorities.TENANT_USERS_UPDATE,
                Authorities.TENANT_USERS_ASSIGN_ROLES,
                Authorities.TENANT_SETTINGS_READ,
                Authorities.TENANT_SETTINGS_UPDATE,
                Authorities.SUPPORT_READ,
                Authorities.OPS_FEED,
                Authorities.NOTIFICATIONS_READ)),
        Map.entry("MERCHANT_MANAGER", List.of(
                Authorities.MERCHANT_PAYMENTS_CREATE,
                Authorities.MERCHANT_PAYMENTS_READ,
                Authorities.MERCHANT_PAYMENTS_LIFECYCLE,
                Authorities.OPS_FEED,
                Authorities.NOTIFICATIONS_READ)),
        Map.entry("SUPPORT_AGENT", List.of(
                Authorities.MERCHANTS_READ,
                Authorities.PLATFORM_PAYMENTS_READ,
                Authorities.PLATFORM_PAYMENTS_AUDIT,
                Authorities.PLATFORM_AUDIT_READ,
                Authorities.PLATFORM_PAYMENT_NOTES_READ,
                Authorities.PLATFORM_PAYMENT_NOTES_CREATE,
                Authorities.SUPPORT_READ,
                Authorities.SUPPORT_OPERATE,
                Authorities.OPS_FEED,
                Authorities.NOTIFICATIONS_READ)),
        Map.entry("READ_ONLY_USER", List.of(
                Authorities.MERCHANTS_READ,
                Authorities.PLATFORM_PAYMENTS_READ,
                Authorities.SUPPORT_READ,
                Authorities.OPS_FEED,
                Authorities.NOTIFICATIONS_READ)));

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
                .flatMap(role -> authoritiesFor(role).stream())
                .distinct()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }

    private static List<String> authoritiesFor(String role) {
        List<String> composite = COMPOSITE_AUTHORITIES.get(role);
        if (composite != null) {
            return composite;
        }
        String mapped = KNOWN_ROLES.get(role);
        return mapped == null ? List.of() : List.of(mapped);
    }
}
