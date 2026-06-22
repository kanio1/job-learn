package lab.paymentquality.payment.internal.web;

import java.util.List;

public record PaymentErrorResponse(
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

    public static PaymentErrorResponse of(String error, String message, String correlationId) {
        return of(error, message, null, correlationId, 400, "Bad request");
    }

    public static PaymentErrorResponse withDetails(String error, String message,
                                                     List<FieldError> details, String correlationId) {
        return of(error, message, details, correlationId, 400, "Bad request");
    }

    public static PaymentErrorResponse of(String error, String message, List<FieldError> details,
                                          String correlationId, int status, String title) {
        return new PaymentErrorResponse(
                "https://api.payment-quality.local/problems/" + error.replace('_', '-'),
                title,
                status,
                message,
                error.toUpperCase(),
                correlationId,
                error,
                message,
                details
        );
    }
}
