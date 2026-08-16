package lab.paymentquality.payment.internal.domain;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PaymentEvidenceContentUnavailableException extends RuntimeException {

    public PaymentEvidenceContentUnavailableException(UUID evidenceId) {
        super("Evidence content is not stored for download: " + evidenceId);
    }

    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }

    public String error() {
        return "evidence_content_unavailable";
    }
}
