package lab.paymentquality.tenant;

import java.util.UUID;

/**
 * Public read-model of a tenant for org-tree and similar directory queries.
 * Does not expose tenant internals or settings.
 */
public record TenantSummary(UUID tenantId, String tenantReference, String name) {
}
