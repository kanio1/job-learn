package lab.paymentquality.shared.security;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Origin RFC 9457 bodies for filter-chain 401/403 (before MVC exception handlers).
 */
public final class ProblemSecurityResponses {

    private ProblemSecurityResponses() {
    }

    public static void unauthorized(HttpServletResponse response) throws IOException {
        write(response, 401, "unauthorized", "Unauthorized", "Authentication required");
    }

    public static void forbidden(HttpServletResponse response) throws IOException {
        write(response, 403, "forbidden", "Forbidden", "Access denied");
    }

    private static void write(HttpServletResponse response, int status, String error, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        response.setHeader("X-Correlation-ID", correlationId);
        String type = "https://api.payment-quality.local/problems/" + error.replace('_', '-');
        String json = """
                {"type":"%s","title":"%s","status":%d,"detail":"%s","correlationId":"%s","error":"%s"}
                """.formatted(
                jsonEscape(type), jsonEscape(title), status, jsonEscape(detail), jsonEscape(correlationId), jsonEscape(error));
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
