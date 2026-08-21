package lab.paymentquality.support.internal.domain;

public class SupportMerchantNotFoundException extends RuntimeException {
    public SupportMerchantNotFoundException(String merchantId) {
        super("Merchant not found: " + merchantId);
    }
}
