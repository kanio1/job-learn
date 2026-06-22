package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JpaIdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    @Query("""
            SELECT record
            FROM IdempotencyRecord record
            WHERE record.merchantId = :merchantId
              AND record.action = 'CREATE'
              AND record.idempotencyKeyHash = :keyHash
            """)
    Optional<IdempotencyRecord> findByMerchantIdAndIdempotencyKeyHash(
            @Param("merchantId") UUID merchantId,
            @Param("keyHash") String keyHash);

    Optional<IdempotencyRecord> findByMerchantIdAndActionAndIdempotencyKeyHash(
            UUID merchantId, String action, String keyHash);

    Optional<IdempotencyRecord> findByMerchantIdAndPaymentOrderIdAndActionAndIdempotencyKeyHash(
            UUID merchantId, UUID paymentOrderId, String action, String keyHash);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO idempotency_records (
                idempotency_record_id,
                merchant_id,
                action,
                idempotency_key_hash,
                request_fingerprint_hash,
                created_at
            ) VALUES (
                :idempotencyRecordId,
                :merchantId,
                'CREATE',
                :keyHash,
                :fingerprintHash,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int reserveIfAbsent(@Param("idempotencyRecordId") UUID idempotencyRecordId,
                        @Param("merchantId") UUID merchantId,
                        @Param("keyHash") String keyHash,
                        @Param("fingerprintHash") String fingerprintHash);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO idempotency_records (
                idempotency_record_id,
                merchant_id,
                payment_order_id,
                action,
                idempotency_key_hash,
                request_fingerprint_hash,
                created_at
            ) VALUES (
                :idempotencyRecordId,
                :merchantId,
                :paymentOrderId,
                :action,
                :keyHash,
                :fingerprintHash,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int reserveIfAbsent(@Param("idempotencyRecordId") UUID idempotencyRecordId,
                        @Param("merchantId") UUID merchantId,
                        @Param("paymentOrderId") UUID paymentOrderId,
                        @Param("action") String action,
                        @Param("keyHash") String keyHash,
                        @Param("fingerprintHash") String fingerprintHash);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE IdempotencyRecord record
            SET record.paymentOrderId = :paymentOrderId,
                record.completedAt = :completedAt
            WHERE record.idempotencyRecordId = :idempotencyRecordId
            """)
    int complete(@Param("idempotencyRecordId") UUID idempotencyRecordId,
                 @Param("paymentOrderId") UUID paymentOrderId,
                 @Param("completedAt") Instant completedAt);
}
