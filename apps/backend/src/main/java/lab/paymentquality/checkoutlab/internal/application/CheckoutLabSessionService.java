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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
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
    private final CheckoutLabClock clock;
    private final CheckoutLabScenarioService scenarioService;
    private final CheckoutLabNotifier notifier;

    public CheckoutLabSessionService(
            CheckoutLabProperties properties,
            JpaCheckoutSessionRepository sessionRepository,
            JpaCheckoutFulfillmentRepository fulfillmentRepository,
            CheckoutLabClock clock,
            CheckoutLabScenarioService scenarioService,
            CheckoutLabNotifier notifier) {
        this.properties = properties;
        this.sessionRepository = sessionRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.clock = clock;
        this.scenarioService = scenarioService;
        this.notifier = notifier;
    }

    @Transactional
    public CreatedCheckoutSession createSession(CreateCheckoutSessionCommand command, String correlationId) {
        validateAmount(command.amountMinor());
        validateCurrency(command.currency());

        String fingerprint = fingerprint(command);
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            String keyHash = sha256(command.idempotencyKey());
            return sessionRepository.findByIdempotencyKeyHash(keyHash)
                    .map(existing -> replayOrConflict(existing, fingerprint, keyHash))
                    .orElseGet(() -> persistNew(command, correlationId, keyHash));
        }
        return persistNew(command, correlationId, null);
    }

    @Transactional(readOnly = true)
    public CheckoutSession getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CheckoutSessionNotFoundException(sessionId));
    }

    @Transactional(noRollbackFor = CheckoutLinkExpiredException.class)
    public CheckoutSession simulate(UUID sessionId, CheckoutSessionStatus outcome) {
        CheckoutSession session = getSession(sessionId);
        Instant now = clock.instant();
        if (session.isExpired(now) || scenarioService.scenarioFor(sessionId) == CheckoutLabScenario.EXPIRED_LINK) {
            session.applyStatus(CheckoutSessionStatus.EXPIRED, now);
            fulfillmentRepository.findBySessionId(sessionId).ifPresent(fulfillment -> fulfillment.expire(now));
            throw new CheckoutLinkExpiredException(sessionId);
        }
        if (session.getStatus() != CheckoutSessionStatus.CREATED && session.getStatus() != CheckoutSessionStatus.PENDING) {
            return session;
        }
        session.applyStatus(outcome, now);
        if (outcome == CheckoutSessionStatus.COMPLETED) {
            emitAfterCommit(session, outcome, "checkout.session.completed");
        } else if (outcome == CheckoutSessionStatus.CANCELED) {
            emitAfterCommit(session, outcome, "checkout.session.canceled");
        }
        return session;
    }

    private void emitAfterCommit(CheckoutSession session, CheckoutSessionStatus outcome, String eventType) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifier.emit(session, outcome, eventType);
                }
            });
            return;
        }
        notifier.emit(session, outcome, eventType);
    }

    @Transactional
    public CheckoutSession refund(UUID sessionId) {
        CheckoutSession session = getSession(sessionId);
        if (session.getStatus() == CheckoutSessionStatus.REFUNDED) {
            throw new CheckoutRefundNotAllowedException(sessionId);
        }
        if (session.getStatus() != CheckoutSessionStatus.COMPLETED) {
            throw new CheckoutRefundNotAllowedException(sessionId);
        }
        Instant now = clock.instant();
        session.applyStatus(CheckoutSessionStatus.REFUNDED, now);
        emitAfterCommit(session, CheckoutSessionStatus.REFUNDED, "checkout.session.refunded");
        return session;
    }

    private CreatedCheckoutSession persistNew(CreateCheckoutSessionCommand command, String correlationId, String idempotencyHash) {
        UUID sessionId = UUID.randomUUID();
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        String redirectUri = buildRedirectUri(sessionId, command.language());
        Instant validityUntil = now.plusSeconds(command.validitySeconds());
        CheckoutLabScenario scenario = command.scenario() == null
                ? CheckoutLabScenario.HAPPY_COMPLETED
                : command.scenario();
        if (scenario == CheckoutLabScenario.EXPIRED_LINK) {
            validityUntil = now.minusSeconds(1);
        }

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
        if (idempotencyHash != null) {
            session.assignIdempotencyKeyHash(idempotencyHash);
        }
        sessionRepository.save(session);
        scenarioService.assign(sessionId, scenario);

        CheckoutFulfillment fulfillment = CheckoutFulfillment.newFulfillment(
                UUID.randomUUID(),
                sessionId,
                CheckoutFulfillmentStatus.AWAITING_PAYMENT,
                now,
                now);
        fulfillmentRepository.save(fulfillment);

        return new CreatedCheckoutSession(sessionId, redirectUri, CheckoutSessionStatus.CREATED, false);
    }

    private CreatedCheckoutSession replayOrConflict(CheckoutSession existing, String fingerprint, String hash) {
        if (!fingerprint.equals(fingerprintFromSession(existing))) {
            throw new CheckoutIdempotencyConflictException();
        }
        return new CreatedCheckoutSession(
                existing.getSessionId(),
                existing.getRedirectUri(),
                existing.getStatus(),
                true);
    }

    private String fingerprint(CreateCheckoutSessionCommand command) {
        return command.extOrderId() + "|" + command.amountMinor() + "|" + command.currency()
                + "|" + command.continueUrl() + "|" + command.notifyUrl();
    }

    private String fingerprintFromSession(CheckoutSession session) {
        return session.getExtOrderId() + "|" + session.getAmountMinor() + "|" + session.getCurrency()
                + "|" + session.getContinueUrl() + "|" + session.getNotifyUrl();
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

    private String buildRedirectUri(UUID sessionId, String language) {
        String base = properties.hostedCheckoutBaseUrl() + "/psp/checkout/" + sessionId;
        if (language == null || language.isBlank()) {
            return base;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("pl") && !normalized.equals("en")) {
            return base;
        }
        return base + "?lang=" + normalized;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record CreatedCheckoutSession(
            UUID sessionId,
            String redirectUri,
            CheckoutSessionStatus status,
            boolean replayed) {
    }
}
