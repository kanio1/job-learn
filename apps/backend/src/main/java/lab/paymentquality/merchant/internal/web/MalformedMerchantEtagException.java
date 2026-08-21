package lab.paymentquality.merchant.internal.web;

public class MalformedMerchantEtagException extends RuntimeException {
    public MalformedMerchantEtagException(String message) {
        super(message);
    }
}
