package lab.paymentquality.checkoutlab.internal.application;

public class MissingCheckoutSimulateTokenException extends RuntimeException {

    public MissingCheckoutSimulateTokenException() {
        super("Lab-Simulate-Token header is required");
    }
}
