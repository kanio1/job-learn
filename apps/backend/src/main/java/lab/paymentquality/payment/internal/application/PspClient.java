package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentLifecycleAction;

import java.util.UUID;

public interface PspClient {

    PspResult authorize(UUID paymentOrderId, long amountMinor, String currency);

    PspResult capture(UUID paymentOrderId, long amountMinor, String currency);

    PspResult voidAuthorization(UUID paymentOrderId, String pspReference);

    PspResult refund(UUID paymentOrderId, long amountMinor, String currency);

    record PspResult(boolean success, String pspReference, String message) {

        public static PspResult success(String pspReference) {
            return new PspResult(true, pspReference, "OK");
        }
    }
}
