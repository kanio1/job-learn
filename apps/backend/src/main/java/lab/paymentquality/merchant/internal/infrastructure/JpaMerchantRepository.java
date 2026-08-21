package lab.paymentquality.merchant.internal.infrastructure;

import lab.paymentquality.merchant.internal.domain.Merchant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaMerchantRepository extends JpaRepository<Merchant, UUID>, JpaSpecificationExecutor<Merchant> {

    Optional<Merchant> findByNormalizedReference(String ref);

    Optional<Merchant> findByMerchantIdAndTenantId(UUID merchantId, UUID tenantId);

    List<Merchant> findAllByOrderByCreatedAtDescMerchantIdAsc(Pageable pageable);

    List<Merchant> findAllByTenantIdOrderByCreatedAtDescMerchantIdAsc(UUID tenantId, Pageable pageable);
}
