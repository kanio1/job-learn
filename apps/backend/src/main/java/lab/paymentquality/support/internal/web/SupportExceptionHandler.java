package lab.paymentquality.support.internal.web;

import lab.paymentquality.support.internal.domain.IllegalSupportTransitionException;
import lab.paymentquality.support.internal.domain.SupportCaseNotFoundException;
import lab.paymentquality.support.internal.domain.SupportCaseVersionMismatchException;
import lab.paymentquality.support.internal.domain.SupportMerchantNotFoundException;
import lab.paymentquality.tenant.TenantResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = SupportCaseController.class)
public class SupportExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SupportExceptionHandler.class);

    @ExceptionHandler(SupportCaseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(SupportCaseNotFoundException e) {
        log.warn("support.case.failed.not-found correlationId={}", MDC.get("correlationId"));
        return problem(HttpStatus.NOT_FOUND, "not_found", "Not Found", e.getMessage());
    }

    @ExceptionHandler(SupportMerchantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMerchantNotFound(SupportMerchantNotFoundException e) {
        return problem(HttpStatus.BAD_REQUEST, "validation", "Bad Request", e.getMessage());
    }

    @ExceptionHandler(IllegalSupportTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalTransition(IllegalSupportTransitionException e) {
        log.warn("support.case.failed.illegal-transition from={} to={} correlationId={}",
                e.getFrom(), e.getTo(), MDC.get("correlationId"));
        return problem(HttpStatus.CONFLICT, "illegal_transition", "Conflict", e.getMessage());
    }

    @ExceptionHandler(DuplicateSupportCaseReferenceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateSupportCaseReferenceException e) {
        return problem(HttpStatus.CONFLICT, "duplicate_case_reference", "Conflict", e.getMessage());
    }

    @ExceptionHandler({SupportCaseVersionMismatchException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> handleVersionMismatch(RuntimeException e) {
        return problem(HttpStatus.PRECONDITION_FAILED, "case_version_mismatch", "Precondition Failed",
                "Support case was modified after you loaded it. Reload and retry.");
    }

    @ExceptionHandler(SupportPreconditionRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePreconditionRequired(SupportPreconditionRequiredException e) {
        return problem(HttpStatus.PRECONDITION_REQUIRED, "precondition_required", "Precondition Required",
                e.getMessage());
    }

    @ExceptionHandler(MalformedSupportEtagException.class)
    public ResponseEntity<Map<String, Object>> handleMalformed(MalformedSupportEtagException e) {
        return problem(HttpStatus.BAD_REQUEST, "malformed_if_match", "Bad Request", e.getMessage());
    }

    @ExceptionHandler(InvalidSupportCaseRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidSupportCaseRequestException e) {
        return problem(HttpStatus.BAD_REQUEST, "validation", "Bad Request", e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Map<String, Object>> handleValidation(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "validation", "Bad Request", "Invalid support case request");
    }

    @ExceptionHandler(TenantResolutionException.class)
    public ResponseEntity<Map<String, Object>> handleTenantForbidden(TenantResolutionException e) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "Access denied");
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String error, String title, String detail) {
        String correlationId = MDC.get("correlationId");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://api.payment-quality.local/problems/" + error.replace('_', '-'));
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        if (correlationId != null && !correlationId.isBlank()) {
            body.put("correlationId", correlationId);
        }
        body.put("error", error);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
