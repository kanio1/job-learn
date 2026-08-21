package lab.paymentquality.merchant.internal.web;

public class MerchantPreconditionRequiredException extends RuntimeException {
    public MerchantPreconditionRequiredException(String message) {
        super(message);
    }
}
