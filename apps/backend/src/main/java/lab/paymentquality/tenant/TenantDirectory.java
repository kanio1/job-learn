package lab.paymentquality.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public directory of tenants for read-models such as the org tree.
 * Callers must still enforce platform vs tenant-scoped visibility.
 */
public interface TenantDirectory {

    List<TenantSummary> listAll();

    Optional<TenantSummary> findById(UUID tenantId);
}
