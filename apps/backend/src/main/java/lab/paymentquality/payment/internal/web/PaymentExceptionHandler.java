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

import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = PaymentOrderController.class)
public class PaymentExceptionHandler {

    @ExceptionHandler({InvalidPaymentAmountException.class, InvalidCurrencyCodeException.class,
            InvalidClientOrderReferenceException.class, InvalidIdempotencyKeyException.class})
    public ResponseEntity<PaymentErrorResponse> handleValidation(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of("validation", ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PaymentErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<PaymentErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new PaymentErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.withDetails("validation", "Request validation failed", details, getCorrelationId()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<PaymentErrorResponse> handleBindException(BindException ex) {
        List<PaymentErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new PaymentErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.withDetails("validation", "Query parameter validation failed", details, getCorrelationId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PaymentErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of("validation", ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<PaymentErrorResponse> handleDateTimeParse(DateTimeParseException ex) {
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of("validation", "Invalid date format: " + ex.getParsedString(), getCorrelationId()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PaymentErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid " + ex.getName() + ": must be a valid UUID";
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of("validation", message, getCorrelationId()));
    }

    @ExceptionHandler(MerchantNotPaymentEligibleException.class)
    public ResponseEntity<PaymentErrorResponse> handleMerchantNotEligible(MerchantNotPaymentEligibleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PaymentErrorResponse.of("merchant_not_payment_eligible", ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<PaymentErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PaymentErrorResponse.of("idempotency_conflict", ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(PaymentOrderNotFoundException.class)
    public ResponseEntity<PaymentErrorResponse> handleNotFound(PaymentOrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PaymentErrorResponse.of("not_found", ex.getMessage(), getCorrelationId()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<PaymentErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(PaymentErrorResponse.of("forbidden", "Access denied", getCorrelationId()));
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
}
