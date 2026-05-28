package lab.paymentquality.payment.internal.web;

import java.util.List;

public record PaymentErrorResponse(
        String error,
        String message,
        List<FieldError> details,
        String correlationId
) {

    public record FieldError(String field, String message) {
    }

    public static PaymentErrorResponse of(String error, String message, String correlationId) {
        return new PaymentErrorResponse(error, message, null, correlationId);
    }

    public static PaymentErrorResponse withDetails(String error, String message,
                                                    List<FieldError> details, String correlationId) {
        return new PaymentErrorResponse(error, message, details, correlationId);
    }
}
