package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.application.PaymentExportJobService;
import lab.paymentquality.payment.internal.application.PaymentMerchantScopeVerifier;
import lab.paymentquality.payment.internal.domain.PaymentExportJob;
import lab.paymentquality.payment.internal.domain.PaymentExportJobNotReadyException;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants/{merchantId}/payment-orders/export-jobs")
public class PaymentExportJobController {

    private final PaymentExportJobService exportJobService;
    private final PaymentMerchantScopeVerifier merchantScopeVerifier;

    public PaymentExportJobController(PaymentExportJobService exportJobService,
                                      PaymentMerchantScopeVerifier merchantScopeVerifier) {
        this.exportJobService = exportJobService;
        this.merchantScopeVerifier = merchantScopeVerifier;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentExportJobResponse> create(
            @PathVariable UUID merchantId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        requireExportAccess(merchantId, authentication, jwt);
        PaymentExportJobService.CreateResult result = exportJobService.create(
                merchantId, jwt.getSubject(), idempotencyKey);
        PaymentExportJob job = result.job();
        URI location = URI.create("/api/merchants/" + merchantId + "/payment-orders/export-jobs/" + job.getJobId());
        HttpStatus status = result.created() || job.getStatus() == PaymentExportJob.Status.PENDING
                ? HttpStatus.ACCEPTED
                : HttpStatus.OK;
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.status(status),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .header(HttpHeaders.LOCATION, location.toString())
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(PaymentExportJobResponse.from(job));
    }

    @GetMapping(value = "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentExportJobResponse> get(
            @PathVariable UUID merchantId,
            @PathVariable UUID jobId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        requireExportAccess(merchantId, authentication, jwt);
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .body(PaymentExportJobResponse.from(exportJobService.get(merchantId, jobId)));
    }

    @GetMapping(value = "/{jobId}/content", produces = "text/csv;charset=utf-8")
    public ResponseEntity<byte[]> content(
            @PathVariable UUID merchantId,
            @PathVariable UUID jobId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        requireExportAccess(merchantId, authentication, jwt);
        PaymentExportJob job = exportJobService.get(merchantId, jobId);
        if (job.getStatus() != PaymentExportJob.Status.READY || job.getCsvContent() == null) {
            throw new PaymentExportJobNotReadyException(jobId);
        }
        return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(),
                        PaymentHttpHeaders.VARY_AUTHORIZATION)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"payment-orders-" + merchantId + "-" + jobId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=utf-8"))
                .body(job.getCsvContent().getBytes(StandardCharsets.UTF_8));
    }

    private void requireExportAccess(UUID merchantId, Authentication authentication, Jwt jwt) {
        boolean allowed = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Authorities.MERCHANT_PAYMENTS_READ)
                        || a.getAuthority().equals(Authorities.PLATFORM_PAYMENTS_READ));
        if (!allowed) {
            throw new AccessDeniedException("Export not permitted");
        }
        boolean platformReader = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Authorities.PLATFORM_PAYMENTS_READ));
        if (platformReader) {
            return;
        }
        if (!merchantScopeVerifier.matches(merchantId, jwt.getClaimAsString("merchant_id"))) {
            throw new AccessDeniedException("Merchant scope mismatch");
        }
    }

    public record PaymentExportJobResponse(
            UUID jobId,
            UUID merchantId,
            String status,
            Instant createdAt,
            Instant completedAt,
            String errorMessage
    ) {
        static PaymentExportJobResponse from(PaymentExportJob job) {
            return new PaymentExportJobResponse(
                    job.getJobId(),
                    job.getMerchantId(),
                    job.getStatus().name(),
                    job.getCreatedAt(),
                    job.getCompletedAt(),
                    job.getErrorMessage());
        }
    }
}
