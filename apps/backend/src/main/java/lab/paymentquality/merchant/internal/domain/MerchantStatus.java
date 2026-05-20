package lab.paymentquality.merchant.internal.domain;

/**
 * Lifecycle state for a merchant.
 * Phase 1 states: DRAFT, ACTIVE, SUSPENDED.
 * Valid transitions: DRAFT → ACTIVE, ACTIVE → SUSPENDED. All others return false.
 */
public enum MerchantStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED;

    public boolean canTransitionTo(MerchantStatus target) {
        return switch (this) {
            case DRAFT -> target == ACTIVE;
            case ACTIVE -> target == SUSPENDED;
            case SUSPENDED -> false;
        };
    }
}
