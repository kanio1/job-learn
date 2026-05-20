package lab.paymentquality.merchant.internal.domain;

public class InvalidMerchantReferenceException extends RuntimeException {

    private final String attempted;

    public InvalidMerchantReferenceException(String attempted) {
        super("Invalid merchant reference: " + attempted);
        this.attempted = attempted;
    }

    public String getAttempted() {
        return attempted;
    }
}
