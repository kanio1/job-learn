package lab.paymentquality.merchant.internal.web;

public record ErrorResponse(String error, String message, Object details) {

    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}
