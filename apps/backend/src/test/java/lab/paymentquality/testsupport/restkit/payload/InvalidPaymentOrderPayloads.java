package lab.paymentquality.testsupport.restkit.payload;

import java.util.Map;

public final class InvalidPaymentOrderPayloads {

    private InvalidPaymentOrderPayloads() {
    }

    public static Map<String, Object> missingRequiredFields() {
        return Map.of();
    }

    public static Map<String, Object> zeroAmount(String reference) {
        return Map.of(
                "amountMinor", 0,
                "currency", "PLN",
                "clientOrderReference", requireReference(reference)
        );
    }

    public static Map<String, Object> unsupportedCurrency(String reference) {
        return Map.of(
                "amountMinor", 1_000,
                "currency", "GBP",
                "clientOrderReference", requireReference(reference)
        );
    }

    public static String malformedJson() {
        return """
                {
                  "amountMinor": 1000,
                  "currency": "PLN",
                  "clientOrderReference": "broken-json"
                """;
    }

    private static String requireReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
        return reference.trim();
    }
}
