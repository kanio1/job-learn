package lab.paymentquality.merchant.internal.domain;

public class MerchantVersionMismatchException extends RuntimeException {
    public MerchantVersionMismatchException() {
        super("Merchant was modified after you loaded it. Reload and retry.");
    }
}
