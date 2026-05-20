package lab.paymentquality.merchant.internal.infrastructure;

import lab.paymentquality.merchant.internal.domain.Merchant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaMerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByNormalizedReference(String ref);

    List<Merchant> findAllByOrderByCreatedAtDescMerchantIdAsc(Pageable pageable);
}
