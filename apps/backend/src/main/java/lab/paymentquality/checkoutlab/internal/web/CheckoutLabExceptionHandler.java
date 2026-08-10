package lab.paymentquality.checkoutlab.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionNotFoundException;
import lab.paymentquality.checkoutlab.internal.domain.InvalidCheckoutAmountException;
import lab.paymentquality.checkoutlab.internal.domain.InvalidCheckoutCurrencyException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = {
        CheckoutLabSessionController.class,
        CheckoutLabOAuthTokenController.class
})
class CheckoutLabExceptionHandler {

    private static final String ERROR_VALIDATION = "validation";
    private static final String ERROR_NOT_FOUND = "not_found";

    @ExceptionHandler(CheckoutSessionNotFoundException.class)
    ResponseEntity<CheckoutLabErrorResponse> handleNotFound(
            CheckoutSessionNotFoundException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ERROR_NOT_FOUND, ex.getMessage(), headersForRequest(request));
    }

    @ExceptionHandler({InvalidCheckoutAmountException.class, InvalidCheckoutCurrencyException.class})
    ResponseEntity<CheckoutLabErrorResponse> handleDomainValidation(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ERROR_VALIDATION, ex.getMessage(), headersForRequest(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<CheckoutLabErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<CheckoutLabErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new CheckoutLabErrorResponse.FieldError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .toList();
        return problem(
                HttpStatus.BAD_REQUEST,
                ERROR_VALIDATION,
                "Request validation failed",
                details,
                headersForRequest(request));
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<CheckoutLabErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        List<CheckoutLabErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new CheckoutLabErrorResponse.FieldError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .toList();
        return problem(
                HttpStatus.BAD_REQUEST,
                ERROR_VALIDATION,
                "Request validation failed",
                details,
                headersForRequest(request));
    }

    private ResponseEntity<CheckoutLabErrorResponse> problem(
            HttpStatus status,
            String error,
            String message,
            HttpHeaders headers) {
        return problem(status, error, message, null, headers);
    }

    private ResponseEntity<CheckoutLabErrorResponse> problem(
            HttpStatus status,
            String error,
            String message,
            List<CheckoutLabErrorResponse.FieldError> details,
            HttpHeaders headers) {
        String correlationId = resolveCorrelationId(headers);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .headers(headers)
                .body(CheckoutLabErrorResponse.of(
                        error,
                        message,
                        details,
                        correlationId,
                        status.value(),
                        status.getReasonPhrase()));
    }

    private HttpHeaders headersForRequest(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId != null && !correlationId.isBlank()) {
            headers.set("X-Correlation-ID", correlationId);
        }
        return headers;
    }

    private String resolveCorrelationId(HttpHeaders headers) {
        String correlationId = headers.getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = MDC.get("correlationId");
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
