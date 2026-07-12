package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentLifecycleAction;
import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentOrderStatusHistory;
import lab.paymentquality.payment.internal.domain.PaymentStatus;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderStatusHistoryRepository;
import lab.paymentquality.shared.events.AuditableActionEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Sweeps AUTHORIZED payment orders whose authorization window has passed
 * (F-D1). Invoked periodically by {@link PaymentExpirationScheduler}, but
 * kept as a plain injectable service so the sweep logic itself can be unit
 * tested without a Spring scheduling context.
 *
 * This complements — does not replace — the existing lazy check in
 * {@link PaymentOrder#capture}: a capture attempt on an overdue order still
 * expires it immediately, regardless of whether the sweep has run yet.
 */
@Service
@Transactional
public class PaymentExpirationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpirationService.class);

    private final JpaPaymentOrderRepository paymentOrderRepository;
    private final JpaPaymentOrderStatusHistoryRepository statusHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentExpirationService(
            JpaPaymentOrderRepository paymentOrderRepository,
            JpaPaymentOrderStatusHistoryRepository statusHistoryRepository,
            ApplicationEventPublisher eventPublisher) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    /** @return the number of orders transitioned to EXPIRED in this sweep. */
    public int expireOverdueAuthorizations() {
        List<PaymentOrder> overdue = paymentOrderRepository
                .findAllByStatusAndExpiresAtBefore(PaymentStatus.AUTHORIZED, Instant.now());

        for (PaymentOrder order : overdue) {
            PaymentStatus previousStatus = order.getStatus();
            order.expire();
            paymentOrderRepository.saveAndFlush(order);

            PaymentOrderStatusHistory entry = PaymentOrderStatusHistory.lifecycleEntry(
                    order.getPaymentOrderId(), previousStatus, order.getStatus(),
                    PaymentLifecycleAction.EXPIRE, null, null, null, null, null, null);
            statusHistoryRepository.saveAndFlush(entry);

            eventPublisher.publishEvent(AuditableActionEventFactory.success(
                    "PAYMENT_EXPIRED",
                    "PAYMENT_ORDER",
                    order.getPaymentOrderId().toString(),
                    null,
                    null,
                    null,
                    Map.of("status", previousStatus.name()),
                    Map.of("status", order.getStatus().name())));

            log.info("payment.expire.succeeded paymentOrderId={} previousStatus={}",
                    order.getPaymentOrderId(), previousStatus);
        }

        return overdue.size();
    }
}
