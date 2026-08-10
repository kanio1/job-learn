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

    CheckoutLabSessionController(CheckoutLabSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/sessions")
    ResponseEntity<CreateCheckoutSessionResponse> createSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String resolvedCorrelationId = resolveCorrelationId(correlationId);
        CreateCheckoutSessionCommand command = new CreateCheckoutSessionCommand(
                request.extOrderId(),
                request.amountMinor(),
                request.currency(),
                request.continueUrl(),
                request.notifyUrl(),
                request.validitySeconds());
        CheckoutLabSessionService.CreatedCheckoutSession created =
                sessionService.createSession(command, resolvedCorrelationId);

        CreateCheckoutSessionResponse body = new CreateCheckoutSessionResponse(
                created.sessionId(),
                created.redirectUri(),
                created.status());

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, created.redirectUri())
                .header("X-Correlation-ID", resolvedCorrelationId)
                .body(body);
    }

    @GetMapping("/sessions/{sessionId}")
    ResponseEntity<CheckoutSessionResponse> getSession(@PathVariable UUID sessionId) {
        CheckoutSessionResponse body = CheckoutSessionMapper.toResponse(sessionService.getSession(sessionId));
        return ResponseEntity.ok(body);
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
