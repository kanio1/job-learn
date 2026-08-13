package lab.paymentquality.checkoutlab.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabSessionService;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabSignatureService;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabSimulateToken;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabClock;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillment;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSession;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionStatus;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/checkout-lab/hosted/sessions")
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
@Profile("!prod")
class CheckoutLabHostedController {

    private final CheckoutLabSessionService sessionService;
    private final CheckoutLabSignatureService signatureService;
    private final CheckoutLabClock clock;
    private final JpaCheckoutFulfillmentRepository fulfillmentRepository;

    CheckoutLabHostedController(
            CheckoutLabSessionService sessionService,
            CheckoutLabSignatureService signatureService,
            CheckoutLabClock clock,
            JpaCheckoutFulfillmentRepository fulfillmentRepository) {
        this.sessionService = sessionService;
        this.signatureService = signatureService;
        this.clock = clock;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    @GetMapping("/{sessionId}")
    ResponseEntity<HostedCheckoutSessionResponse> get(@PathVariable UUID sessionId) {
        CheckoutSession session = sessionService.getSession(sessionId);
        return ResponseEntity.ok(toHosted(session));
    }

    @PostMapping("/{sessionId}/simulate")
    ResponseEntity<HostedCheckoutSessionResponse> simulate(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "Lab-Simulate-Token", required = false) String simulateToken,
            @Valid @RequestBody SimulateCheckoutRequest request) {
        signatureService.verifySimulateToken(sessionId, simulateToken, sessionService.getSession(sessionId).getValidityUntil());
        CheckoutSession session = sessionService.simulate(sessionId, request.toStatus());
        return ResponseEntity.ok(toHosted(session));
    }

    @GetMapping("/{sessionId}/fulfillment")
    ResponseEntity<FulfillmentResponse> fulfillment(@PathVariable UUID sessionId) {
        CheckoutFulfillment fulfillment = fulfillmentRepository.findBySessionId(sessionId).orElse(null);
        if (fulfillment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(FulfillmentResponse.from(fulfillment));
    }

    private HostedCheckoutSessionResponse toHosted(CheckoutSession session) {
        String simulateToken = null;
        java.time.Instant simulateTokenExpiresAt = null;
        if (session.getStatus() == CheckoutSessionStatus.CREATED
                || session.getStatus() == CheckoutSessionStatus.PENDING) {
            if (!session.isExpired(clock.instant())) {
                CheckoutLabSimulateToken token = signatureService.issueSimulateToken(
                        session.getSessionId(),
                        session.getValidityUntil());
                simulateToken = token.token();
                simulateTokenExpiresAt = token.expiresAt();
            }
        }
        return new HostedCheckoutSessionResponse(
                session.getSessionId(),
                session.getExtOrderId(),
                session.getStatus(),
                session.getAmountMinor(),
                session.getCurrency(),
                session.getValidityUntil(),
                session.getContinueUrl(),
                simulateToken,
                simulateTokenExpiresAt);
    }
}
