package lab.paymentquality.apitest.core.http;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;

/**
 * One-time REST Assured installation: builds immutable spec templates and registers parsers.
 *
 * <p>Call {@link #install(String)} once before any test that uses {@link RequestSpecs}.
 * Typically called from a JUnit {@code @BeforeAll} or a JUnit extension's {@code beforeAll}.
 *
 * <p>Critical decisions made here (see spec §7 / §10 for rationale):
 * <ul>
 *   <li><strong>problem+json parser</strong>: must be registered or error-body assertions throw
 *       a parse exception instead of a clean assertion failure.</li>
 *   <li><strong>EncoderConfig charset fix</strong>: prevents REST Assured from appending
 *       {@code charset=UTF-8} to {@code application/merge-patch+json}, which causes 415.</li>
 *   <li><strong>BigDecimal number type</strong>: financial amounts must not lose precision
 *       through {@code double} parsing — {@code BIG_DECIMAL} is mandatory.</li>
 *   <li><strong>Jackson 2</strong>: REST Assured 6.0.0 has a bug with Jackson 3 ({@code jsonPath()});
 *       we keep the default Jackson 2 on the test side.</li>
 *   <li><strong>Two templates</strong>: {@code BASE} gains {@link AuthFilter} and
 *       {@link CorrelationFilter} (Phase 3); {@code ANONYMOUS} is permanently filter-free
 *       so public-endpoint tests never carry an Authorization header.</li>
 * </ul>
 *
 * <p>SDET learning: {@code RequestSpecBuilder.build()} produces an <em>immutable template</em>.
 * {@code given().spec(BASE)} creates an <em>isolated mutable copy</em> — the template stays clean.
 * This is the correct answer to "how do you avoid header leakage between tests?"
 */
public final class RestAssuredSetup {

    private static volatile boolean installed = false;

    /**
     * Installs REST Assured for black-box API testing.
     * Builds the {@link RequestSpecs#BASE} and {@link RequestSpecs#ANONYMOUS_BASE} templates.
     * Safe to call multiple times (idempotent per base URI).
     */
    public static void install(String baseUri) {
        if (baseUri == null || baseUri.isBlank()) {
            throw new IllegalArgumentException("baseUri must not be blank");
        }

        RestAssuredConfig config = buildConfig();

        RestAssured.registerParser(ContentTypes.PROBLEM_JSON, Parser.JSON);

        RequestSpecification base = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AuthFilter())
                .addFilter(new CorrelationFilter())
                .addFilter(new ErrorLoggingFilter())
                .setConfig(config)
                .build();

        RequestSpecification anonymousBase = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new CorrelationFilter())
                .addFilter(new ErrorLoggingFilter())
                .setConfig(config)
                .build();

        RequestSpecs.BASE = base;
        RequestSpecs.ANONYMOUS_BASE = anonymousBase;
        installed = true;
    }

    /** Returns {@code true} if {@link #install(String)} has been called. */
    public static boolean isInstalled() {
        return installed;
    }

    private static RestAssuredConfig buildConfig() {
        return RestAssuredConfig.config()
                .encoderConfig(EncoderConfig.encoderConfig()
                        // Prevents "application/merge-patch+json; charset=UTF-8" → 415
                        .appendDefaultContentCharsetToContentTypeIfUndefined(false))
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL)
                        .enablePrettyPrinting(true))
                .jsonConfig(JsonConfig.jsonConfig()
                        // BigDecimal for all JSON numbers — financial amounts must not use float
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    private RestAssuredSetup() {}
}
