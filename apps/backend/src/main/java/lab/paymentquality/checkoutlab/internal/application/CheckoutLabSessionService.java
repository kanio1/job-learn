package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.config.CheckoutLabProperties;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillment;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillmentStatus;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSession;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionNotFoundException;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionStatus;
import lab.paymentquality.checkoutlab.internal.domain.InvalidCheckoutAmountException;
import lab.paymentquality.checkoutlab.internal.domain.InvalidCheckoutCurrencyException;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutSessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabSessionService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("PLN", "EUR", "USD");
    private static final long MIN_AMOUNT_MINOR = 1L;
    private static final long MAX_AMOUNT_MINOR = 100_000_000L;

    private final CheckoutLabProperties properties;
    private final JpaCheckoutSessionRepository sessionRepository;
    private final JpaCheckoutFulfillmentRepository fulfillmentRepository;
    private final Clock clock;

    public CheckoutLabSessionService(
            CheckoutLabProperties properties,
            JpaCheckoutSessionRepository sessionRepository,
            JpaCheckoutFulfillmentRepository fulfillmentRepository) {
        this.properties = properties;
        this.sessionRepository = sessionRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public CreatedCheckoutSession createSession(CreateCheckoutSessionCommand command, String correlationId) {
        validateAmount(command.amountMinor());
        validateCurrency(command.currency());

        UUID sessionId = UUID.randomUUID();
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        String redirectUri = buildRedirectUri(sessionId);
        Instant validityUntil = now.plusSeconds(command.validitySeconds());

        CheckoutSession session = CheckoutSession.newSession(
                sessionId,
                command.extOrderId(),
                command.amountMinor(),
                command.currency(),
                CheckoutSessionStatus.CREATED,
                command.continueUrl(),
                command.notifyUrl(),
                redirectUri,
                validityUntil,
                correlationId,
                now,
                now);
        sessionRepository.save(session);

        CheckoutFulfillment fulfillment = CheckoutFulfillment.newFulfillment(
                UUID.randomUUID(),
                sessionId,
                CheckoutFulfillmentStatus.AWAITING_PAYMENT,
                now,
                now);
        fulfillmentRepository.save(fulfillment);

        return new CreatedCheckoutSession(sessionId, redirectUri, CheckoutSessionStatus.CREATED);
    }

    @Transactional(readOnly = true)
    public CheckoutSession getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CheckoutSessionNotFoundException(sessionId));
    }

    private void validateAmount(long amountMinor) {
        if (amountMinor < MIN_AMOUNT_MINOR || amountMinor > MAX_AMOUNT_MINOR) {
            throw new InvalidCheckoutAmountException(amountMinor);
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || !SUPPORTED_CURRENCIES.contains(currency)) {
            throw new InvalidCheckoutCurrencyException(currency);
        }
    }

    private String buildRedirectUri(UUID sessionId) {
        return properties.hostedCheckoutBaseUrl() + "/psp/checkout/" + sessionId;
    }

    public record CreatedCheckoutSession(
            UUID sessionId,
            String redirectUri,
            CheckoutSessionStatus status) {
    }
}
