package lab.paymentquality.tenant;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * PUBLIC module API for the tenant module. Resolves the JWT tenant_id claim to a TenantContext.
 * Throws TenantResolutionException (→403) when:
 *   - The tenant_id claim is absent
 *   - The claim maps to no tenant_reference in the database
 *   - The resolved tenant's status is SUSPENDED (for non-platform principals)
 */
public interface TenantResolver {
    TenantContext resolve(Jwt jwt);
}
