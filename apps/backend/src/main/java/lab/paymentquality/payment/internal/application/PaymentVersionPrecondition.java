package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentOrderVersionMismatchException;

// Feature 011 helper: enforces the already-parsed HTTP version marker before any lifecycle/domain mutation runs.
public final class PaymentVersionPrecondition {

    private PaymentVersionPrecondition() {
    }

    public static void requireCurrentVersion(PaymentOrder order, long expectedVersion) {
        Long currentVersion = order.getVersion();
        if (currentVersion == null || currentVersion != expectedVersion) {
            throw new PaymentOrderVersionMismatchException();
        }
    }
}
