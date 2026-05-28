package lab.paymentquality.payment.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.payment.internal.application.PaymentActorContext;
import lab.paymentquality.payment.internal.application.PaymentCreateResult;
import lab.paymentquality.payment.internal.application.PaymentOrderService;
import lab.paymentquality.payment.internal.domain.*;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants/{merchantId}/payment-orders")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    public PaymentOrderController(PaymentOrderService paymentOrderService) {
        this.paymentOrderService = paymentOrderService;
    }

    @PostMapping
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(
            @PathVariable UUID merchantId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
            @Valid @RequestBody CreatePaymentOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PaymentAmount amount = PaymentAmount.of(request.amountMinor());
        CurrencyCode currency = CurrencyCode.of(request.currency());
        ClientOrderReference clientRef = ClientOrderReference.of(request.clientOrderReference());
        IdempotencyKey idempotencyKey = IdempotencyKey.of(idempotencyKeyHeader);

        String merchantIdClaim = jwt.getClaimAsString("merchant_id");
        if (merchantIdClaim == null || !merchantId.toString().equals(merchantIdClaim)) {
            throw new AccessDeniedException("Merchant scope mismatch");
        }
        PaymentActorContext actor = new PaymentActorContext(jwt.getSubject());

        PaymentCreateResult result = paymentOrderService.create(
                merchantId, amount, currency, clientRef, idempotencyKey, actor, getCorrelationId());

        PaymentOrderResponse response = PaymentOrderMapper.toResponse(result.paymentOrder());
        String etag = buildEtag(result.paymentOrder());

        if (result.created()) {
            URI location = URI.create("/api/merchants/" + merchantId + "/payment-orders/" + result.paymentOrder().getPaymentOrderId());
            return ResponseEntity.created(location)
                    .header("ETag", etag)
                    .header("X-Correlation-ID", getCorrelationId())
                    .body(response);
        }

        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("X-Correlation-ID", getCorrelationId())
                .body(response);
    }

    @GetMapping("/{paymentOrderId}")
    public ResponseEntity<PaymentOrderResponse> getPaymentOrder(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isPlatformReader = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("platform:payments:read"));

        PaymentOrder order;
        if (isPlatformReader) {
            order = paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        } else {
            String jwtMerchantId = jwt.getClaimAsString("merchant_id");
            if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
                throw new PaymentOrderNotFoundException(paymentOrderId);
            }
            order = paymentOrderService.findForMerchant(merchantId, paymentOrderId);
        }

        PaymentOrderResponse response = PaymentOrderMapper.toResponse(order);
        String etag = buildEtag(order);

        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("X-Correlation-ID", getCorrelationId())
                .body(response);
    }

    private String buildEtag(PaymentOrder order) {
        return "\"po-" + order.getPaymentOrderId() + "-v" + order.getVersion() + "\"";
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

}
