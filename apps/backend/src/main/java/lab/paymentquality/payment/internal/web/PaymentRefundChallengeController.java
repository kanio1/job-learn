package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.application.PaymentMerchantScopeVerifier;
import lab.paymentquality.payment.internal.application.PaymentRefundChallengeService;
import lab.paymentquality.payment.internal.domain.PaymentRefundChallenge;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund-challenges")
public class PaymentRefundChallengeController {

    private final PaymentRefundChallengeService challengeService;
    private final PaymentMerchantScopeVerifier merchantScopeVerifier;

    public PaymentRefundChallengeController(
            PaymentRefundChallengeService challengeService,
            PaymentMerchantScopeVerifier merchantScopeVerifier) {
        this.challengeService = challengeService;
        this.merchantScopeVerifier = merchantScopeVerifier;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChallengeResponse> create(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {
        verifyOwnership(merchantId, jwt, authentication);
        PaymentRefundChallengeService.CreatedChallenge created = challengeService.create(merchantId, paymentOrderId);
        PaymentRefundChallenge challenge = created.challenge();
        URI location = URI.create("/api/merchants/" + merchantId + "/payment-orders/"
                + paymentOrderId + "/refund-challenges/" + challenge.getChallengeId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(location)
                .body(ChallengeResponse.from(challenge, created.pin()));
    }

    @PostMapping(value = "/{challengeId}/verify", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChallengeResponse> verify(
            @PathVariable UUID merchantId,
            @PathVariable UUID paymentOrderId,
            @PathVariable UUID challengeId,
            @RequestBody VerifyPinRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {
        verifyOwnership(merchantId, jwt, authentication);
        PaymentRefundChallenge challenge = challengeService.verify(
                merchantId, paymentOrderId, challengeId,
                request == null ? null : request.pin(),
                jwt.getSubject());
        return ResponseEntity.ok(ChallengeResponse.from(challenge, null));
    }

    private void verifyOwnership(UUID merchantId, Jwt jwt, Authentication authentication) {
        boolean platform = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Authorities.PLATFORM_PAYMENTS_LIFECYCLE)
                        || a.getAuthority().equals(Authorities.PLATFORM_PAYMENTS_READ));
        if (platform) {
            return;
        }
        String merchantIdClaim = jwt.getClaimAsString("merchant_id");
        if (!merchantScopeVerifier.matches(merchantId, merchantIdClaim)) {
            throw new AccessDeniedException("Merchant scope mismatch");
        }
    }

    public record VerifyPinRequest(String pin) {
    }

    public record ChallengeResponse(
            UUID challengeId,
            UUID approvalId,
            Instant expiresAt,
            Instant verifiedAt,
            Instant lockedUntil,
            Integer attemptCount,
            String pin
    ) {
        static ChallengeResponse from(PaymentRefundChallenge challenge, String pin) {
            return new ChallengeResponse(
                    challenge.getChallengeId(),
                    challenge.getApprovalId(),
                    challenge.getExpiresAt(),
                    challenge.getVerifiedAt(),
                    challenge.getLockedUntil(),
                    challenge.getAttemptCount(),
                    pin);
        }
    }
}
