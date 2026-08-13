package lab.paymentquality.checkoutlab.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabSessionService;
import lab.paymentquality.checkoutlab.internal.application.CreateCheckoutSessionCommand;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/checkout-lab")
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
@Profile("!prod")
class CheckoutLabSessionController {

    private final CheckoutLabSessionService sessionService;
    private final lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutEventRepository eventRepository;
    private final lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository fulfillmentRepository;
    private final lab.paymentquality.checkoutlab.internal.application.CheckoutLabDeliveryLog deliveryLog;

    CheckoutLabSessionController(
            CheckoutLabSessionService sessionService,
            lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutEventRepository eventRepository,
            lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository fulfillmentRepository,
            lab.paymentquality.checkoutlab.internal.application.CheckoutLabDeliveryLog deliveryLog) {
        this.sessionService = sessionService;
        this.eventRepository = eventRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.deliveryLog = deliveryLog;
    }

    @PostMapping("/sessions")
    ResponseEntity<CreateCheckoutSessionResponse> createSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "Lab-Force-Scenario", required = false) String forceScenario) {
        String resolvedCorrelationId = resolveCorrelationId(correlationId);
        CreateCheckoutSessionCommand command = new CreateCheckoutSessionCommand(
                request.extOrderId(),
                request.amountMinor(),
                request.currency(),
                request.continueUrl(),
                request.notifyUrl(),
                request.validitySeconds(),
                idempotencyKey,
                lab.paymentquality.checkoutlab.internal.application.CheckoutLabScenario.fromHeader(forceScenario),
                request.language());
        CheckoutLabSessionService.CreatedCheckoutSession created =
                sessionService.createSession(command, resolvedCorrelationId);

        CreateCheckoutSessionResponse body = new CreateCheckoutSessionResponse(
                created.sessionId(),
                created.redirectUri(),
                created.status());

        var response = ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, created.redirectUri())
                .header("X-Correlation-ID", resolvedCorrelationId);
        if (created.replayed()) {
            response.header("Idempotency-Replayed", "true");
        }
        return response.body(body);
    }

    @GetMapping("/sessions/{sessionId}/events")
    ResponseEntity<java.util.List<CheckoutEventResponse>> listEvents(@PathVariable UUID sessionId) {
        sessionService.getSession(sessionId);
        return ResponseEntity.ok(eventRepository.findBySessionIdOrderByReceivedAtAsc(sessionId).stream()
                .map(CheckoutEventResponse::from)
                .toList());
    }

    @GetMapping("/sessions/{sessionId}/deliveries")
    ResponseEntity<java.util.List<CheckoutEventResponse.DeliveryResponse>> deliveries(@PathVariable UUID sessionId) {
        sessionService.getSession(sessionId);
        return ResponseEntity.ok(deliveryLog.forSession(sessionId).stream()
                .map(CheckoutEventResponse.DeliveryResponse::from)
                .toList());
    }

    @GetMapping("/sessions/{sessionId}/fulfillment")
    ResponseEntity<FulfillmentResponse> fulfillment(@PathVariable UUID sessionId) {
        sessionService.getSession(sessionId);
        return fulfillmentRepository.findBySessionId(sessionId)
                .map(found -> ResponseEntity.ok(FulfillmentResponse.from(found)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/sessions/{sessionId}")
    ResponseEntity<CheckoutSessionResponse> getSession(@PathVariable UUID sessionId) {
        CheckoutSessionResponse body = CheckoutSessionMapper.toResponse(sessionService.getSession(sessionId));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/sessions/{sessionId}/refund")
    ResponseEntity<CheckoutSessionResponse> refund(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(CheckoutSessionMapper.toResponse(sessionService.refund(sessionId)));
    }

    private String resolveCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        String mdcCorrelationId = MDC.get("correlationId");
        if (mdcCorrelationId != null && !mdcCorrelationId.isBlank()) {
            return mdcCorrelationId;
        }
        return UUID.randomUUID().toString();
    }
}
