package lab.paymentquality.merchant.internal.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaMerchantImportPreviewRepository extends JpaRepository<MerchantImportPreview, UUID> {
}
