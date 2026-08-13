package lab.paymentquality.mirrorlab.internal.web;

import lab.paymentquality.mirrorlab.internal.application.MirrorLabProblemException;
import lab.paymentquality.mirrorlab.internal.application.MirrorLabService;
import lab.paymentquality.shared.security.Authorities;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mirror-lab")
@Profile("!prod")
@ConditionalOnProperty(name = "app.mirror-lab.enabled", havingValue = "true")
class MirrorLabController {

    record AmountRequest(long amountMinor, UUID merchantId) {
    }

    record DisputeRequest(UUID merchantId) {
    }

    private final MirrorLabService service;

    MirrorLabController(MirrorLabService service) {
        this.service = service;
    }

    @PostMapping("/high-value-refunds")
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_LIFECYCLE + "','"
            + Authorities.PLATFORM_PAYMENTS_LIFECYCLE + "')")
    ResponseEntity<Map<String, Object>> highValue(
            @RequestBody AmountRequest request,
            @RequestHeader(value = "X-Lab-Step-Up", required = false) String stepUp) {
        return ResponseEntity.ok(service.highValueRefund(request.amountMinor(), request.merchantId(), stepUp));
    }

    @GetMapping("/statements")
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_READ + "','"
            + Authorities.PLATFORM_PAYMENTS_READ + "','"
            + Authorities.MERCHANT_PAYMENTS_LIFECYCLE + "','"
            + Authorities.PLATFORM_PAYMENTS_LIFECYCLE + "')")
    ResponseEntity<byte[]> statements(@RequestParam(name = "format", defaultValue = "csv") String format) {
        byte[] body = service.statement(format);
        boolean pdf = "pdf".equalsIgnoreCase(format);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(pdf ? MediaType.APPLICATION_PDF : new MediaType("text", "csv"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(pdf ? "statement.pdf" : "statement.csv", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @PostMapping("/disputes")
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_LIFECYCLE + "','"
            + Authorities.PLATFORM_PAYMENTS_LIFECYCLE + "')")
    ResponseEntity<Map<String, Object>> openDispute(@RequestBody DisputeRequest request) {
        var dispute = service.openDispute(request.merchantId());
        return ResponseEntity.ok(Map.of("disputeId", dispute.getDisputeId(), "status", dispute.getStatus()));
    }

    @PostMapping(value = "/disputes/{disputeId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_LIFECYCLE + "','"
            + Authorities.PLATFORM_PAYMENTS_LIFECYCLE + "')")
    ResponseEntity<Map<String, Object>> evidence(
            @PathVariable UUID disputeId,
            @RequestParam("file") MultipartFile file) {
        var dispute = service.uploadEvidence(disputeId, file);
        return ResponseEntity.ok(Map.of("disputeId", dispute.getDisputeId(), "status", dispute.getStatus()));
    }

    @PostMapping("/refund-approvals")
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_LIFECYCLE + "','"
            + Authorities.PLATFORM_PAYMENTS_LIFECYCLE + "')")
    ResponseEntity<Map<String, Object>> createApproval(
            @RequestBody AmountRequest request,
            Authentication authentication) {
        var approval = service.createApproval(request.merchantId(), request.amountMinor(), authentication.getName());
        HashMap<String, Object> body = new HashMap<>();
        body.put("approvalId", approval.getApprovalId());
        body.put("status", approval.getStatus());
        if (approval.getStepUpUntil() != null) {
            body.put("stepUpUntil", approval.getStepUpUntil().toString());
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refund-approvals/{approvalId}/approve")
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANT_PAYMENTS_LIFECYCLE + "','"
            + Authorities.PLATFORM_PAYMENTS_LIFECYCLE + "')")
    ResponseEntity<Map<String, Object>> approve(
            @PathVariable UUID approvalId,
            Authentication authentication) {
        var approval = service.approve(approvalId, authentication.getName());
        return ResponseEntity.ok(Map.of("approvalId", approval.getApprovalId(), "status", approval.getStatus()));
    }

    @PostMapping("/consents")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Map<String, Object>> grant(Authentication authentication) {
        var consent = service.grant(authentication.getName());
        return ResponseEntity.ok(Map.of(
                "consentId", consent.getConsentId(),
                "accessToken", consent.getAccessToken(),
                "status", consent.getStatus()));
    }

    @PostMapping("/consents/{consentId}/revoke")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Map<String, Object>> revoke(@PathVariable UUID consentId, Authentication authentication) {
        service.revoke(consentId, authentication.getName());
        return ResponseEntity.ok(Map.of("status", "REVOKED"));
    }

    @GetMapping("/tpp/accounts")
    ResponseEntity<Map<String, Object>> tpp(
            @RequestParam(name = "token", required = false) String token,
            @RequestHeader(value = "X-Lab-Consent-Token", required = false) String headerToken,
            HttpServletRequest request) {
        service.assertTppRateLimit(request.getRemoteAddr());
        String resolved = headerToken != null && !headerToken.isBlank() ? headerToken : token;
        if (resolved == null || resolved.isBlank()) {
            throw new MirrorLabProblemException(HttpStatus.FORBIDDEN, "consent_denied", "Token required");
        }
        service.requireActiveConsent(resolved);
        return ResponseEntity.ok(Map.of("accounts", List.of(Map.of("iban", "PL00LAB00000000000000000000"))));
    }
}
