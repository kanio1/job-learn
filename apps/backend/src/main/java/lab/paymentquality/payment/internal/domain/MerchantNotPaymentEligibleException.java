package lab.paymentquality.payment.internal.domain;

import java.util.UUID;

public class MerchantNotPaymentEligibleException extends RuntimeException {

    public MerchantNotPaymentEligibleException(UUID merchantId) {
        super("Merchant " + merchantId + " is not payment eligible");
    }
}
