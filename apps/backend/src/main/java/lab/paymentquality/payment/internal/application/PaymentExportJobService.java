package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentExportJob;
import lab.paymentquality.payment.internal.domain.PaymentExportJobNotFoundException;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentExportJobRepository;
import lab.paymentquality.payment.internal.web.PaymentOrderCsvExporter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentExportJobService {

    private final JpaPaymentExportJobRepository jobRepository;
    private final PaymentOrderListService paymentOrderListService;
    private final TransactionTemplate transactionTemplate;

    public PaymentExportJobService(JpaPaymentExportJobRepository jobRepository,
                                   PaymentOrderListService paymentOrderListService,
                                   org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.paymentOrderListService = paymentOrderListService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public CreateResult create(UUID merchantId, String actorSubject, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = jobRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey);
            if (existing.isPresent()) {
                return new CreateResult(existing.get(), false);
            }
        }
        PaymentExportJob created = jobRepository.saveAndFlush(
                PaymentExportJob.pending(merchantId, actorSubject, idempotencyKey));
        return new CreateResult(created, true);
    }

    public record CreateResult(PaymentExportJob job, boolean created) {
    }

    @Transactional(readOnly = true)
    public PaymentExportJob get(UUID merchantId, UUID jobId) {
        PaymentExportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new PaymentExportJobNotFoundException(jobId));
        if (!job.getMerchantId().equals(merchantId)) {
            throw new PaymentExportJobNotFoundException(jobId);
        }
        return job;
    }

    public int processDueJobs(int batchSize) {
        Integer processed = transactionTemplate.execute(status -> doProcess(batchSize));
        return processed == null ? 0 : processed;
    }

    private int doProcess(int batchSize) {
        List<Object> ids = jobRepository.claimPendingIds(batchSize);
        int processed = 0;
        for (Object rawId : ids) {
            UUID id = UUID.fromString(String.valueOf(rawId));
            PaymentExportJob job = jobRepository.findById(id).orElse(null);
            if (job == null) {
                continue;
            }
            try {
                String csv = PaymentOrderCsvExporter.toCsv(paymentOrderListService.findAllForExport(job.getMerchantId()));
                job.markReady(csv);
            } catch (RuntimeException ex) {
                job.markFailed(ex.getMessage());
            }
            processed++;
        }
        return processed;
    }
}
