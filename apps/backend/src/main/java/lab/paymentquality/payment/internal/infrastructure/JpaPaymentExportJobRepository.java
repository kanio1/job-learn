package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.PaymentExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentExportJobRepository extends JpaRepository<PaymentExportJob, UUID> {

    Optional<PaymentExportJob> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    @Query(value = """
            SELECT CAST(job_id AS varchar) FROM payment_export_jobs
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Object> claimPendingIds(@Param("batchSize") int batchSize);
}
