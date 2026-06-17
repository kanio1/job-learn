package lab.paymentquality.tenant.internal.infrastructure;

import lab.paymentquality.tenant.internal.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JpaTenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByTenantReference(String tenantReference);
}
