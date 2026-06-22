package lab.paymentquality.payment.internal.web;

public record CaptureRequest(Long amountMinor, String reason) {
}
