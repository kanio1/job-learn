package lab.paymentquality.tenant.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.internal.domain.InvalidPaymentPolicyException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// F-C4: Translates tenant settings domain exceptions to RFC 9457 Problem Details.
@RestControllerAdvice(assignableTypes = TenantSettingsController.class)
class TenantSettingsExceptionHandler {

    private static final String PROBLEM_BASE = "https://api.payment-quality.local/problems/";

    @ExceptionHandler(TenantSettingsPreconditionRequiredException.class)
    ResponseEntity<Map<String, Object>> handlePreconditionRequired(
            TenantSettingsPreconditionRequiredException ex) {
        return problem(HttpStatus.PRECONDITION_REQUIRED,
                "precondition_required", "Precondition Required", ex.getMessage());
    }

    @ExceptionHandler(TenantSettingsPreconditionFailedException.class)
    ResponseEntity<Map<String, Object>> handlePreconditionFailed(
            TenantSettingsPreconditionFailedException ex) {
        return problem(HttpStatus.PRECONDITION_FAILED,
                "tenant_settings_version_mismatch", "Precondition Failed", ex.getMessage());
    }

    @ExceptionHandler(TenantResolutionException.class)
    ResponseEntity<Map<String, Object>> handleTenantResolution(TenantResolutionException ex) {
        return problem(HttpStatus.FORBIDDEN,
                "tenant_access_denied", "Tenant access denied", ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentPolicyException.class)
    ResponseEntity<Map<String, Object>> handleInvalidPaymentPolicy(InvalidPaymentPolicyException ex) {
        return problem(HttpStatus.BAD_REQUEST, "validation", "Validation Failed", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        String detail = errors.isEmpty() ? "Validation failed" : String.join("; ", errors);
        return problem(HttpStatus.BAD_REQUEST, "validation", "Validation Failed", detail);
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String code,
                                                         String title, String detail) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", PROBLEM_BASE + code.replace('_', '-'));
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("code", code);
        body.put("correlationId", correlationId);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
