package lab.paymentquality.testsupport.restkit.payload;

import java.util.Map;

public record CreatePaymentOrderPayload(
        long amountMinor,
        String currency,
        String clientOrderReference
) {
    public CreatePaymentOrderPayload {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        if (clientOrderReference == null || clientOrderReference.isBlank()) {
            throw new IllegalArgumentException("clientOrderReference must not be blank");
        }
    }

    public static CreatePaymentOrderPayload pln(long amountMinor, String reference) {
        return new CreatePaymentOrderPayload(amountMinor, "PLN", reference);
    }

    public static CreatePaymentOrderPayload eur(long amountMinor, String reference) {
        return new CreatePaymentOrderPayload(amountMinor, "EUR", reference);
    }

    public static CreatePaymentOrderPayload withCurrency(
            long amountMinor,
            String currency,
            String reference
    ) {
        return new CreatePaymentOrderPayload(amountMinor, currency, reference);
    }

    public Map<String, Object> asJson() {
        return Map.of(
                "amountMinor", amountMinor,
                "currency", currency,
                "clientOrderReference", clientOrderReference
        );
    }

}
