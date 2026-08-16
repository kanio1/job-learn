package lab.paymentquality.payment.internal.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payment.export-jobs.worker-enabled", havingValue = "true", matchIfMissing = true)
public class PaymentExportJobWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentExportJobWorker.class);

    private final PaymentExportJobService exportJobService;

    public PaymentExportJobWorker(PaymentExportJobService exportJobService) {
        this.exportJobService = exportJobService;
    }

    @Scheduled(fixedDelayString = "${payment.export-jobs.worker-delay-ms:250}")
    public void poll() {
        int processed = exportJobService.processDueJobs(10);
        if (processed > 0) {
            log.info("payment.export-jobs.processed count={}", processed);
        }
    }
}
