package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentOrderRepository extends JpaRepository<PaymentOrder, UUID>,
        JpaSpecificationExecutor<PaymentOrder> {

    Optional<PaymentOrder> findByMerchantIdAndPaymentOrderId(UUID merchantId, UUID paymentOrderId);

    Optional<PaymentOrder> findByPaymentOrderId(UUID paymentOrderId);

    @Query("""
            SELECT COUNT(po) AS orderCount,
                   COALESCE(SUM(po.amountMinor), 0) AS totalAmountMinor
            FROM PaymentOrder po
            WHERE po.merchantId = :merchantId
              AND (CAST(:currency AS string) IS NULL OR po.currency = :currency)
              AND (CAST(:status AS string) IS NULL OR po.status = :status)
              AND (CAST(:fromInclusive AS java.time.Instant) IS NULL OR po.createdAt >= :fromInclusive)
              AND (CAST(:toInclusive AS java.time.Instant) IS NULL OR po.createdAt <= :toInclusive)
            """)
    SummaryTotalsProjection findSummaryTotals(
            @Param("merchantId") UUID merchantId,
            @Param("currency") String currency,
            @Param("status") PaymentStatus status,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toInclusive") Instant toInclusive
    );

    @Query("""
            SELECT po.currency AS currency,
                   COUNT(po) AS orderCount,
                   COALESCE(SUM(po.amountMinor), 0) AS totalAmountMinor
            FROM PaymentOrder po
            WHERE po.merchantId = :merchantId
              AND (CAST(:currency AS string) IS NULL OR po.currency = :currency)
              AND (CAST(:status AS string) IS NULL OR po.status = :status)
              AND (CAST(:fromInclusive AS java.time.Instant) IS NULL OR po.createdAt >= :fromInclusive)
              AND (CAST(:toInclusive AS java.time.Instant) IS NULL OR po.createdAt <= :toInclusive)
            GROUP BY po.currency
            ORDER BY po.currency ASC
            """)
    List<CurrencySummaryProjection> findSummaryByCurrency(
            @Param("merchantId") UUID merchantId,
            @Param("currency") String currency,
            @Param("status") PaymentStatus status,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toInclusive") Instant toInclusive
    );

    @Query("""
            SELECT po.status AS status,
                   COUNT(po) AS orderCount,
                   COALESCE(SUM(po.amountMinor), 0) AS totalAmountMinor
            FROM PaymentOrder po
            WHERE po.merchantId = :merchantId
              AND (CAST(:currency AS string) IS NULL OR po.currency = :currency)
              AND (CAST(:status AS string) IS NULL OR po.status = :status)
              AND (CAST(:fromInclusive AS java.time.Instant) IS NULL OR po.createdAt >= :fromInclusive)
              AND (CAST(:toInclusive AS java.time.Instant) IS NULL OR po.createdAt <= :toInclusive)
            GROUP BY po.status
            ORDER BY po.status ASC
            """)
    List<StatusSummaryProjection> findSummaryByStatus(
            @Param("merchantId") UUID merchantId,
            @Param("currency") String currency,
            @Param("status") PaymentStatus status,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toInclusive") Instant toInclusive
    );

    interface SummaryTotalsProjection {
        long getOrderCount();

        Long getTotalAmountMinor();
    }

    interface CurrencySummaryProjection {
        String getCurrency();

        long getOrderCount();

        Long getTotalAmountMinor();
    }

    interface StatusSummaryProjection {
        PaymentStatus getStatus();

        long getOrderCount();

        Long getTotalAmountMinor();
    }
}
