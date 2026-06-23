package lab.paymentquality.apitest;

import lab.paymentquality.apitest.core.http.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Framework wiring test — Phase 2 and Phase 3.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>{@link RestAssuredSetup#install} runs without error.</li>
 *   <li>All {@link RequestSpecs} factory methods build without throwing.</li>
 *   <li>{@link ResponseSpecs} factory methods build without throwing.</li>
 *   <li>{@link Headers} and {@link ContentTypes} constants have the expected values.</li>
 * </ul>
 *
 * <p>No network calls. No containers. Runs fully offline via {@code mvn -q test}.
 *
 * <p>SDET concept: wiring tests catch misconfiguration early — a build without a wiring test
 * may compile but throw {@code NullPointerException} on the first actual test run.
 * REST Assured concept: validates that {@code RequestSpecBuilder.build()} produces non-null
 * immutable templates and that {@code given().spec(BASE)} does not throw before any HTTP call.
 */
class CoreHttpWiringTest {

    @BeforeAll
    static void installRestAssured() {
        RestAssuredSetup.install("http://localhost:8080");
    }

    @Test
    void install_doesNotThrow() {
        assertThatNoException().isThrownBy(
                () -> RestAssuredSetup.install("http://localhost:9090"));
    }

    @Test
    void baseSpec_buildsWithoutThrowingBeforeHttpCall() {
        assertThatNoException().isThrownBy(RequestSpecs::base);
    }

    @Test
    void anonymousSpec_buildsWithoutThrowingBeforeHttpCall() {
        assertThatNoException().isThrownBy(RequestSpecs::anonymous);
    }

    @Test
    void idempotentSpec_buildsWithoutThrowingBeforeHttpCall() {
        assertThatNoException().isThrownBy(
                () -> RequestSpecs.idempotent("idem-wiring-test-001"));
    }

    @Test
    void conditionalSpec_buildsWithoutThrowingBeforeHttpCall() {
        assertThatNoException().isThrownBy(
                () -> RequestSpecs.conditional("\"v1\""));
    }

    @Test
    void lifecycleSpec_buildsWithoutThrowingBeforeHttpCall() {
        assertThatNoException().isThrownBy(
                () -> RequestSpecs.lifecycle("\"v1\"", "idem-lifecycle-001"));
    }

    @Test
    void mergePatchSpec_buildsWithoutThrowingBeforeHttpCall() {
        assertThatNoException().isThrownBy(
                () -> RequestSpecs.mergePatch("\"v1\""));
    }

    @Test
    void sensitiveResponseSpec_buildsWithoutThrowing() {
        assertThatNoException().isThrownBy(ResponseSpecs::sensitive);
    }

    @Test
    void problemJsonResponseSpec_buildsWithoutThrowing() {
        assertThatNoException().isThrownBy(ResponseSpecs::problemJson);
    }

    @Test
    void conditionalResponseSpec_buildsWithoutThrowing() {
        assertThatNoException().isThrownBy(ResponseSpecs::conditional);
    }

    @Test
    void createdResponseSpec_buildsWithoutThrowing() {
        assertThatNoException().isThrownBy(ResponseSpecs::created);
    }

    @Test
    void headerConstants_haveExpectedValues() {
        assertThat(Headers.IDEMPOTENCY_KEY).isEqualTo("Idempotency-Key");
        assertThat(Headers.IF_MATCH).isEqualTo("If-Match");
        assertThat(Headers.IF_NONE_MATCH).isEqualTo("If-None-Match");
        assertThat(Headers.ETAG).isEqualTo("ETag");
        assertThat(Headers.CORRELATION_ID).isEqualTo("X-Correlation-ID");
        assertThat(Headers.VARY).isEqualTo("Vary");
        assertThat(Headers.CACHE_CONTROL).isEqualTo("Cache-Control");
        assertThat(Headers.LOCATION).isEqualTo("Location");
        assertThat(Headers.RETRY_AFTER).isEqualTo("Retry-After");
    }

    @Test
    void contentTypeConstants_haveExpectedValues() {
        assertThat(ContentTypes.JSON).isEqualTo("application/json");
        assertThat(ContentTypes.PROBLEM_JSON).isEqualTo("application/problem+json");
        assertThat(ContentTypes.MERGE_PATCH_JSON).isEqualTo("application/merge-patch+json");
    }
}
