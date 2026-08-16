package lab.paymentquality.payment.internal.domain;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PaymentExportJobNotReadyException extends RuntimeException {

    public PaymentExportJobNotReadyException(UUID jobId) {
        super("Export job is not ready: " + jobId);
    }

    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }

    public String error() {
        return "export_job_not_ready";
    }
}
