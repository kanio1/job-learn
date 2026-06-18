package lab.paymentquality.iam.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import lab.paymentquality.iam.internal.domain.exception.DuplicateUserException;
import lab.paymentquality.iam.internal.domain.exception.InvalidRoleException;
import lab.paymentquality.iam.internal.domain.exception.KeycloakAdminUnavailableException;
import lab.paymentquality.iam.internal.domain.exception.MissingTenantReferenceException;
import lab.paymentquality.iam.internal.domain.exception.TenantBoundaryViolationException;
import lab.paymentquality.iam.internal.domain.exception.UserNotFoundException;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.UUID;

@RestControllerAdvice(basePackages = "lab.paymentquality.iam.internal.web")
public class UserManagementExceptionHandler {

    private static final String PROBLEM_BASE = "https://api.payment-quality.local/problems/";
    private static final String X_CORRELATION_ID = "X-Correlation-ID";

    @ExceptionHandler(TenantBoundaryViolationException.class)
    public ResponseEntity<ProblemDetail> handleTenantBoundary(
            TenantBoundaryViolationException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "Access denied", request);
    }

    @ExceptionHandler({InvalidRoleException.class, MissingTenantReferenceException.class})
    public ResponseEntity<ProblemDetail> handleValidation(
            RuntimeException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "validation", "Bad Request", exception.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            UserNotFoundException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "not_found", "Not Found", "User not found", request);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(
            DuplicateUserException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "conflict", "Conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(KeycloakAdminUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleAdminUnavailable(
            KeycloakAdminUnavailableException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_GATEWAY,
                "bad_gateway",
                "Bad Gateway",
                "Identity provider administration is unavailable",
                request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String error,
            String title,
            String detail,
            HttpServletRequest request) {
        String correlationId = correlationId();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(URI.create(PROBLEM_BASE + error.replace('_', '-')));
        body.setTitle(title);
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("code", error.toUpperCase());
        body.setProperty("correlationId", correlationId);
        body.setProperty("error", error);
        body.setProperty("message", detail);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .cacheControl(CacheControl.noStore())
                .varyBy("Authorization")
                .header(X_CORRELATION_ID, correlationId)
                .body(body);
    }

    private String correlationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
    }
}
