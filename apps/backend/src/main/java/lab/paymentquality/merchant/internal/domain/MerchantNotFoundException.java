package lab.paymentquality.merchant.internal.domain;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(String id) {
        super("Merchant not found: " + id);
    }
}
