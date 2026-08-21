package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.domain.*;
import lab.paymentquality.tenant.TenantResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        MerchantController.class,
        MerchantImportController.class,
        OrgTreeController.class,
        EntitySearchController.class
})
public class MerchantExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MerchantExceptionHandler.class);

    @ExceptionHandler(InvalidMerchantContactException.class)
    public ResponseEntity<ErrorResponse> handleContactValidation(InvalidMerchantContactException e) {
        log.warn("merchant.patch.failed.validation field={} correlationId={}",
                e.getField(), MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ErrorResponse("validation", "Invalid merchant request", e.getMessage()));
    }

    @ExceptionHandler({InvalidMerchantReferenceException.class, InvalidDisplayNameException.class})
    public ResponseEntity<ErrorResponse> handleValidation(RuntimeException e) {
        String detail = e instanceof InvalidMerchantReferenceException ire
                ? ire.getAttempted() : ((InvalidDisplayNameException) e).getAttempted();
        log.warn("merchant.create.failed.validation detail={} correlationId={}",
                detail, MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("validation", "Invalid merchant request", detail));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException e) {
        Map<String, String> details = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                details.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("merchant.request.failed.validation correlationId={}", MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ErrorResponse("validation", "Invalid merchant request", details));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        Map<String, String> details = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                details.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("merchant.list.failed.validation correlationId={}", MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ErrorResponse("validation", "Invalid merchant request", details));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
        log.warn("merchant.list.failed.validation correlationId={}", MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ErrorResponse("validation", "Invalid merchant request"));
    }

    @ExceptionHandler(SearchQueryRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSearchQueryRequired(SearchQueryRequiredException e) {
        log.warn("search.query.failed.validation correlationId={}", MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ErrorResponse("validation", "Invalid search request", e.getMessage()));
    }

    @ExceptionHandler(OrgTreeInvalidParentException.class)
    public ResponseEntity<ErrorResponse> handleOrgTreeInvalidParent(OrgTreeInvalidParentException e) {
        log.warn("org-tree.parent.failed.validation correlationId={}", MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ErrorResponse("validation", "Invalid org-tree parent", e.getMessage()));
    }

    @ExceptionHandler({MissingTenantReferenceException.class, UnresolvableTenantReferenceException.class})
    public ResponseEntity<ErrorResponse> handleTenantValidation(RuntimeException e) {
        log.warn("merchant.tenant.failed.validation type={} correlationId={}",
                e.getClass().getSimpleName(), MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ErrorResponse("validation", "Invalid merchant request", e.getMessage()));
    }

    @ExceptionHandler({TenantResolutionException.class, TenantBoundaryViolationException.class})
    public ResponseEntity<ErrorResponse> handleTenantForbidden(RuntimeException e) {
        log.warn("merchant.tenant.failed.forbidden type={} correlationId={}",
                e.getClass().getSimpleName(), MDC.get("correlationId"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ErrorResponse("forbidden", "Forbidden", "Access denied"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("merchant.lookup.failed.not-found correlationId={}", MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("validation", "Malformed merchant ID"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("merchant.lookup.failed.not-found correlationId={}", MDC.get("correlationId"));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("validation", "Malformed merchant ID"));
    }

    @ExceptionHandler(DuplicateMerchantReferenceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateMerchantReferenceException e) {
        log.warn("merchant.create.failed.duplicate reference={} correlationId={}",
                e.getConflictingReference(), MDC.get("correlationId"));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.CONFLICT, "duplicate_merchant_reference", "Merchant already exists",
                        "A merchant with this reference already exists"));
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(MerchantNotFoundException e) {
        log.warn("merchant.lookup.failed.not-found correlationId={}", MDC.get("correlationId"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.NOT_FOUND, "not_found", "Not Found", e.getMessage()));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidTransitionException e) {
        log.warn("merchant.status.failed.invalid-transition from={} to={} correlationId={}",
                e.getFrom(), e.getTo(), MDC.get("correlationId"));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("invalid_transition", e.getMessage()));
    }

    @ExceptionHandler(MerchantPreconditionRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePreconditionRequired(MerchantPreconditionRequiredException e) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.PRECONDITION_REQUIRED, "precondition_required", "Precondition Required",
                        e.getMessage()));
    }

    @ExceptionHandler(MalformedMerchantEtagException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedIfMatch(MalformedMerchantEtagException e) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.BAD_REQUEST, "malformed_if_match", "Bad Request", e.getMessage()));
    }

    @ExceptionHandler(MerchantVersionMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleVersionMismatch(MerchantVersionMismatchException e) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.PRECONDITION_FAILED, "merchant_version_mismatch", "Precondition Failed",
                        e.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(
            org.springframework.dao.OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.PRECONDITION_FAILED, "merchant_version_mismatch", "Precondition Failed",
                        "Merchant was modified by another request"));
    }

    @ExceptionHandler(MerchantImportMalformedException.class)
    public ResponseEntity<Map<String, Object>> handleImportMalformed(MerchantImportMalformedException e) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.BAD_REQUEST, "validation", "Bad Request", e.getMessage()));
    }

    @ExceptionHandler(MerchantImportPreviewNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleImportPreviewNotFound(MerchantImportPreviewNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.NOT_FOUND, "not_found", "Not Found", e.getMessage()));
    }

    @ExceptionHandler(MerchantImportAlreadyCommittedException.class)
    public ResponseEntity<Map<String, Object>> handleImportAlreadyCommitted(MerchantImportAlreadyCommittedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(HttpStatus.CONFLICT, "import_already_committed", "Conflict", e.getMessage()));
    }

    private Map<String, Object> problem(HttpStatus status, String error, String title, String detail) {
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
        return body;
    }
}
