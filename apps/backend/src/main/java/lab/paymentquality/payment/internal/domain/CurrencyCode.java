package lab.paymentquality.payment.internal.domain;

public record CurrencyCode(String code) {

    private static final java.util.Set<String> SUPPORTED = java.util.Set.of("PLN", "EUR", "USD");

    public static CurrencyCode of(String code) {
        if (code == null || !SUPPORTED.contains(code)) {
            throw new InvalidCurrencyCodeException(code);
        }
        return new CurrencyCode(code);
    }

    public String code() {
        return code;
    }
}
