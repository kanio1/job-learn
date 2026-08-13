package lab.paymentquality.mirrorlab.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import lab.paymentquality.mirrorlab.internal.application.MirrorLabProblemException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = MirrorLabController.class)
class MirrorLabExceptionHandler {

    @ExceptionHandler(MirrorLabProblemException.class)
    ResponseEntity<Map<String, Object>> handle(MirrorLabProblemException ex, HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", correlationId);
        return ResponseEntity.status(ex.status())
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Map.of(
                        "type", "https://api.payment-quality.local/problems/" + ex.error().replace('_', '-'),
                        "title", ex.status().getReasonPhrase(),
                        "status", ex.status().value(),
                        "detail", ex.getMessage(),
                        "error", ex.error(),
                        "correlationId", correlationId));
    }
}
