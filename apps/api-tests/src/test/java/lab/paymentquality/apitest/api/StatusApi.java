package lab.paymentquality.apitest.api;

import io.restassured.response.Response;
import lab.paymentquality.apitest.core.http.RequestSpecs;

/**
 * Thin client for {@code GET /api/status}.
 *
 * <p>Uses {@link RequestSpecs#anonymous()} — the status endpoint is public and must not
 * carry an {@code Authorization} header. Returns the raw {@link Response} so specs can
 * assert both status code and body without committing to a fixed return contract here.
 *
 * <p>SDET learning: a thin client is a one-line facade over REST Assured. It hides path
 * strings from scenarios (single source of truth for the URL) and is the only layer
 * allowed to call {@code given/when/get}. Scenarios express business intent only.
 */
public final class StatusApi {

    private StatusApi() {}

    /** Calls {@code GET /api/status} anonymously and returns the raw response. */
    public static Response status() {
        return RequestSpecs.anonymous()
                .when()
                .get("/api/status");
    }
}
