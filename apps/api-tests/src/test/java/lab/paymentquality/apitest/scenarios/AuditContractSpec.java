package lab.paymentquality.apitest.scenarios;

import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.audit.AuditApi;
import lab.paymentquality.apitest.api.audit.dto.AuditListResponse;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.CreatePaymentOrderRequest;
import lab.paymentquality.apitest.api.payment.dto.PaymentOrderResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.auth.Identity;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.IdempotencyKeys;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.data.UniqueReferences;
import lab.paymentquality.apitest.core.http.Headers;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Audit API contract spec — Phase 8A.
 *
 * <p><strong>Test category:</strong> Async event contract — verifies that payment lifecycle
 * operations emit durable audit events that are visible through the
 * {@code GET /api/audit} endpoint after the async write completes.
 *
 * <p><strong>Async write model:</strong> {@code PaymentLifecycleService} publishes
 * {@code AuditableActionOccurred} Spring Application Events using
 * {@code ApplicationEventPublisher.publishEvent()}. These are consumed by
 * {@code AuditEventListener}, which is annotated with
 * {@code @ApplicationModuleListener} — a Spring Modulith meta-annotation that combines
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} and {@code @Async}.
 * The event listener runs in a separate thread and transaction AFTER the lifecycle
 * operation's transaction commits. The HTTP response is therefore returned BEFORE
 * the audit event is persisted.
 *
 * <p><strong>Why Awaitility and not Thread.sleep:</strong>
 * <ul>
 *   <li>{@code Thread.sleep} introduces a fixed latency that may be too short (flaky) or
 *       too long (slow). Neither is acceptable in a production test suite.</li>
 *   <li>Awaitility polls at a short interval and succeeds as soon as the condition holds,
 *       making tests both fast and stable.</li>
 *   <li>The {@code atMost(10, SECONDS)} ceiling is generous enough for a test JVM under
 *       load; typical completion is under 1 second.</li>
 * </ul>
 *
 * <p><strong>Correlation ID propagation:</strong> {@code CorrelationIdFilter} sets
 * {@code X-Correlation-ID} on every request in the MDC. {@code AuditableActionEventFactory}
 * reads this from MDC and passes it into the event. The audit event row stores it in the
 * {@code correlation_id} column, and the API exposes it in {@code correlationId} field.
 * Asserting that the audit event's {@code correlationId} matches the lifecycle response's
 * {@code X-Correlation-ID} verifies end-to-end observability tracing.
 *
 * <p><strong>Audit event action strings:</strong> {@code PaymentLifecycleService} publishes:
 * <ul>
 *   <li>{@code "PAYMENT_AUTHORIZED"} on authorize</li>
 *   <li>{@code "PAYMENT_CAPTURED"} on capture</li>
 *   <li>{@code "PAYMENT_CANCELLED"} on cancel</li>
 *   <li>{@code "PAYMENT_REFUNDED"} on refund</li>
 *   <li>Payment <em>create</em> does NOT emit an audit event.</li>
 * </ul>
 *
 * <p><strong>Isolation:</strong> audit events are NOT cleared by {@code SeedApi.reset()}.
 * Tests isolate their events by filtering on {@code targetId == paymentOrderId.toString()}
 * after querying by {@code action + target_type}.
 *
 * <p><strong>Authorization:</strong> The live spec uses {@code tenantAdmin("TENANT_ALPHA")}
 * which carries {@code tenant:audit:read} (via {@code TENANT_ADMIN} composite). The
 * {@code TenantResolverService} resolves {@code TENANT_ALPHA} as {@code isTenantScoped=true},
 * so only events with {@code tenantId="TENANT_ALPHA"} are visible. Events from
 * {@code merchant.alpha.creator} have {@code tenantId="TENANT_ALPHA"}, matching the filter.
 * The {@code platform:audit:read} path is exercised by separate security smoke tests in the
 * backend's {@code AuditSecurityMatrixIT}.
 *
 * <p><strong>Awaitility thread model:</strong> Awaitility 4.x evaluates conditions in a
 * dedicated background thread. {@link lab.paymentquality.apitest.core.context.Ctx} is a
 * {@code ThreadLocal} — values set on the test (main) thread are NOT visible on the
 * Awaitility polling thread. Each condition lambda must call {@code Ctx.set(...)} to
 * establish the correct identity on the polling thread before making API calls that
 * require authentication via {@link lab.paymentquality.apitest.core.http.AuthFilter}.
 *
 * <p><strong>SDET interview topics:</strong>
 * <ul>
 *   <li>What is the difference between {@code @TransactionalEventListener} and
 *       {@code @EventListener}? (Hint: the former fires only after the publishing transaction
 *       commits — guarantees event not fired on rollback.)</li>
 *   <li>Why does {@code @ApplicationModuleListener} combine {@code @Async} with
 *       {@code @TransactionalEventListener(AFTER_COMMIT)}?</li>
 *   <li>Why does the audit event carry the same {@code correlationId} as the HTTP response?
 *       What would break in incident response if it did not?</li>
 *   <li>Why is testing asynchronous behavior harder than testing synchronous behavior?
 *       What can go wrong if you replace Awaitility with a fixed {@code Thread.sleep}?</li>
 *   <li>Why does payment create not emit an audit event, but authorize does?
 *       (Hint: consider the compliance risk of auditing every payment creation vs. only
 *       authorized financial commitments.)</li>
 * </ul>
 */
@ApiTest
@DisplayName("Audit API — async event contract")
class AuditContractSpec {

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

    /**
     * Authorize operation emits a {@code PAYMENT_AUTHORIZED} audit event visible via
     * {@code GET /api/audit} after the async write completes.
     *
     * <p><strong>Test category:</strong> Async audit event contract — verifies the full chain:
     * lifecycle operation → Spring Application Event → async listener → DB row →
     * audit API response.
     *
     * <p><strong>Awaitility contract:</strong>
     * <ul>
     *   <li>Polls {@code GET /api/audit?action=PAYMENT_AUTHORIZED&target_type=PAYMENT_ORDER}
     *       every 500 ms, up to 10 seconds.</li>
     *   <li>Condition: the response contains an entry with
     *       {@code targetId == paymentOrderId.toString()}.</li>
     *   <li>After condition holds, asserts stable fields: action, targetType, targetId,
     *       correlationId (matching the authorize response header), occurredAt, outcome.</li>
     * </ul>
     *
     * <p><strong>Correlation ID end-to-end assertion:</strong>
     * The {@code X-Correlation-ID} response header on the {@code authorize} call contains
     * the same ID that was injected into MDC by {@code CorrelationIdFilter} and subsequently
     * written into the audit event row by {@code AuditableActionEventFactory}.
     * Asserting the match verifies that the tracing ID survives the async hop across
     * transaction and thread boundaries.
     *
     * <p><strong>Compliance risk:</strong> if the audit event were missing or contained a
     * different {@code correlationId}, a payment lifecycle audit trail would be untraceable
     * back to the originating HTTP request. In a PCI-DSS or PSD2 context, this breaks
     * legally required audit log completeness.
     *
     * <p><strong>Reader identity:</strong> {@code tenantAdmin("TENANT_ALPHA")} carries
     * {@code tenant:audit:read}. The lifecycle operation (authorize) is performed by
     * {@code merchant.alpha.creator} whose JWT contains {@code tenant_id="TENANT_ALPHA"},
     * so all emitted audit events have {@code tenantId="TENANT_ALPHA"} — visible to the
     * tenant-scoped admin. {@code actorDisplay} is the Keycloak {@code preferred_username}
     * of the lifecycle actor (not the reading identity). The test asserts non-null only.
     */
    @Test
    @DisplayName("Authorize emits PAYMENT_AUTHORIZED audit event visible via /api/audit after async write [Phase 8A]")
    void authorize_emits_payment_authorized_audit_event() {
        // ── SETUP: create a new payment order ────────────────────────────────
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        var createResp = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(7_500L, "EUR", UniqueReferences.paymentRef("audit-auth")),
                IdempotencyKeys.generate("audit-auth"));
        createResp.then().statusCode(201);

        UUID paymentOrderId = createResp.as(PaymentOrderResponse.class).paymentOrderId();
        String etag = createResp.header(Headers.ETAG);

        // ── ACT: authorize the payment order ─────────────────────────────────
        // The authorize call returns after PaymentLifecycleService commits its transaction.
        // At commit time, Spring publishes AuditableActionOccurred via ApplicationEventPublisher.
        // The AuditEventListener (@ApplicationModuleListener = @Async + @TransactionalEventListener)
        // picks this up in a separate thread and persists it. The response arrives before this.
        var authResp = PaymentOrdersApi.authorize(
                Seeds.MERCHANT_ALPHA_001_ID,
                paymentOrderId.toString(),
                etag,
                IdempotencyKeys.generate("audit-auth-lifecycle"));
        authResp.then().statusCode(200);

        // Capture the correlation ID from the authorize response header.
        // CorrelationIdFilter injected this into MDC; AuditableActionEventFactory read it from MDC;
        // AuditEventListener persisted it in the audit_event row.
        String expectedCorrelationId = authResp.header(Headers.CORRELATION_ID);
        assertThat(expectedCorrelationId)
                .as("authorize response must carry X-Correlation-ID (injected by CorrelationIdFilter)")
                .isNotNull().isNotBlank();

        // ── POLL: switch to tenant admin and await the audit event ──────────
        // tenantAdmin("TENANT_ALPHA") carries tenant:audit:read. merchant.alpha.creator's JWT
        // carries tenant_id="TENANT_ALPHA", so emitted audit events have tenantId="TENANT_ALPHA"
        // and are visible to the tenant-scoped audit reader.
        //
        // THREADING NOTE: Awaitility 4.x evaluates conditions in a dedicated background thread
        // named "awaitility-thread". Ctx is a ThreadLocal — values set on the main test thread
        // are NOT visible on the polling thread. To ensure AuthFilter injects the Bearer token,
        // Ctx must be set inside the condition lambda (on "awaitility-thread") on every poll.
        // The outer Ctx.set() keeps the main-thread context correct for the post-await assertions.
        Identity auditIdentity = Identities.tenantAdmin("TENANT_ALPHA");
        Ctx.set(TestContext.of(auditIdentity));

        // Awaitility polls GET /api/audit?action=PAYMENT_AUTHORIZED&target_type=PAYMENT_ORDER
        // until the response contains an entry for this specific paymentOrderId.
        // atMost(15s) is generous; typical async delay is <500ms in test JVM.
        // pollDelay(200ms): give the @ApplicationModuleListener thread a head start before
        // the first poll — avoids a guaranteed false-negative on the very first call.
        String paymentOrderIdStr = paymentOrderId.toString();
        await()
                .atMost(Duration.ofSeconds(15))
                .pollDelay(Duration.ofMillis(200))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> {
                    // Set Ctx on the awaitility polling thread so AuthFilter injects the Bearer token.
                    // See threading note above — this must be done on every poll iteration.
                    Ctx.set(TestContext.of(auditIdentity));
                    var r = AuditApi.list("PAYMENT_AUTHORIZED", "PAYMENT_ORDER");
                    int status = r.statusCode();
                    if (status != 200) {
                        // Throw AssertionError (not Exception) so Awaitility propagates immediately
                        // without waiting for the full timeout — makes failures actionable.
                        throw new AssertionError(
                                "Audit endpoint returned " + status + " for tenantAdmin. Body: "
                                        + r.body().asString());
                    }
                    AuditListResponse resp = r.as(AuditListResponse.class);
                    return resp.content().stream()
                            .anyMatch(e -> paymentOrderIdStr.equals(e.targetId()));
                });

        // ── ASSERT: verify the audit event fields ────────────────────────────
        AuditListResponse finalList = AuditApi.list("PAYMENT_AUTHORIZED", "PAYMENT_ORDER")
                .then()
                .statusCode(200)
                .extract()
                .as(AuditListResponse.class);

        AuditListResponse.AuditEventSummary event = finalList.content().stream()
                .filter(e -> paymentOrderIdStr.equals(e.targetId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Audit event for paymentOrderId=" + paymentOrderIdStr + " not found"));

        assertThat(event.action())
                .as("action must be PAYMENT_AUTHORIZED (the lifecycle action string)")
                .isEqualTo("PAYMENT_AUTHORIZED");

        assertThat(event.targetType())
                .as("targetType must be PAYMENT_ORDER (set by publishSuccess)")
                .isEqualTo("PAYMENT_ORDER");

        assertThat(event.targetId())
                .as("targetId must equal paymentOrderId.toString()")
                .isEqualTo(paymentOrderIdStr);

        assertThat(event.correlationId())
                .as("correlationId must match the X-Correlation-ID from the authorize response — " +
                        "proves the tracing ID survives the async transaction boundary")
                .isEqualTo(expectedCorrelationId);

        assertThat(event.occurredAt())
                .as("occurredAt must be non-null (set to Instant.now() at event publication time)")
                .isNotNull().isNotBlank();

        assertThat(event.outcome())
                .as("outcome must be SUCCESS (lifecycle succeeded)")
                .isEqualTo("SUCCESS");

        assertThat(event.id())
                .as("audit event must have a generated UUID")
                .isNotNull();

        assertThat(event.actorDisplay())
                .as("actorDisplay must be non-null (sourced from JWT preferred_username)")
                .isNotNull().isNotBlank();
    }

    /**
     * {@code GET /api/audit} returns 403 for a user without any audit authority.
     *
     * <p><strong>Test category:</strong> Authorization boundary — verifies the
     * {@code @PreAuthorize("hasAnyAuthority('platform:audit:read', 'tenant:audit:read')")}
     * annotation on {@code AuditController.list()} is enforced.
     *
     * <p><strong>Security mechanism:</strong> the {@code denied()} user has a valid JWT
     * (passes {@code anyRequest().authenticated()} in the URL-security rules) but no
     * matching authority. Spring Method Security's AOP interceptor evaluates
     * {@code @PreAuthorize} and throws {@code AccessDeniedException}, which
     * {@code AuditExceptionHandler.handleForbidden()} catches and returns as 403.
     *
     * <p><strong>HTTP/REST concept:</strong> 401 vs 403 — the user IS identified
     * (authenticated JWT is present), but is NOT authorized. 401 Unauthorized would be
     * incorrect here; it means "not identified." 403 Forbidden means "identified but
     * not permitted."
     *
     * <p><strong>Compliance risk:</strong> if this check were accidentally removed
     * (e.g., by deleting the {@code @PreAuthorize} annotation), any authenticated user
     * could read the full audit trail of all payment operations. Audit logs often contain
     * actor identities and transaction amounts — exposing them to unauthorized users is a
     * privacy and compliance violation.
     *
     * <p><strong>Why no data setup:</strong> {@code @PreAuthorize} fires before any
     * controller body executes, so no payment order or tenant data is needed to trigger
     * the 403.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>At which point in the request lifecycle does {@code @PreAuthorize} fire?
     *       (Hint: AOP, before the method body runs but after the filter chain passes.)</li>
     *   <li>Why does the audit endpoint use {@code @PreAuthorize} rather than a URL rule
     *       in {@code SecurityConfig.authorizeHttpRequests()}?</li>
     * </ul>
     */
    @Test
    @DisplayName("GET /api/audit with denied user (no roles) → 403 Forbidden [Phase 8A]")
    void audit_list_returns_403_for_denied_user() {
        Ctx.set(TestContext.of(Identities.denied()));

        AuditApi.listAll()
                .then()
                .statusCode(403);
    }
}
