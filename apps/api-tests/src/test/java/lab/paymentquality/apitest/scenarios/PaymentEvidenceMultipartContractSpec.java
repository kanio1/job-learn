package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.PaymentEvidenceListResponse;
import lab.paymentquality.apitest.api.payment.dto.PaymentEvidenceResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.http.Headers;
import lab.paymentquality.apitest.core.http.ResponseSpecs;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave 4 — black-box payment-order evidence multipart contract ({@code REST-MULTIPART-01}).
 *
 * <p>Closes the gate with construction, binary type, 201+Location, GET-list read-back,
 * negatives, malformed boundary, and cross-merchant denial. Does not import backend types
 * and does not assert database rows.
 */
@ApiTest
@Tag("contract")
@DisplayName("Payment evidence multipart — contract")
class PaymentEvidenceMultipartContractSpec {

    private static final long MAX_EVIDENCE_BYTES = 2L * 1024L * 1024L;

    /** 1×1 PNG (valid signature + IHDR/IDAT/IEND). */
    private static final byte[] MINIMAL_PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89,
            0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
            0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
            0x0D, 0x0A, 0x2D, (byte) 0xB4,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @BeforeAll
    static void seedDatabase() {
        SeedApi.seed();
    }

    @AfterAll
    static void resetDatabase() {
        SeedApi.reset();
    }

    @AfterEach
    void clearContext() {
        Ctx.clear();
    }

    @Test
    @DisplayName("AT-MP-01…04 POST PNG evidence → 201 Location + GET list read-back")
    void png_upload_returns_201_location_and_list_contains_evidence() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Response upload = PaymentOrdersApi.uploadEvidence(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID,
                "refund-proof.png",
                MINIMAL_PNG,
                "image/png");

        assertThat(upload.statusCode()).isEqualTo(201);
        upload.then().spec(ResponseSpecs.sensitive());

        String location = upload.header(Headers.LOCATION);
        PaymentEvidenceResponse created = upload.as(PaymentEvidenceResponse.class);
        UUID evidenceId = created.evidenceId();

        assertThat(location)
                .isEqualTo("/api/merchants/" + Seeds.MERCHANT_ALPHA_001_ID
                        + "/payment-orders/" + Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID
                        + "/evidence/" + evidenceId);
        assertThat(created.paymentOrderId().toString())
                .isEqualTo(Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID);
        assertThat(created.originalFilename()).isEqualTo("refund-proof.png");
        assertThat(created.contentType()).isEqualTo("image/png");
        assertThat(created.sizeBytes()).isEqualTo(MINIMAL_PNG.length);
        assertThat(created.uploadedAt()).isNotBlank();
        assertThat(upload.asString()).doesNotContain("storageKey");

        Response list = PaymentOrdersApi.listEvidence(
                Seeds.MERCHANT_ALPHA_001_ID, Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID);
        assertThat(list.statusCode()).isEqualTo(200);
        list.then().spec(ResponseSpecs.sensitive());
        PaymentEvidenceListResponse envelope = list.as(PaymentEvidenceListResponse.class);
        assertThat(envelope.content())
                .extracting(PaymentEvidenceResponse::evidenceId)
                .contains(evidenceId);
        assertThat(list.asString()).doesNotContain("storageKey");
    }

    @Test
    @DisplayName("AT-MP-05a missing part file → 400 validation")
    void missing_file_part_returns_400_validation() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.uploadEvidenceMissingFilePart(
                        Seeds.MERCHANT_ALPHA_001_ID, Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID))
                .hasStatus(400)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.VALIDATION);
    }

    @Test
    @DisplayName("AT-MP-05b empty file → 400 empty_evidence_file")
    void empty_file_returns_400() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.uploadEvidence(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID,
                        "empty.txt",
                        new byte[0],
                        "text/plain"))
                .hasStatus(400)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.EMPTY_EVIDENCE_FILE);
    }

    @Test
    @DisplayName("AT-MP-05c unsupported content type → 415")
    void unsupported_content_type_returns_415() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.uploadEvidence(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID,
                        "script.sh",
                        "rm".getBytes(StandardCharsets.UTF_8),
                        "application/x-sh"))
                .hasStatus(415)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.UNSUPPORTED_EVIDENCE_CONTENT_TYPE);
    }

    @Test
    @DisplayName("AT-MP-05d oversized file → 413 evidence_file_too_large")
    void oversized_file_returns_413() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));
        byte[] oversized = new byte[(int) MAX_EVIDENCE_BYTES + 1];

        ProblemAssert.assertThat(PaymentOrdersApi.uploadEvidence(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID,
                        "huge.txt",
                        oversized,
                        "text/plain"))
                .hasStatus(413)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.EVIDENCE_FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("AT-MP-05e blank filename → 400 missing_evidence_filename")
    void blank_filename_returns_400_missing_evidence_filename() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.uploadEvidence(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID,
                        "",
                        "note".getBytes(StandardCharsets.UTF_8),
                        "text/plain"))
                .hasStatus(400)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.MISSING_EVIDENCE_FILENAME);
    }

    @Test
    @DisplayName("AT-MP-05f exactly 2 MiB is accepted")
    void exactly_2mib_file_is_accepted() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));
        byte[] exactLimit = new byte[(int) MAX_EVIDENCE_BYTES];

        Response upload = PaymentOrdersApi.uploadEvidence(
                Seeds.MERCHANT_ALPHA_001_ID,
                Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID,
                "exact-limit.txt",
                exactLimit,
                "text/plain");

        assertThat(upload.statusCode()).isEqualTo(201);
        assertThat(upload.as(PaymentEvidenceResponse.class).sizeBytes()).isEqualTo(MAX_EVIDENCE_BYTES);
    }

    @Test
    @DisplayName("AT-MP-06 unsafe filename ../ → 400 invalid_evidence_filename")
    void unsafe_filename_returns_400() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.uploadEvidence(
                        Seeds.MERCHANT_ALPHA_001_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID,
                        "../secret.txt",
                        "note".getBytes(StandardCharsets.UTF_8),
                        "text/plain"))
                .hasStatus(400)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.INVALID_EVIDENCE_FILENAME);
    }

    @Test
    @DisplayName("AT-MP-07 truncated multipart → client error without new domain code")
    void malformed_boundary_returns_client_error_without_new_contract() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Response response = PaymentOrdersApi.uploadEvidenceMalformedBoundary(
                Seeds.MERCHANT_ALPHA_001_ID, Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID);

        assertThat(response.statusCode()).isEqualTo(400);
        ProblemAssert.assertThat(response)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.VALIDATION);
    }

    @Test
    @DisplayName("AT-MP-08 cross-merchant upload → 403 forbidden")
    void merchant_cannot_upload_evidence_for_another_merchant_order() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.uploadEvidence(
                        Seeds.MERCHANT_ALPHA_002_ID,
                        Seeds.PAYMENT_ORDER_ALPHA_002_CANCELLED_ID,
                        "note.txt",
                        "case".getBytes(StandardCharsets.UTF_8),
                        "text/plain"))
                .hasStatus(403)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.FORBIDDEN);
    }

    @Test
    @DisplayName("AT-MP-10 cross-merchant GET list → 404 not_found")
    void merchant_cannot_list_evidence_for_another_merchant_order() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.listEvidence(
                        Seeds.MERCHANT_ALPHA_002_ID, Seeds.PAYMENT_ORDER_ALPHA_002_CANCELLED_ID))
                .hasStatus(404)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.NOT_FOUND);
    }

    @Test
    @DisplayName("AT-MP-09 JSON body instead of multipart → 415 unsupported_media_type")
    void json_body_on_evidence_upload_returns_415() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(PaymentOrdersApi.uploadEvidenceAsJson(
                        Seeds.MERCHANT_ALPHA_001_ID, Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID))
                .hasStatus(415)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.UNSUPPORTED_MEDIA_TYPE);
    }
}
