package lab.paymentquality.payment.internal.domain;

public record ClientOrderReference(String value) {

    private static final int MAX_LENGTH = 120;

    public static ClientOrderReference of(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidClientOrderReferenceException("Client order reference must not be blank");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidClientOrderReferenceException(
                    "Client order reference must not exceed " + MAX_LENGTH + " characters");
        }
        return new ClientOrderReference(trimmed);
    }

    public String value() {
        return value;
    }
}
