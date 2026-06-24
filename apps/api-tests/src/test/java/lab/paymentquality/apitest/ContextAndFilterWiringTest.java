package lab.paymentquality.apitest;

import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.auth.Identity;
import lab.paymentquality.apitest.core.auth.TokenFactory;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.CorrelationIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Auth/correlation infrastructure test — Phase 3.
 *
 * <p>Verifies:
 * <ul>
 *   <li>{@link Ctx} correctly stores and retrieves {@link TestContext} per thread.</li>
 *   <li>{@link TestContext} carries correlation ID and identity correctly.</li>
 *   <li>{@link Identities} factory produces expected persona shapes.</li>
 *   <li>{@link Identity} correctly distinguishes anonymous from non-anonymous.</li>
 *   <li>{@link TokenFactory#placeholder()} produces a non-blank non-secret string.</li>
 * </ul>
 *
 * <p>No REST Assured calls. No network. No containers.
 * Filters ({@code AuthFilter}, {@code CorrelationFilter}) are compiled and wired into
 * {@code RequestSpecs.BASE} — their logic is trusted via the filter contract and the
 * {@link CoreHttpWiringTest} spec-build assertion.
 *
 * <p>SDET concept: context infrastructure must be testable without the network. Isolating
 * identity/context logic allows fast feedback before any container is running.
 */
class ContextAndFilterWiringTest {

    @AfterEach
    void clearContext() {
        Ctx.clear();
    }

    // ── Ctx ─────────────────────────────────────────────────────────────────

    @Test
    void ctx_isNotSet_beforeAnyTestSetsIt() {
        assertThat(Ctx.isSet()).isFalse();
        assertThat(Ctx.currentOrNull()).isNull();
    }

    @Test
    void ctx_set_and_current_returnSameInstance() {
        TestContext ctx = TestContext.anonymous();
        Ctx.set(ctx);

        assertThat(Ctx.current()).isSameAs(ctx);
        assertThat(Ctx.isSet()).isTrue();
    }

    @Test
    void ctx_clear_removesContext() {
        Ctx.set(TestContext.anonymous());
        Ctx.clear();

        assertThat(Ctx.isSet()).isFalse();
        assertThat(Ctx.currentOrNull()).isNull();
    }

    @Test
    void ctx_current_throwsWhenNotSet() {
        assertThatThrownBy(Ctx::current)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No TestContext set");
    }

    @Test
    void ctx_set_rejectsNullContext() {
        assertThatThrownBy(() -> Ctx.set(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── TestContext ──────────────────────────────────────────────────────────

    @Test
    void testContext_of_identity_generatesCorrelationId() {
        Identity admin = Identities.platformAdmin();
        TestContext ctx = TestContext.of(admin);

        assertThat(ctx.correlationId()).isNotBlank().startsWith("test-");
        assertThat(ctx.identity()).isSameAs(admin);
        assertThat(ctx.scenarioName()).isNull();
    }

    @Test
    void testContext_of_correlationId_and_identity() {
        String corrId = "test-custom-001";
        TestContext ctx = TestContext.of(corrId, Identities.ANONYMOUS);

        assertThat(ctx.correlationId()).isEqualTo(corrId);
        assertThat(ctx.identity().isAnonymous()).isTrue();
    }

    @Test
    void testContext_anonymous_usesAnonymousIdentity() {
        TestContext ctx = TestContext.anonymous();

        assertThat(ctx.identity().isAnonymous()).isTrue();
        assertThat(ctx.correlationId()).isNotBlank();
    }

    // ── Identity ─────────────────────────────────────────────────────────────

    @Test
    void identity_anonymous_isAnonymousAndHasNoToken() {
        Identity anon = Identity.anonymous();

        assertThat(anon.isAnonymous()).isTrue();
        assertThat(anon.logicalName()).isEqualTo("anonymous");
        assertThatThrownBy(anon::token)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Anonymous identity has no token");
    }

    @Test
    void identity_named_withPlaceholderFactory_producesToken() {
        Identity actor = Identity.of("test-actor", TokenFactory.placeholder());

        assertThat(actor.isAnonymous()).isFalse();
        assertThat(actor.logicalName()).isEqualTo("test-actor");
        String token = actor.token();
        assertThat(token).isNotBlank().doesNotContain("secret").contains("test-actor");
    }

    @Test
    void identity_named_withNoOpFactory_throwsWhenTokenCalled() {
        Identity actor = Identity.of("no-op-actor", TokenFactory.noOp());

        assertThatThrownBy(actor::token)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("No TokenFactory configured");
    }

    // ── Identities (Object Mother) ───────────────────────────────────────────

    @Test
    void identities_anonymous_isStaticAndAnonymous() {
        assertThat(Identities.ANONYMOUS.isAnonymous()).isTrue();
        assertThat(Identities.ANONYMOUS.logicalName()).isEqualTo("anonymous");
    }

    @Test
    void identities_platformAdmin_isNotAnonymousAndHasPlaceholderToken() {
        Identity admin = Identities.platformAdmin();

        assertThat(admin.isAnonymous()).isFalse();
        assertThat(admin.logicalName()).contains("platform-admin");
        assertThat(admin.roles()).contains("PLATFORM_ADMIN");
        assertThat(admin.token()).isNotBlank();
    }

    @Test
    void identities_tenantAdmin_includesTenantIdInName() {
        Identity tenantAdmin = Identities.tenantAdmin("tenant-001");

        assertThat(tenantAdmin.logicalName()).contains("tenant-001");
        assertThat(tenantAdmin.tenantId()).isEqualTo("tenant-001");
    }

    @Test
    void identities_paymentCreator_hasExpectedRole() {
        Identity creator = Identities.paymentCreator("merchant-abc");

        assertThat(creator.roles()).contains("merchant:payments:create");
        assertThat(creator.isAnonymous()).isFalse();
    }

    // ── CorrelationIds ────────────────────────────────────────────────────────

    @Test
    void correlationIds_generate_producesReadableId() {
        String id = CorrelationIds.generate("create-payment");

        assertThat(id).startsWith("test-create-payment-");
        assertThat(id).matches("[a-z0-9-]+");
        assertThat(id.length()).isGreaterThan(10);
    }

    @Test
    void correlationIds_generate_producesUniqueIds() {
        String id1 = CorrelationIds.generate("test");
        String id2 = CorrelationIds.generate("test");

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void correlationIds_generate_sanitizesSpecialCharsInPrefix() {
        String id = CorrelationIds.generate("My Test! Scenario");

        assertThat(id).matches("[a-z0-9-]+");
    }
}
