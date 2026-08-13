package lab.paymentquality.checkoutlab.internal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

class CheckoutLabGetWithBodyRejectFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.GET.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (hasBody(request)) {
            String correlationId = request.getHeader("X-Correlation-ID");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("X-Correlation-ID", correlationId);
            String body = """
                    {"type":"https://api.payment-quality.local/problems/get-with-body","title":"Forbidden","status":403,"detail":"GET must not include a body","error":"get_with_body","correlationId":"%s"}
                    """.formatted(correlationId);
            response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean hasBody(HttpServletRequest request) {
        int contentLength = request.getContentLength();
        if (contentLength > 0) {
            return true;
        }
        String header = request.getHeader("Content-Length");
        if (header != null && !header.isBlank()) {
            try {
                return Long.parseLong(header.trim()) > 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        String transferEncoding = request.getHeader("Transfer-Encoding");
        return transferEncoding != null && !transferEncoding.isBlank();
    }
}
