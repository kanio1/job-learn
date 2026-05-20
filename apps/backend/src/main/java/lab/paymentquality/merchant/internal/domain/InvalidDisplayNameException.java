package lab.paymentquality.merchant.internal.domain;

public class InvalidDisplayNameException extends RuntimeException {

    private final String attempted;

    public InvalidDisplayNameException(String attempted) {
        super("Invalid display name: " + attempted);
        this.attempted = attempted;
    }

    public String getAttempted() {
        return attempted;
    }
}
