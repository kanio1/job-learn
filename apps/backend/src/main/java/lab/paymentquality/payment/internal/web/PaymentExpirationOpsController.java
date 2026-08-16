package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.application.PaymentExpirationService;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-ops")
public class PaymentExpirationOpsController {

    private final PaymentExpirationService expirationService;

    public PaymentExpirationOpsController(PaymentExpirationService expirationService) {
        this.expirationService = expirationService;
    }

    @PostMapping(value = "/expiration-sweep", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + Authorities.PLATFORM_PAYMENTS_LIFECYCLE + "')")
    public ResponseEntity<SweepResponse> sweep() {
        int expiredCount = expirationService.expireOverdueAuthorizations();
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(new SweepResponse(expiredCount));
    }

    public record SweepResponse(int expiredCount) {
    }
}
