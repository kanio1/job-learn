package lab.paymentquality.payment.internal.domain;

public class InvalidCurrencyCodeException extends RuntimeException {

    public InvalidCurrencyCodeException(String code) {
        super("Currency code must be one of PLN, EUR, USD, got: " + code);
    }
}
