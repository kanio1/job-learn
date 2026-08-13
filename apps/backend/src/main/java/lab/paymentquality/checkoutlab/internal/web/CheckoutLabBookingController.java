package lab.paymentquality.checkoutlab.internal.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabBookingService;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabScenario;
import lab.paymentquality.checkoutlab.internal.application.CreateCheckoutSessionCommand;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillment;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository;
import org.slf4j.MDC;
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
@RequestMapping("/api/checkout-lab/bookings")
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
@Profile("!prod")
class CheckoutLabBookingController {

    public record CreateBookingRequest(
            @NotBlank String mode,
            @NotBlank String extOrderId,
            long amountMinor,
            @NotBlank String currency,
            String continueUrl,
            String notifyUrl,
            Long validitySeconds
    ) {
    }

    private final CheckoutLabBookingService bookingService;
    private final JpaCheckoutFulfillmentRepository fulfillmentRepository;

    CheckoutLabBookingController(
            CheckoutLabBookingService bookingService,
            JpaCheckoutFulfillmentRepository fulfillmentRepository) {
        this.bookingService = bookingService;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    @PostMapping
    ResponseEntity<CheckoutLabBookingService.BookingResult> create(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestHeader(value = "Lab-Force-Scenario", required = false) String forceScenario) {
        if ("CASH".equalsIgnoreCase(request.mode())) {
            return ResponseEntity.ok(bookingService.createCash(
                    request.extOrderId(),
                    request.amountMinor(),
                    request.currency()));
        }
        String resolved = correlationId == null || correlationId.isBlank() ? MDC.get("correlationId") : correlationId;
        CreateCheckoutSessionCommand command = new CreateCheckoutSessionCommand(
                request.extOrderId(),
                request.amountMinor(),
                request.currency(),
                request.continueUrl() == null ? "http://localhost:3000/checkout-lab/return" : request.continueUrl(),
                request.notifyUrl() == null ? "http://localhost:8080/api/checkout-lab/notify" : request.notifyUrl(),
                request.validitySeconds() == null ? 900L : request.validitySeconds(),
                null,
                CheckoutLabScenario.fromHeader(forceScenario));
        return ResponseEntity.ok(bookingService.createOnline(command, resolved));
    }

    @GetMapping("/{bookingId}")
    ResponseEntity<FulfillmentResponse> get(@PathVariable UUID bookingId) {
        CheckoutFulfillment fulfillment = fulfillmentRepository.findById(bookingId).orElse(null);
        if (fulfillment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(FulfillmentResponse.from(fulfillment));
    }
}
