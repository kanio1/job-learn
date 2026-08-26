package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.application.PaymentMerchantScopeVerifier;
import lab.paymentquality.payment.internal.application.PaymentRefundApprovalService;
import lab.paymentquality.payment.internal.domain.IdempotencyKey;
import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentRefundApproval;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund-approvals")
public class PaymentRefundApprovalController {

    private final PaymentRefundApprovalService approvalService;
    private final PaymentMerchantScopeVerifier merchantScopeVerifier;

    public PaymentRefundApprovalController(PaymentRefundApprovalService approvalService,
                                           PaymentMerchantScopeVerifier merchantScopeVerifier) {
        this.approvalService = approvalService;
        this.merchantScopeVerifier = merchantScopeVerifier;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentRefundApprovalResponse> create(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @RequestBody(required = false) RefundRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyOwnership(merchantId, jwt, authentication);
        PaymentRefundApproval approval = approvalService.request(
                merchantId,
                paymentOrderId,
                request != null ? request.amountMinor() : null,
                request != null ? request.reason() : null,
                jwt.getSubject());
        URI location = URI.create("/api/merchants/" + merchantId + "/payment-orders/"
                + paymentOrderId + "/refund-approvals/" + approval.getApprovalId());
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.created(location),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(PaymentRefundApprovalResponse.from(approval));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListResponse> list(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyOwnership(merchantId, jwt, authentication);
        List<PaymentRefundApprovalResponse> content = approvalService.list(merchantId, paymentOrderId).stream()
                .map(PaymentRefundApprovalResponse::from)
                .toList();
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(new ListResponse(content));
    }

    @PostMapping(value = "/{approvalId}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentLifecycleResponse> approve(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @PathVariable UUID approvalId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyHeader,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        verifyOwnership(merchantId, jwt, authentication);
        long expectedVersion = PaymentEtag.requireVersion(ifMatch);
        IdempotencyKey idempotencyKey = IdempotencyKey.of(idempotencyKeyHeader);
        String correlationId = PaymentHttpHeaders.correlationId();
        PaymentOrder order = approvalService.approve(
                merchantId, paymentOrderId, approvalId, jwt.getSubject(),
                idempotencyKey.keyHash(), expectedVersion, correlationId);
        PaymentLifecycleResponse body = PaymentOrderMapper.toLifecycleResponse(order);
        return PaymentHttpHeaders.versionedPaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION_IF_MATCH)
                .header("ETag", PaymentEtag.from(order))
                .body(body);
    }

    private void verifyOwnership(UUID merchantId, Jwt jwt, Authentication authentication) {
        boolean platform = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("platform:payments:lifecycle")
                        || a.getAuthority().equals("platform:payments:read"));
        if (platform) {
            return;
        }
        String merchantIdClaim = jwt.getClaimAsString("merchant_id");
        if (!merchantScopeVerifier.matches(merchantId, merchantIdClaim)) {
            throw new AccessDeniedException("Merchant scope mismatch");
        }
    }

    public record ListResponse(List<PaymentRefundApprovalResponse> content) {
    }

    public record PaymentRefundApprovalResponse(
            UUID approvalId,
            UUID merchantId,
            UUID paymentOrderId,
            Long amountMinor,
            String reason,
            String makerSubject,
            String status,
            Instant createdAt,
            Instant approvedAt
    ) {
        static PaymentRefundApprovalResponse from(PaymentRefundApproval approval) {
            return new PaymentRefundApprovalResponse(
                    approval.getApprovalId(),
                    approval.getMerchantId(),
                    approval.getPaymentOrderId(),
                    approval.getAmountMinor(),
                    approval.getReason(),
                    approval.getMakerSubject(),
                    approval.getStatus().name(),
                    approval.getCreatedAt(),
                    approval.getApprovedAt());
        }
    }
}
