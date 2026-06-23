package lab.paymentquality.apitest;

import lab.paymentquality.apitest.core.data.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test data helper test — Phase 5.
 *
 * <p>Verifies that all test data generators produce:
 * <ul>
 *   <li>Non-blank, HTTP-safe strings.</li>
 *   <li>Unique values on each call (collision resistance for parallel runs).</li>
 *   <li>Readable prefix-based output for debuggable failure messages.</li>
 * </ul>
 *
 * <p>Also verifies {@link ETag} parsing and {@link Versioned} wrapper construction.
 *
 * <p>No network. No containers.
 *
 * <p>SDET concept: test data helpers are first-class framework citizens — if they produce
 * colliding IDs under parallel execution, tests become flaky. Unit tests here catch
 * format regressions before any live test is affected.
 */
class DataHelpersTest {

    // ── CorrelationIds ────────────────────────────────────────────────────────

    @Test
    void correlationIds_generate_withPrefix_hasExpectedFormat() {
        String id = CorrelationIds.generate("create-payment");
        assertThat(id).startsWith("test-create-payment-");
        assertThat(id).matches("[a-z0-9-]+");
    }

    @Test
    void correlationIds_generate_withoutPrefix_startsWithTestReq() {
        String id = CorrelationIds.generate();
        assertThat(id).startsWith("test-req-");
    }

    @Test
    void correlationIds_isValid_detectsNonBlank() {
        assertThat(CorrelationIds.isValid("test-abc")).isTrue();
        assertThat(CorrelationIds.isValid(null)).isFalse();
        assertThat(CorrelationIds.isValid("  ")).isFalse();
    }

    @Test
    void correlationIds_generate_producesUniqueIds() {
        String id1 = CorrelationIds.generate("same");
        String id2 = CorrelationIds.generate("same");
        assertThat(id1).isNotEqualTo(id2);
    }

    // ── IdempotencyKeys ───────────────────────────────────────────────────────

    @Test
    void idempotencyKeys_generate_withScenario_hasExpectedFormat() {
        String key = IdempotencyKeys.generate("authorize");
        assertThat(key).startsWith("idem-authorize-");
        assertThat(key).matches("[a-z0-9-]+");
    }

    @Test
    void idempotencyKeys_generate_withoutScenario_startsWithIdemOp() {
        String key = IdempotencyKeys.generate();
        assertThat(key).startsWith("idem-op-");
    }

    @Test
    void idempotencyKeys_generate_producesUniqueKeys() {
        String key1 = IdempotencyKeys.generate("create");
        String key2 = IdempotencyKeys.generate("create");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void idempotencyKeys_generate_sanitizesSpecialChars() {
        String key = IdempotencyKeys.generate("Create Order!");
        assertThat(key).matches("[a-z0-9-]+");
    }

    // ── UniqueReferences ──────────────────────────────────────────────────────

    @Test
    void uniqueReferences_merchantRef_startsWithMerch() {
        String ref = UniqueReferences.merchantRef();
        assertThat(ref).startsWith("merch-");
        assertThat(ref).matches("[a-z0-9-]+");
    }

    @Test
    void uniqueReferences_merchantRef_withLabel_includesLabel() {
        String ref = UniqueReferences.merchantRef("alpha");
        assertThat(ref).startsWith("merch-alpha-");
    }

    @Test
    void uniqueReferences_paymentRef_startsWithPay() {
        String ref = UniqueReferences.paymentRef("checkout");
        assertThat(ref).startsWith("pay-checkout-");
        assertThat(ref).matches("[a-z0-9-]+");
    }

    @Test
    void uniqueReferences_of_usesGivenPrefix() {
        String ref = UniqueReferences.of("tenant");
        assertThat(ref).startsWith("tenant-");
    }

    @Test
    void uniqueReferences_uuid_isValidUuidFormat() {
        String uuid = UniqueReferences.uuid();
        assertThat(uuid).matches("[0-9a-f-]{36}");
    }

    @Test
    void uniqueReferences_merchantRef_producesUniqueValues() {
        String r1 = UniqueReferences.merchantRef();
        String r2 = UniqueReferences.merchantRef();
        assertThat(r1).isNotEqualTo(r2);
    }

    // ── ETag ─────────────────────────────────────────────────────────────────

    @Test
    void etag_of_rawValue_roundtrips() {
        ETag etag = ETag.of("\"v3\"");
        assertThat(etag.raw()).isEqualTo("\"v3\"");
    }

    @Test
    void etag_isQuoted_trueForValidVnFormat() {
        assertThat(ETag.of("\"v1\"").isQuoted()).isTrue();
        assertThat(ETag.of("\"v100\"").isQuoted()).isTrue();
    }

    @Test
    void etag_isQuoted_falseForUnquotedValue() {
        assertThat(ETag.of("v1").isQuoted()).isFalse();
        assertThat(ETag.of("abc").isQuoted()).isFalse();
    }

    @Test
    void etag_version_extractsIntegerFromQuotedVnEtag() {
        assertThat(ETag.of("\"v1\"").version()).isEqualTo(1);
        assertThat(ETag.of("\"v42\"").version()).isEqualTo(42);
    }

    @Test
    void etag_version_throwsIfNotInVnFormat() {
        ETag unquoted = ETag.of("v1");
        assertThatThrownBy(unquoted::version)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("\"vN\"");
    }

    @Test
    void etag_of_rejectsNullAndBlank() {
        assertThatThrownBy(() -> ETag.of(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ETag.of("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void etag_equality_basedOnRawValue() {
        ETag a = ETag.of("\"v2\"");
        ETag b = ETag.of("\"v2\"");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // ── Versioned ────────────────────────────────────────────────────────────

    @Test
    void versioned_of_body_and_rawEtag_wrapsCorrectly() {
        Versioned<String> v = Versioned.of("my-body", "\"v1\"");

        assertThat(v.body()).isEqualTo("my-body");
        assertThat(v.etag().raw()).isEqualTo("\"v1\"");
        assertThat(v.etag().version()).isEqualTo(1);
    }

    @Test
    void versioned_of_body_and_etag_object_wrapsCorrectly() {
        ETag etag = ETag.of("\"v5\"");
        Versioned<Integer> v = Versioned.of(42, etag);

        assertThat(v.body()).isEqualTo(42);
        assertThat(v.etag()).isSameAs(etag);
    }

    @Test
    void versioned_rejectsNullBody() {
        assertThatThrownBy(() -> Versioned.of(null, "\"v1\""))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void versioned_rejectsNullEtag() {
        assertThatThrownBy(() -> new Versioned<>("body", null))
                .isInstanceOf(NullPointerException.class);
    }
}
