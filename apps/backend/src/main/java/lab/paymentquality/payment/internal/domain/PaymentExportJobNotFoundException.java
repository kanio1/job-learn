package lab.paymentquality.payment.internal.domain;

import java.util.UUID;

public class PaymentExportJobNotFoundException extends RuntimeException {

    public PaymentExportJobNotFoundException(UUID jobId) {
        super("Export job not found: " + jobId);
    }
}
