package lab.paymentquality.checkoutlab.internal.domain;

public class InvalidCheckoutCurrencyException extends RuntimeException {

    public InvalidCheckoutCurrencyException(String currency) {
        super("currency must be one of PLN, EUR, USD, got: " + currency);
    }
}
