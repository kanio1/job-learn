package lab.paymentquality.payment.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import lab.paymentquality.payment.internal.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.time.format.DateTimeParseException;

import java.util.List;

@RestControllerAdvice(assignableTypes = PaymentOrderController.class)
public class PaymentExceptionHandler {

    private static final String ERROR_VALIDATION = "validation";
    private static final String ERROR_MALFORMED_JSON = "malformed_json";
    private static final String ERROR_UNSUPPORTED_MEDIA_TYPE = "unsupported_media_type";
    private static final String ERROR_FORBIDDEN = "forbidden";
    private static final String ERROR_NOT_FOUND = "not_found";
    private static final String ERROR_MERCHANT_NOT_ELIGIBLE = "merchant_not_payment_eligible";
    private static final String ERROR_IDEMPOTENCY_CONFLICT = "idempotency_conflict";
    private static final String ERROR_INVALID_TRANSITION = "invalid_transition";
    private static final String ERROR_AUTHORIZATION_EXPIRED = "authorization_expired";
    private static final String ERROR_CAPTURE_AMOUNT_EXCEEDS_AUTHORIZED = "capture_amount_exceeds_authorized";
    private static final String ERROR_REFUND_AMOUNT_EXCEEDS_CAPTURED = "refund_amount_exceeds_captured";
    private static final String ERROR_CONCURRENCY_CONFLICT = "concurrency_conflict";
    private static final String ERROR_MISSING_REQUIRED_HEADER = "missing_required_header";
    private static final String ERROR_PRECONDITION_REQUIRED = "precondition_required";
    private static final String ERROR_MALFORMED_IF_MATCH = "malformed_if_match";
    private static final String ERROR_PAYMENT_ORDER_VERSION_MISMATCH = "payment_order_version_mismatch";
    private static final String ERROR_NOT_ACCEPTABLE = "not_acceptable";
    private static final String ERROR_METHOD_NOT_ALLOWED = "method_not_allowed";
    private static final String ERROR_UNKNOWN_TOP_LEVEL_FIELD = "unknown_top_level_field";

    private static final String MSG_MALFORMED_JSON = "Request body contains invalid JSON syntax";
    private static final String MSG_UNSUPPORTED_MEDIA_TYPE = "Content-Type must be application/json or application/merge-patch+json where PATCH is supported";
    private static final String MSG_FORBIDDEN = "Access denied";
    private static final String MSG_IDEMPOTENCY_KEY_REQUIRED = "Idempotency-Key header is required for create operations";

    @ExceptionHandler({InvalidPaymentAmountException.class, InvalidCurrencyCodeException.class,
            InvalidClientOrderReferenceException.class, InvalidIdempotencyKeyException.class})
    public ResponseEntity<PaymentErrorResponse> handleValidation(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ERROR_VALIDATION, ex.getMessage(), headersForRequest(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PaymentErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                             HttpServletRequest request) {
        List<PaymentErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new PaymentErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, ERROR_VALIDATION, "Request validation failed",
                details, headersForRequest(request));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<PaymentErrorResponse> handleBindException(BindException ex) {
        List<PaymentErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new PaymentErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, ERROR_VALIDATION, "Query parameter validation failed", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PaymentErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, ERROR_VALIDATION, ex.getMessage());
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<PaymentErrorResponse> handleDateTimeParse(DateTimeParseException ex) {
        return problem(HttpStatus.BAD_REQUEST, ERROR_VALIDATION, "Invalid date format. Expected ISO date (YYYY-MM-DD)");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PaymentErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid " + ex.getName() + ": must be a valid UUID";
        return problem(HttpStatus.BAD_REQUEST, ERROR_VALIDATION, message);
    }

    @ExceptionHandler(MerchantNotPaymentEligibleException.class)
    public ResponseEntity<PaymentErrorResponse> handleMerchantNotEligible(MerchantNotPaymentEligibleException ex,
                                                                          HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ERROR_MERCHANT_NOT_ELIGIBLE, ex.getMessage(), headersForRequest(request));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<PaymentErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex,
                                                                          HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ERROR_IDEMPOTENCY_CONFLICT, ex.getMessage(), headersForRequest(request));
    }

    @ExceptionHandler(PaymentOrderNotFoundException.class)
    public ResponseEntity<PaymentErrorResponse> handleNotFound(PaymentOrderNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ERROR_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<PaymentErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, ERROR_FORBIDDEN, MSG_FORBIDDEN);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<PaymentErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, ERROR_MALFORMED_JSON, MSG_MALFORMED_JSON);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<PaymentErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ERROR_UNSUPPORTED_MEDIA_TYPE,
                MSG_UNSUPPORTED_MEDIA_TYPE, addAcceptPatchHeader());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<PaymentErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex,
                                                                           HttpServletRequest request) {
        if ("Idempotency-Key".equals(ex.getHeaderName())) {
            return problem(HttpStatus.BAD_REQUEST, ERROR_VALIDATION, MSG_IDEMPOTENCY_KEY_REQUIRED,
                    headersForRequest(request));
        }
        return problem(HttpStatus.BAD_REQUEST, ERROR_MISSING_REQUIRED_HEADER, "Required header missing: " + ex.getHeaderName());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<PaymentErrorResponse> handleInvalidStateTransition(InvalidStateTransitionException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_INVALID_TRANSITION, ex.getMessage(), preconditionHeaders());
    }

    @ExceptionHandler(AuthorizationExpiredException.class)
    public ResponseEntity<PaymentErrorResponse> handleAuthorizationExpired(AuthorizationExpiredException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_AUTHORIZATION_EXPIRED, ex.getMessage(), preconditionHeaders());
    }

    @ExceptionHandler(InvalidCaptureAmountException.class)
    public ResponseEntity<PaymentErrorResponse> handleInvalidCaptureAmount(InvalidCaptureAmountException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_CAPTURE_AMOUNT_EXCEEDS_AUTHORIZED, ex.getMessage(), preconditionHeaders());
    }

    @ExceptionHandler(InvalidRefundAmountException.class)
    public ResponseEntity<PaymentErrorResponse> handleInvalidRefundAmount(InvalidRefundAmountException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_REFUND_AMOUNT_EXCEEDS_CAPTURED, ex.getMessage(), preconditionHeaders());
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<PaymentErrorResponse> handleOptimisticLock(org.springframework.dao.OptimisticLockingFailureException ex) {
        return problem(HttpStatus.PRECONDITION_FAILED, ERROR_CONCURRENCY_CONFLICT,
                "Payment order was modified by another request", preconditionHeaders());
    }

    @ExceptionHandler(PaymentPreconditionRequiredException.class)
    public ResponseEntity<PaymentErrorResponse> handlePreconditionRequired(PaymentPreconditionRequiredException ex) {
        return problem(HttpStatus.PRECONDITION_REQUIRED, ERROR_PRECONDITION_REQUIRED, ex.getMessage(), preconditionHeaders());
    }

    @ExceptionHandler(MalformedPaymentEtagException.class)
    public ResponseEntity<PaymentErrorResponse> handleMalformedPaymentEtag(MalformedPaymentEtagException ex) {
        return problem(HttpStatus.BAD_REQUEST, ERROR_MALFORMED_IF_MATCH, ex.getMessage(), preconditionHeaders());
    }

    @ExceptionHandler(PaymentOrderVersionMismatchException.class)
    public ResponseEntity<PaymentErrorResponse> handlePaymentOrderVersionMismatch(PaymentOrderVersionMismatchException ex) {
        return problem(HttpStatus.PRECONDITION_FAILED, ERROR_PAYMENT_ORDER_VERSION_MISMATCH, ex.getMessage(), preconditionHeaders());
    }

    @ExceptionHandler(UnknownMetadataPatchFieldException.class)
    public ResponseEntity<PaymentErrorResponse> handleUnknownMetadataPatchField(UnknownMetadataPatchFieldException ex) {
        List<PaymentErrorResponse.FieldError> details = ex.fieldNames().stream()
                .map(fieldName -> new PaymentErrorResponse.FieldError(fieldName,
                        "Unknown top-level field is not allowed for metadata PATCH"))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, ERROR_UNKNOWN_TOP_LEVEL_FIELD, ex.getMessage(),
                details, preconditionHeaders());
    }

    @ExceptionHandler(UnknownCreatePaymentOrderFieldException.class)
    public ResponseEntity<PaymentErrorResponse> handleUnknownCreatePaymentOrderField(UnknownCreatePaymentOrderFieldException ex) {
        List<PaymentErrorResponse.FieldError> details = ex.fieldNames().stream()
                .map(fieldName -> new PaymentErrorResponse.FieldError(fieldName,
                        "Unknown top-level field is not allowed for create payment order"))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, ERROR_UNKNOWN_TOP_LEVEL_FIELD, ex.getMessage(),
                details, createHeaders());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<PaymentErrorResponse> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return problem(HttpStatus.NOT_ACCEPTABLE, ERROR_NOT_ACCEPTABLE, "Accept header must allow application/json");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<PaymentErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        HttpHeaders headers = paymentErrorHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ERROR_METHOD_NOT_ALLOWED,
                "HTTP method is not allowed for this payment resource", headers);
    }

    private ResponseEntity<PaymentErrorResponse> problem(HttpStatus status, String error, String message) {
        return problem(status, error, message, paymentErrorHeaders());
    }

    private ResponseEntity<PaymentErrorResponse> problem(HttpStatus status, String error, String message,
                                                         List<PaymentErrorResponse.FieldError> details) {
        return problem(status, error, message, details, paymentErrorHeaders());
    }

    private ResponseEntity<PaymentErrorResponse> problem(HttpStatus status, String error, String message,
                                                         HttpHeaders headers) {
        return problem(status, error, message, null, headers);
    }

    private ResponseEntity<PaymentErrorResponse> problem(HttpStatus status, String error, String message,
                                                         List<PaymentErrorResponse.FieldError> details,
                                                         HttpHeaders headers) {
        String correlationId = headers.getFirst(PaymentHttpHeaders.X_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = PaymentHttpHeaders.correlationId();
            headers.set(PaymentHttpHeaders.X_CORRELATION_ID, correlationId);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .headers(headers)
                .body(problemBody(status, error, message, details, correlationId));
    }

    private PaymentErrorResponse problemBody(HttpStatus status, String error, String message,
                                             List<PaymentErrorResponse.FieldError> details,
                                             String correlationId) {
        return PaymentErrorResponse.of(error, message, details,
                correlationId, status.value(), status.getReasonPhrase());
    }

    private HttpHeaders paymentErrorHeaders() {
        return PaymentHttpHeaders.sensitivePaymentHeaders(PaymentHttpHeaders.VARY_AUTHORIZATION);
    }

    private HttpHeaders preconditionHeaders() {
        return PaymentHttpHeaders.sensitivePaymentHeaders(PaymentHttpHeaders.VARY_AUTHORIZATION_IF_MATCH);
    }

    private HttpHeaders createHeaders() {
        return PaymentHttpHeaders.sensitivePaymentHeaders(PaymentHttpHeaders.VARY_AUTHORIZATION_IDEMPOTENCY_KEY);
    }

    private HttpHeaders headersForRequest(HttpServletRequest request) {
        if (isCreatePaymentOrderRequest(request)) {
            return createHeaders();
        }
        if (isConditionalPaymentMutationRequest(request)) {
            return preconditionHeaders();
        }
        return paymentErrorHeaders();
    }

    private boolean isCreatePaymentOrderRequest(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && request.getRequestURI().matches("/api/merchants/[^/]+/payment-orders/?");
    }

    private boolean isConditionalPaymentMutationRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return ("PATCH".equals(request.getMethod()) && uri.matches("/api/merchants/[^/]+/payment-orders/[^/]+/?"))
                || ("POST".equals(request.getMethod())
                && uri.matches("/api/merchants/[^/]+/payment-orders/[^/]+/(authorize|capture|cancel|refund)/?"));
    }

    private HttpHeaders addAcceptPatchHeader() {
        HttpHeaders headers = paymentErrorHeaders();
        headers.set(PaymentHttpHeaders.ACCEPT_PATCH, PaymentHttpHeaders.MERGE_PATCH_JSON);
        return headers;
    }
}
