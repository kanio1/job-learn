package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutAnomaly;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillmentStatus;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSession;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionStatus;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutAnomalyRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutSessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabReconcileService {

    private final JpaCheckoutSessionRepository sessionRepository;
    private final JpaCheckoutFulfillmentRepository fulfillmentRepository;
    private final JpaCheckoutAnomalyRepository anomalyRepository;
    private final CheckoutLabClock clock;
    private final boolean reconcileEnabled;

    public CheckoutLabReconcileService(
            JpaCheckoutSessionRepository sessionRepository,
            JpaCheckoutFulfillmentRepository fulfillmentRepository,
            JpaCheckoutAnomalyRepository anomalyRepository,
            CheckoutLabClock clock,
            @org.springframework.beans.factory.annotation.Value("${app.checkout-lab.reconcile-enabled:true}") boolean reconcileEnabled) {
        this.sessionRepository = sessionRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.anomalyRepository = anomalyRepository;
        this.clock = clock;
        this.reconcileEnabled = reconcileEnabled;
    }

    @Scheduled(fixedDelayString = "${app.checkout-lab.reconcile-delay-ms:5000}")
    public void scheduledReconcile() {
        if (!reconcileEnabled) {
            return;
        }
        reconcile();
    }

    @Transactional
    public int reconcile() {
        int created = 0;
        for (CheckoutSession session : sessionRepository.findAll()) {
            var fulfillment = fulfillmentRepository.findBySessionId(session.getSessionId()).orElse(null);
            if (fulfillment == null) {
                continue;
            }
            boolean mismatch = session.getStatus() == CheckoutSessionStatus.COMPLETED
                    && fulfillment.getStatus() == CheckoutFulfillmentStatus.AWAITING_PAYMENT;
            if (mismatch) {
                if (anomalyRepository.existsBySessionIdAndKind(
                        session.getSessionId(),
                        "session_completed_fulfillment_pending")) {
                    continue;
                }
                try {
                    anomalyRepository.save(CheckoutAnomaly.detected(
                            UUID.randomUUID(),
                            session.getSessionId(),
                            "session_completed_fulfillment_pending",
                            "Session is COMPLETED but fulfillment is still AWAITING_PAYMENT",
                            clock.instant()));
                    created++;
                } catch (DataIntegrityViolationException duplicate) {
                    // Another reconcile run inserted the same anomaly concurrently.
                }
            }
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<CheckoutAnomaly> list() {
        return anomalyRepository.findAllByOrderByDetectedAtDesc();
    }
}
