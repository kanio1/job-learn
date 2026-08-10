package lab.paymentquality.checkoutlab.internal.web;

import java.util.List;

public record CheckoutLabErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String code,
        String correlationId,
        String error,
        String message,
        List<FieldError> details
) {

    public record FieldError(String field, String message) {
    }

    public static CheckoutLabErrorResponse of(
            String error,
            String message,
            List<FieldError> details,
            String correlationId,
            int status,
            String title) {
        return new CheckoutLabErrorResponse(
                "https://api.payment-quality.local/problems/" + error.replace('_', '-'),
                title,
                status,
                message,
                error.toUpperCase(),
                correlationId,
                error,
                message,
                details);
    }
}
