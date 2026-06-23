package lab.paymentquality.apitest.core.problem;

import io.restassured.response.Response;
import lab.paymentquality.apitest.core.http.Headers;
import lab.paymentquality.apitest.core.http.ContentTypes;
import org.assertj.core.api.AbstractAssert;

import java.util.List;

/**
 * Custom AssertJ assertion for {@code application/problem+json} error responses.
 *
 * <p>Wraps a REST Assured {@link Response} and provides fluent, readable assertions
 * over HTTP status, content-type, problem body fields, and header contract.
 *
 * <p>Example usage:
 * <pre>{@code
 *   ProblemAssert.assertThat(response)
 *       .hasStatus(409)
 *       .hasError(ProblemCodes.IDEMPOTENCY_CONFLICT)
 *       .hasCorrelationId()
 *       .hasNoStore()
 *       .varyContains("Idempotency-Key");
 * }</pre>
 *
 * <p>SDET learning: extending {@link AbstractAssert} integrates with the full AssertJ ecosystem —
 * you get {@code as()}, {@code describedAs()}, {@code satisfies()}, and {@code SoftAssertions}
 * support for free. Failure messages include the full response body for fast diagnosis.
 *
 * <p>The {@code matchesProblemSchema()} method is deferred to Phase 4+ when the
 * {@code json-schema-validator} dependency is added to {@code pom.xml}.
 *
 * <p>Category: Problem/error contract support.
 */
public final class ProblemAssert extends AbstractAssert<ProblemAssert, Response> {

    private ProblemAssert(Response actual) {
        super(actual, ProblemAssert.class);
    }

    /** Entry point — call {@code ProblemAssert.assertThat(response)} in test code. */
    public static ProblemAssert assertThat(Response response) {
        return new ProblemAssert(response);
    }

    /** Asserts the HTTP status code matches. */
    public ProblemAssert hasStatus(int expected) {
        isNotNull();
        int statusCode = actual.statusCode();
        if (statusCode != expected) {
            failWithMessage(
                    "Expected HTTP status <%d> but was <%d>.%nResponse body:%n%s",
                    expected, statusCode, actual.body().asPrettyString());
        }
        return this;
    }

    /** Asserts the Content-Type is {@code application/problem+json}. */
    public ProblemAssert hasContentTypeProblemJson() {
        isNotNull();
        String contentType = actual.contentType();
        if (contentType == null || !contentType.contains(ContentTypes.PROBLEM_JSON)) {
            failWithMessage(
                    "Expected Content-Type to contain <%s> but was <%s>",
                    ContentTypes.PROBLEM_JSON, contentType);
        }
        return this;
    }

    /**
     * Asserts the {@code error} field matches the expected code.
     * Use {@link ProblemCodes} constants for stable values.
     */
    public ProblemAssert hasError(String expectedCode) {
        isNotNull();
        String errorCode = actual.jsonPath().getString("error");
        if (!expectedCode.equals(errorCode)) {
            failWithMessage(
                    "Expected error code <%s> but was <%s>.%nResponse body:%n%s",
                    expectedCode, errorCode, actual.body().asPrettyString());
        }
        return this;
    }

    /** Asserts the {@code correlationId} body field is present and non-blank. */
    public ProblemAssert hasCorrelationId() {
        isNotNull();
        String correlationId = actual.jsonPath().getString("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            failWithMessage(
                    "Expected non-blank correlationId in response body but was <%s>.%nBody:%n%s",
                    correlationId, actual.body().asPrettyString());
        }
        return this;
    }

    /** Asserts the {@code correlationId} body field equals the expected value. */
    public ProblemAssert hasCorrelationId(String expectedId) {
        isNotNull();
        String correlationId = actual.jsonPath().getString("correlationId");
        if (!expectedId.equals(correlationId)) {
            failWithMessage(
                    "Expected correlationId <%s> but was <%s>",
                    expectedId, correlationId);
        }
        return this;
    }

    /**
     * Asserts the {@code correlationId} in the body matches the {@code X-Correlation-ID} header.
     * Verifies the backend echoes the same ID in both the body and the response header.
     */
    public ProblemAssert hasCorrelationIdConsistent() {
        isNotNull();
        String inBody = actual.jsonPath().getString("correlationId");
        String inHeader = actual.header(Headers.CORRELATION_ID);
        if (inBody == null || !inBody.equals(inHeader)) {
            failWithMessage(
                    "correlationId inconsistent: body=<%s>, header X-Correlation-ID=<%s>",
                    inBody, inHeader);
        }
        return this;
    }

    /** Asserts the response path (URL) contains the given substring. */
    public ProblemAssert hasPathContaining(String expected) {
        isNotNull();
        String detail = actual.jsonPath().getString("detail");
        if (detail == null || !detail.contains(expected)) {
            failWithMessage(
                    "Expected 'detail' to contain <%s> but was <%s>",
                    expected, detail);
        }
        return this;
    }

    /** Asserts the {@code detail} or {@code message} field contains the expected substring. */
    public ProblemAssert hasMessageContaining(String expected) {
        isNotNull();
        String detail = actual.jsonPath().getString("detail");
        String message = actual.jsonPath().getString("message");
        boolean detailMatches = detail != null && detail.contains(expected);
        boolean messageMatches = message != null && message.contains(expected);
        if (!detailMatches && !messageMatches) {
            failWithMessage(
                    "Expected 'detail' or 'message' to contain <%s>.%n  detail=<%s>%n  message=<%s>",
                    expected, detail, message);
        }
        return this;
    }

    /** Asserts {@code Cache-Control: no-store} is present in the response. */
    public ProblemAssert hasNoStore() {
        isNotNull();
        String cacheControl = actual.header(Headers.CACHE_CONTROL);
        if (cacheControl == null || !cacheControl.contains("no-store")) {
            failWithMessage(
                    "Expected Cache-Control containing 'no-store' but was <%s>", cacheControl);
        }
        return this;
    }

    /** Asserts the {@code Vary} header contains the given header name (case-insensitive). */
    public ProblemAssert varyContains(String headerName) {
        isNotNull();
        String vary = actual.header(Headers.VARY);
        if (vary == null || !vary.toLowerCase().contains(headerName.toLowerCase())) {
            failWithMessage(
                    "Expected Vary to contain <%s> but was <%s>", headerName, vary);
        }
        return this;
    }

    /**
     * Asserts the {@code details} array contains an entry for the given field name.
     * Used for 400 validation errors with field-level error details.
     */
    public ProblemAssert hasFieldError(String field) {
        isNotNull();
        List<String> fields = actual.jsonPath().getList("details.field");
        if (fields == null || !fields.contains(field)) {
            failWithMessage(
                    "Expected field error for <%s> in 'details' but found <%s>.%nBody:%n%s",
                    field, fields, actual.body().asPrettyString());
        }
        return this;
    }

    /**
     * Asserts the response body matches the problem JSON schema.
     *
     * <p><strong>Deferred to Phase 4+</strong>: requires the {@code json-schema-validator}
     * dependency in {@code pom.xml} ({@code io.rest-assured:json-schema-validator:6.0.0}).
     * The dependency is not yet in the local Maven cache — add it when network is available.
     *
     * @throws UnsupportedOperationException until json-schema-validator is added
     */
    public ProblemAssert matchesProblemSchema() {
        throw new UnsupportedOperationException(
                "matchesProblemSchema() requires json-schema-validator dependency (Phase 4+). " +
                "Add io.rest-assured:json-schema-validator:6.0.0 to pom.xml when network is available.");
    }
}
