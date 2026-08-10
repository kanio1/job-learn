package lab.paymentquality.checkoutlab.internal.application;

public record CreateCheckoutSessionCommand(
        String extOrderId,
        long amountMinor,
        String currency,
        String continueUrl,
        String notifyUrl,
        long validitySeconds
) {
}
