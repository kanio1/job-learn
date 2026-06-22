package lab.paymentquality.payment.internal.infrastructure;

import jakarta.persistence.criteria.Predicate;
import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PaymentOrderSpecification {

    private PaymentOrderSpecification() {
    }

    public static Specification<PaymentOrder> hasMerchantId(UUID merchantId) {
        return (root, query, cb) -> cb.equal(root.get("merchantId"), merchantId);
    }

    public static Specification<PaymentOrder> hasStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), PaymentStatus.valueOf(status));
    }

    public static Specification<PaymentOrder> hasCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("currency"), currency);
    }

    public static Specification<PaymentOrder> createdBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (from != null) {
                ZonedDateTime startOfDay = from.atStartOfDay(ZoneOffset.UTC);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay.toInstant()));
            }
            if (to != null) {
                ZonedDateTime endOfDay = to.atTime(23, 59, 59, 999_999_999)
                        .atZone(ZoneOffset.UTC);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endOfDay.toInstant()));
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<PaymentOrder> amountBetween(Long minAmount, Long maxAmount) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minAmount != null && minAmount >= 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amountMinor"), minAmount));
            }
            if (maxAmount != null && maxAmount >= 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amountMinor"), maxAmount));
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<PaymentOrder> clientOrderReferenceContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("clientOrderReference")),
                "%" + search.toLowerCase() + "%"
        );
    }
}
