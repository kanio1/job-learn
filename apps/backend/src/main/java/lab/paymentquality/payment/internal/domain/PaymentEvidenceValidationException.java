package lab.paymentquality.payment.internal.domain;

import org.springframework.http.HttpStatus;

public class PaymentEvidenceValidationException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public PaymentEvidenceValidationException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus status() {
        return status;
    }

    public String error() {
        return error;
    }
}
