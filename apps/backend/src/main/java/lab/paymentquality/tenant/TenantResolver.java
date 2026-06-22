package lab.paymentquality.tenant;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * PUBLIC module API for the tenant module. Resolves the JWT tenant_id claim to a TenantContext.
 * Throws TenantResolutionException (→403) when:
 *   - The tenant_id claim is absent
 *   - The claim maps to no tenant_reference in the database
 *   - The resolved tenant's status is SUSPENDED (for non-platform principals)
 */
public interface TenantResolver {
    TenantContext resolve(Jwt jwt);

    /**
     * Resolves a tenant natural key to its internal tenant_id for ownership assignment.
     *
     * <p>This is intentionally smaller than returning tenant internals: callers get only the
     * stable UUID foreign key they need to persist tenant-owned records.</p>
     */
    UUID resolveTenantId(TenantReference tenantReference);
}
