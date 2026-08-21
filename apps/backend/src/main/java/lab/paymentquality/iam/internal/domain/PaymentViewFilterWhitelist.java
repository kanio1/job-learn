package lab.paymentquality.iam.internal.domain;

import lab.paymentquality.iam.internal.domain.exception.InvalidSavedViewFiltersException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Allowed payment-list query keys stored on a view. Intentionally duplicated
 * here so iam does not import {@code payment.internal}.
 */
public final class PaymentViewFilterWhitelist {

    public static final Set<String> KEYS = Set.of(
            "status",
            "currency",
            "minAmount",
            "maxAmount",
            "fromDate",
            "toDate",
            "clientOrderReference",
            "sort");

    private PaymentViewFilterWhitelist() {
    }

    public static Map<String, Object> validated(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            if (!KEYS.contains(key)) {
                throw new InvalidSavedViewFiltersException("Unknown filter key: " + key);
            }
            if (entry.getValue() != null) {
                copy.put(key, entry.getValue());
            }
        }
        return Map.copyOf(copy);
    }
}
