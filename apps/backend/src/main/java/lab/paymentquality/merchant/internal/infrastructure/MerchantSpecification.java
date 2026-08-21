package lab.paymentquality.merchant.internal.infrastructure;

import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.domain.MerchantStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class MerchantSpecification {

    private MerchantSpecification() {
    }

    public static Specification<Merchant> hasTenantId(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Merchant> hasStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), MerchantStatus.valueOf(status));
    }

    public static Specification<Merchant> riskFlagged(Boolean riskFlagged) {
        if (riskFlagged == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("riskFlagged"), riskFlagged);
    }

    public static Specification<Merchant> matchesQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("normalizedReference")), pattern),
                cb.like(cb.lower(root.get("displayName")), pattern));
    }
}
