package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.*;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = PaymentOrderController.class)
public class PaymentExceptionHandler {

    private static final String ERROR_VALIDATION = "validation";
    private static final String ERROR_MALFORMED_JSON = "malformed_json";
    private static final String ERROR_UNSUPPORTED_MEDIA_TYPE = "unsupported_media_type";
    private static final String ERROR_FORBIDDEN = "forbidden";
    private static final String ERROR_NOT_FOUND = "not_found";
    private static final String ERROR_MERCHANT_NOT_ELIGIBLE = "merchant_not_payment_eligible";
    private static final String ERROR_IDEMPOTENCY_CONFLICT = "idempotency_conflict";

    private static final String MSG_MALFORMED_JSON = "Request body contains invalid JSON syntax";
    private static final String MSG_UNSUPPORTED_MEDIA_TYPE = "Content-Type must be application/json";
    private static final String MSG_FORBIDDEN = "Access denied";
    private static final String MSG_IDEMPOTENCY_KEY_REQUIRED = "Idempotency-Key header is required for create operations";

    @ExceptionHandler({InvalidPaymentAmountException.class, InvalidCurrencyCodeException.class,
            InvalidClientOrderReferenceException.class, InvalidIdempotencyKeyException.class})
    public ResponseEntity<PaymentErrorResponse> handleValidation(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of(ERROR_VALIDATION, ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PaymentErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<PaymentErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new PaymentErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.withDetails(ERROR_VALIDATION, "Request validation failed", details, getCorrelationId()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<PaymentErrorResponse> handleBindException(BindException ex) {
        List<PaymentErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new PaymentErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.withDetails(ERROR_VALIDATION, "Query parameter validation failed", details, getCorrelationId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PaymentErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of(ERROR_VALIDATION, ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<PaymentErrorResponse> handleDateTimeParse(DateTimeParseException ex) {
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of(ERROR_VALIDATION, "Invalid date format. Expected ISO date (YYYY-MM-DD)", getCorrelationId()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PaymentErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid " + ex.getName() + ": must be a valid UUID";
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of(ERROR_VALIDATION, message, getCorrelationId()));
    }

    @ExceptionHandler(MerchantNotPaymentEligibleException.class)
    public ResponseEntity<PaymentErrorResponse> handleMerchantNotEligible(MerchantNotPaymentEligibleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PaymentErrorResponse.of(ERROR_MERCHANT_NOT_ELIGIBLE, ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<PaymentErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PaymentErrorResponse.of(ERROR_IDEMPOTENCY_CONFLICT, ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(PaymentOrderNotFoundException.class)
    public ResponseEntity<PaymentErrorResponse> handleNotFound(PaymentOrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PaymentErrorResponse.of(ERROR_NOT_FOUND, ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<PaymentErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(PaymentErrorResponse.of(ERROR_FORBIDDEN, MSG_FORBIDDEN, getCorrelationId()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<PaymentErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of(ERROR_MALFORMED_JSON, MSG_MALFORMED_JSON, getCorrelationId()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<PaymentErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(PaymentErrorResponse.of(ERROR_UNSUPPORTED_MEDIA_TYPE, MSG_UNSUPPORTED_MEDIA_TYPE, getCorrelationId()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<PaymentErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        if ("Idempotency-Key".equals(ex.getHeaderName())) {
            return ResponseEntity.badRequest()
                    .body(PaymentErrorResponse.of(ERROR_VALIDATION, MSG_IDEMPOTENCY_KEY_REQUIRED, getCorrelationId()));
        }
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of(ERROR_VALIDATION, "Required header missing: " + ex.getHeaderName(), getCorrelationId()));
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
}
