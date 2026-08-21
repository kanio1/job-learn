package lab.paymentquality.merchant.internal.domain;

public class MerchantImportMalformedException extends RuntimeException {
    public MerchantImportMalformedException(String message) {
        super(message);
    }
}
