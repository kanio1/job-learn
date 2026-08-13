package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutEvent;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutEventProcessStatus;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillment;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillmentStatus;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSession;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionStatus;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutEventRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabInboxWorker {

    private static final Logger log = LoggerFactory.getLogger(CheckoutLabInboxWorker.class);

    private final JpaCheckoutEventRepository eventRepository;
    private final JpaCheckoutSessionRepository sessionRepository;
    private final JpaCheckoutFulfillmentRepository fulfillmentRepository;
    private final CheckoutLabClock clock;
    private final TransactionTemplate transactionTemplate;
    private final boolean workerEnabled;

    public CheckoutLabInboxWorker(
            JpaCheckoutEventRepository eventRepository,
            JpaCheckoutSessionRepository sessionRepository,
            JpaCheckoutFulfillmentRepository fulfillmentRepository,
            CheckoutLabClock clock,
            PlatformTransactionManager transactionManager,
            @org.springframework.beans.factory.annotation.Value("${app.checkout-lab.worker-enabled:true}") boolean workerEnabled) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.workerEnabled = workerEnabled;
    }

    @Scheduled(fixedDelayString = "${app.checkout-lab.worker-delay-ms:250}")
    public void poll() {
        if (!workerEnabled) {
            return;
        }
        processBatch(10);
    }

    public int processBatch(int batchSize) {
        Integer processed = transactionTemplate.execute(status -> doProcess(batchSize));
        return processed == null ? 0 : processed;
    }

    private int doProcess(int batchSize) {
        List<UUID> ids = eventRepository.claimNextReceivedIds(batchSize);
        int processed = 0;
        for (UUID id : ids) {
            CheckoutEvent event = eventRepository.findById(id).orElse(null);
            if (event == null) {
                continue;
            }
            try {
                apply(event);
                if (event.getProcessStatus() == CheckoutEventProcessStatus.PROCESSING) {
                    event.markDone();
                }
            } catch (RuntimeException ex) {
                event.markFailed(ex.getMessage());
                log.warn("Inbox event {} failed: {}", event.getEventId(), ex.getMessage());
            }
            processed++;
        }
        return processed;
    }

    private void apply(CheckoutEvent event) {
        CheckoutSession session = sessionRepository.findById(event.getSessionId()).orElse(null);
        if (session == null) {
            event.markFailed("session missing");
            return;
        }
        CheckoutFulfillment fulfillment = fulfillmentRepository.findBySessionId(session.getSessionId())
                .orElse(null);
        if (fulfillment == null) {
            event.markFailed("fulfillment missing");
            return;
        }
        Instant now = clock.instant();
        if (fulfillment.getStatus() != CheckoutFulfillmentStatus.AWAITING_PAYMENT) {
            return;
        }
        if (session.getStatus() == CheckoutSessionStatus.EXPIRED || session.isExpired(now)) {
            session.applyStatus(CheckoutSessionStatus.EXPIRED, now);
            fulfillment.expire(now);
            return;
        }
        if (session.getStatus() == CheckoutSessionStatus.COMPLETED
                && "checkout.session.completed".equals(event.getEventType())) {
            fulfillment.confirm(event.getEventId(), now);
            return;
        }
        if (session.getStatus() == CheckoutSessionStatus.CANCELED
                && "checkout.session.canceled".equals(event.getEventType())) {
            fulfillment.cancel(now);
        }
    }
}
