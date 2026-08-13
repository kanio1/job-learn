package lab.paymentquality.checkoutlab.internal.application;

public enum CheckoutLabScenario {
    HAPPY_COMPLETED,
    USER_CANCEL,
    RETURN_LIE_SUCCESS,
    BAD_SIGNATURE,
    NOTIFY_5XX_RETRY,
    OOO_EVENTS,
    EXPIRED_LINK,
    PAY_NO_RETURN;

    public static CheckoutLabScenario fromHeader(String raw) {
        if (raw == null || raw.isBlank()) {
            return HAPPY_COMPLETED;
        }
        try {
            return CheckoutLabScenario.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnknownCheckoutScenarioException(raw);
        }
    }
}
