package lab.paymentquality.checkoutlab.internal.application;

import java.time.Instant;

public record CheckoutLabSimulateToken(String token, Instant expiresAt) {
}
