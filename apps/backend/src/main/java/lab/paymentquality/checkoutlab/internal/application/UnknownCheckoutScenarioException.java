package lab.paymentquality.checkoutlab.internal.application;

public class UnknownCheckoutScenarioException extends RuntimeException {

    public UnknownCheckoutScenarioException(String scenario) {
        super("Unknown checkout-lab scenario: " + scenario);
    }
}
