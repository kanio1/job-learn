package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutAnomalyRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutEventRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutSessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabResetService {

    private final JpaCheckoutAnomalyRepository anomalyRepository;
    private final JpaCheckoutEventRepository eventRepository;
    private final JpaCheckoutFulfillmentRepository fulfillmentRepository;
    private final JpaCheckoutSessionRepository sessionRepository;
    private final CheckoutLabDeliveryLog deliveryLog;
    private final CheckoutLabClock clock;

    public CheckoutLabResetService(
            JpaCheckoutAnomalyRepository anomalyRepository,
            JpaCheckoutEventRepository eventRepository,
            JpaCheckoutFulfillmentRepository fulfillmentRepository,
            JpaCheckoutSessionRepository sessionRepository,
            CheckoutLabDeliveryLog deliveryLog,
            CheckoutLabClock clock) {
        this.anomalyRepository = anomalyRepository;
        this.eventRepository = eventRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.sessionRepository = sessionRepository;
        this.deliveryLog = deliveryLog;
        this.clock = clock;
    }

    @Transactional
    public void reset() {
        anomalyRepository.deleteAll();
        eventRepository.deleteAll();
        fulfillmentRepository.deleteAll();
        sessionRepository.deleteAll();
        deliveryLog.clear();
        clock.resetToSystem();
    }
}
