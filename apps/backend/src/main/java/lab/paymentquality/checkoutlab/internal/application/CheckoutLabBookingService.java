package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillment;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillmentStatus;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSession;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabBookingService {

    public record BookingResult(
            UUID bookingId,
            String mode,
            CheckoutFulfillmentStatus fulfillmentStatus,
            UUID sessionId,
            String redirectUri,
            java.time.Instant validityUntil) {
    }

    private final JpaCheckoutFulfillmentRepository fulfillmentRepository;
    private final CheckoutLabSessionService sessionService;
    private final CheckoutLabClock clock;

    public CheckoutLabBookingService(
            JpaCheckoutFulfillmentRepository fulfillmentRepository,
            CheckoutLabSessionService sessionService,
            CheckoutLabClock clock) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.sessionService = sessionService;
        this.clock = clock;
    }

    @Transactional
    public BookingResult createCash(String extOrderId, long amountMinor, String currency) {
        CheckoutFulfillment fulfillment = CheckoutFulfillment.newFulfillment(
                UUID.randomUUID(),
                null,
                CheckoutFulfillmentStatus.AWAITING_PAYMENT,
                clock.instant(),
                clock.instant());
        fulfillment.confirm("cash:" + extOrderId, clock.instant());
        fulfillmentRepository.save(fulfillment);
        return new BookingResult(
                fulfillment.getFulfillmentId(),
                "CASH",
                fulfillment.getStatus(),
                null,
                null,
                null);
    }

    @Transactional
    public BookingResult createOnline(CreateCheckoutSessionCommand command, String correlationId) {
        CheckoutLabSessionService.CreatedCheckoutSession created = sessionService.createSession(command, correlationId);
        CheckoutFulfillment fulfillment = fulfillmentRepository.findBySessionId(created.sessionId()).orElseThrow();
        CheckoutSession session = sessionService.getSession(created.sessionId());
        return new BookingResult(
                fulfillment.getFulfillmentId(),
                "ONLINE",
                fulfillment.getStatus(),
                created.sessionId(),
                created.redirectUri(),
                session.getValidityUntil());
    }
}
