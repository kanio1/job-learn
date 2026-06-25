package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.CreatePaymentOrderRequest;
import lab.paymentquality.apitest.api.payment.dto.PaymentHistoryResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.IdempotencyKeys;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.data.UniqueReferences;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lifecycle idempotency replay contract — Phase 8D.
 *
 * <p><strong>Test category:</strong> Idempotency — verifies that repeating a lifecycle action
 * (authorize, capture) with the same {@code Idempotency-Key} and request body is safe: the
 * backend returns the same stable response, skips the PSP call, does not increment the ETag,
 * and does not write a duplicate status-history entry. Also verifies that a fingerprint mismatch
 * (same key, different body) produces a 409 idempotency conflict.
 *
 * <p><strong>Why API-level and not unit/MockMvc?</strong>
 * <ul>
 *   <li>The idempotency replay check reads from the PostgreSQL {@code idempotency_records} table.
 *       Only a live end-to-end test can verify that the idempotency record is written on the
 *       first call and read correctly on the second call.</li>
 *   <li>The "no duplicate history entry" assertion requires the {@code payment_order_status_history}
 *       table to be checked — only visible through the real history endpoint.</li>
 *   <li>The ETag stability assertion requires the {@code payment_orders.version} JPA
 *       {@code @Version} field to NOT increment on replay — observable only through a real
 *       HTTP round-trip.</li>
 * </ul>
 *
 * <p><strong>Guard order in lifecycle service (discovered from {@code PaymentLifecycleService}):</strong>
 * <pre>
 * Controller:
 *   PaymentEtag.requireVersion(ifMatch)           → 428 if null/blank (BEFORE service call)
 *   IdempotencyKey.of(idempotencyKeyHeader)       → 400 if blank
 *   paymentLifecycleService.authorize(...)
 *
 * Service:
 *   1. isIdempotentLifecycleReplay()              → early return (no state change, no history)
 *   2. PaymentVersionPrecondition.requireCurrentVersion()  → 412 if ETag stale
 *   3. reserveIdempotency()                       → insert record; 409 if fingerprint mismatch
 *   4. pspClient.X()                              → PSP call
 *   5. order.X()                                  → domain state machine; 422 if wrong state
 * </pre>
 *
 * <p><strong>Critical asymmetry:</strong> on replay, {@code isIdempotentLifecycleReplay()}
 * returns {@code true} BEFORE step 2 (the version check). This means the client can safely resend
 * the ORIGINAL {@code If-Match: "v0"} header from the first request, even though the order is
 * now at version 1 after the first transition. The version check is bypassed because the replay
 * was detected first.
 *
 * <p><strong>Idempotency fingerprint scope for lifecycle actions:</strong>
 * <pre>
 * Fingerprint JSON (SHA-256 hashed):
 * {
 *   "operation": "POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize",
 *   "merchantId": "&lt;uuid&gt;",
 *   "paymentOrderId": "&lt;uuid&gt;",
 *   "action": "AUTHORIZE"
 *   // "amountMinor": &lt;long&gt;     — included only when non-null (capture/refund)
 *   // "reason": "&lt;string&gt;"    — included only when non-null
 * }
 * </pre>
 *
 * <p><strong>DB lookup key scope:</strong> {@code (merchantId, paymentOrderId, action, idempotencyKeyHash)}.
 * This means:
 * <ul>
 *   <li>Same key + same action + different paymentOrderId → NOT a conflict (different record).</li>
 *   <li>Same key + different action + same paymentOrderId → NOT a conflict (different record).</li>
 *   <li>Same key + same action + same paymentOrderId + different body → 409 conflict.</li>
 * </ul>
 *
 * <p><strong>Test data strategy:</strong> each test creates its own fresh payment order via the
 * API using a unique {@code clientOrderReference} and idempotency key. No shared seeded state.
 * {@code @AfterAll SeedApi.reset()} cleans up all created orders and idempotency records.
 * {@code seededMerchantCreator()} identity (Keycloak {@code merchant.alpha.creator}) has
 * {@code merchant:payments:create}, {@code merchant:payments:read}, and
 * {@code merchant:payments:lifecycle} authorities, plus a {@code merchant_id} JWT claim equal to
 * {@link Seeds#MERCHANT_ALPHA_001_ID}.
 *
 * <p><strong>SDET interview topics:</strong>
 * <ul>
 *   <li>Why must lifecycle replay return the same ETag as the original response? What client
 *       problem occurs if the ETag changes on replay?</li>
 *   <li>Why is no history entry written on replay? What would a duplicate entry imply for an
 *       auditor reviewing the payment lifecycle?</li>
 *   <li>Why is the If-Match check syntactically required but semantically bypassed on replay?
 *       (Hint: the controller calls {@code requireVersion(ifMatch)} before the service call;
 *       the service calls {@code isIdempotentLifecycleReplay()} before {@code requireCurrentVersion}.)
 *   </li>
 *   <li>What is the fingerprint scope for lifecycle actions?
 *       Which fields are included, which are excluded, and why?</li>
 *   <li>Why does the lifecycle fingerprint include {@code paymentOrderId} (unlike the create
 *       fingerprint which does not)?
 *       (Hint: lifecycle actions are inherently tied to one specific order.)</li>
 *   <li>What is the risk of NOT having idempotency on lifecycle actions in a distributed system?
 *       (Hint: retried authorize after a network timeout could double-reserve funds.)</li>
 *   <li>Why does the idempotency conflict fire BEFORE the ETag version check?
 *       (Hint: the fingerprint conflict is a semantic invariant; the version check only guards
 *       the optimistic-lock write — meaningless if we're not going to write anyway.)</li>
 * </ul>
 */
@ApiTest
@DisplayName("Lifecycle idempotency replay — contract")
class LifecycleIdempotencyContractSpec {

    @BeforeAll
    static void seedDatabase() {
        SeedApi.seed();
    }

    @AfterAll
    static void cleanUp() {
        SeedApi.reset();
    }

    @AfterEach
    void clearContext() {
        Ctx.clear();
    }

    // -------------------------------------------------------------------------
    // Authorize replay: same key + same body → stable 200, no duplicate history
    // -------------------------------------------------------------------------

    /**
     * Authorize replay with the same Idempotency-Key returns 200 and does not write
     * a duplicate history entry.
     *
     * <p><strong>Test category:</strong> Idempotency — lifecycle authorize replay.
     *
     * <p><strong>HTTP/REST concept:</strong> lifecycle idempotency is distinct from create
     * idempotency. Both share the same store ({@code idempotency_records} table) but the key
     * scope differs:
     * <ul>
     *   <li>Create: scoped by {@code (merchantId, idempotencyKeyHash)}.</li>
     *   <li>Lifecycle: scoped by {@code (merchantId, paymentOrderId, action, idempotencyKeyHash)}.</li>
     * </ul>
     * The narrower scope means a key used for "authorize" can be reused for a different action
     * (e.g. "capture") on the same order without triggering a conflict — the backend treats them
     * as independent idempotency slots.
     *
     * <p><strong>Replay ETag stability:</strong> after the first authorize, the order is at
     * version 1. On replay, the service calls {@code isIdempotentLifecycleReplay()}, detects the
     * matching fingerprint, and returns the current order (version 1) WITHOUT calling
     * {@code order.authorize()} again. The controller then calls
     * {@code PaymentEtag.from(order)} → {@code "v1"}, the same ETag as the original response.
     * ETag stability is essential for the client to build its retry loop: it would be a bug if
     * replay returned a different ETag than the first response.
     *
     * <p><strong>No-duplicate history contract:</strong> {@code recordHistory()} is called
     * AFTER {@code reserveIdempotency()} and AFTER the PSP call. On replay, execution returns
     * before {@code reserveIdempotency()} is reached. Therefore, no new history row is written.
     * This is critical for compliance: a payment audit log must not show an action executed twice
     * when it was executed once (the retry was a communication artifact, not a real re-authorization).
     *
     * <p><strong>If-Match reuse on replay:</strong> the test intentionally resends the original
     * {@code If-Match: "v0"} on the replay (even though the order is now at v1). The controller
     * parses it (syntax OK → 0 expected), but the service's replay check fires before the
     * version check, so the stale-ETag guard is never reached. This is intentional backend design:
     * the client's retry infrastructure may have cached the original request including its headers.
     *
     * <p><strong>Business risk if this property is broken:</strong> if replay incremented the ETag,
     * the client's next lifecycle call (e.g. capture) would need the new ETag. If the client
     * used the ETag from the first response (before the retry), it would get 412. A network retry
     * would then cause a 412 on the subsequent action — even though the original request succeeded.
     *
     * <p><strong>Payment security note:</strong> the PSP {@code authorize()} call is NOT repeated
     * on replay. Repeating it could cause a double-authorization charge at the PSP — the client's
     * bank would see two fund reservations.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why is the ETag on the replay response "v1" and not "v0"? (The ETag reflects the
     *       current order state, which moved to v1 during the first authorize.)</li>
     *   <li>Why is the history size checked AFTER both the original and replay call? (To verify
     *       the backend wrote exactly one history row, not zero and not two.)</li>
     *   <li>If you were testing this property with a unit test (MockMvc), what would you be
     *       unable to verify? (Whether the idempotency record was actually persisted and read
     *       from the real DB, and whether the ETag value matches the persisted version.)</li>
     * </ul>
     */
    @Test
    @DisplayName("authorize replay returns stable 200 and does not create duplicate history entry [Phase 8D]")
    void authorize_replay_returns_stable_200_and_does_not_create_duplicate_history_entry() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));
        String merchantId = Seeds.MERCHANT_ALPHA_001_ID;

        // 1. Create a fresh payment order — establishes paymentOrderId and v0 ETag.
        String createKey = IdempotencyKeys.generate("8d-auth-create");
        CreatePaymentOrderRequest createBody = CreatePaymentOrderRequest.valid(
                1100L, "PLN", UniqueReferences.paymentRef("8d-auth-replay"));
        Response createResp = PaymentOrdersApi.create(merchantId, createBody, createKey);
        createResp.then().statusCode(201);
        String paymentOrderId = createResp.jsonPath().getString("paymentOrderId");
        String eTagV0 = createResp.getHeader("ETag"); // "v0"

        // 2. First authorize — writes idempotency record, increments ETag to v1, writes history.
        String authorizeKey = IdempotencyKeys.generate("8d-auth-replay");
        Response firstAuth = PaymentOrdersApi.authorize(merchantId, paymentOrderId, eTagV0, authorizeKey);
        firstAuth.then().statusCode(200);
        String eTagAfterAuthorize = firstAuth.getHeader("ETag"); // "v1"
        assertThat(eTagAfterAuthorize).isEqualTo("\"v1\"");

        // 3. Verify history has exactly 1 entry (AUTHORIZE) before the replay.
        PaymentHistoryResponse historyBeforeReplay = PaymentOrdersApi.history(merchantId, paymentOrderId)
                .then().statusCode(200)
                .extract().as(PaymentHistoryResponse.class);
        assertThat(historyBeforeReplay.content())
                .as("history before replay must have exactly 1 AUTHORIZE entry")
                .hasSize(1);
        assertThat(historyBeforeReplay.content().get(0).action())
                .as("first history entry must be AUTHORIZE")
                .isEqualTo("AUTHORIZE");

        // 4. Replay authorize — same Idempotency-Key, same If-Match ("v0", stale but syntactically valid).
        //    isIdempotentLifecycleReplay() detects the matching fingerprint (reason=null in both calls)
        //    and returns the current order BEFORE the version check fires.
        //    PSP is NOT called again; order version does NOT increment; history is NOT written.
        Response replayAuth = PaymentOrdersApi.authorize(merchantId, paymentOrderId, eTagV0, authorizeKey);
        replayAuth.then()
                .statusCode(200)
                .header("ETag", eTagAfterAuthorize); // must be "v1", same as original
        assertThat(replayAuth.jsonPath().getString("status"))
                .as("replay response body must show AUTHORIZED (same state as original response)")
                .isEqualTo("AUTHORIZED");

        // 5. Verify history still has exactly 1 entry — replay must not add a duplicate AUTHORIZE row.
        PaymentHistoryResponse historyAfterReplay = PaymentOrdersApi.history(merchantId, paymentOrderId)
                .then().statusCode(200)
                .extract().as(PaymentHistoryResponse.class);
        assertThat(historyAfterReplay.content())
                .as("history after replay must still have exactly 1 AUTHORIZE entry — no duplicate")
                .hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Capture replay: same key + same body → stable 200, no duplicate history
    // -------------------------------------------------------------------------

    /**
     * Capture replay with the same Idempotency-Key returns 200 and does not write
     * a duplicate history entry.
     *
     * <p><strong>Test category:</strong> Idempotency — lifecycle capture replay.
     *
     * <p><strong>HTTP/REST concept:</strong> this test verifies the same no-duplicate-history
     * and ETag-stability properties as the authorize replay test, but at the CAPTURE step of the
     * lifecycle chain. The setup is deliberately more complex (create → authorize → capture)
     * because capture is only valid from the AUTHORIZED state — verifying idempotency at a deeper
     * lifecycle step has higher business value (authorization-to-capture is the step where money
     * actually moves from customer to merchant).
     *
     * <p><strong>Two separate idempotency slots:</strong> the authorize step uses its own key
     * ({@code authorizeKey}); the capture step uses a different key ({@code captureKey}). They
     * occupy independent idempotency records, scoped to their respective actions. Reusing the same
     * key across actions is legal (they use different DB rows) but unrelated to this test.
     *
     * <p><strong>Capture ETag chain:</strong>
     * <pre>
     * Create → v0
     * Authorize(authorizeKey, If-Match="v0") → v1
     * Capture(captureKey, If-Match="v1") → v2
     * Replay capture(captureKey, If-Match="v1") → v2 (same — no increment)
     * </pre>
     *
     * <p><strong>Business risk if capture replay is not idempotent:</strong> in a payment flow,
     * the merchant's settlement service retries the capture after a network timeout. If the backend
     * captured a second time, the customer's card would be charged twice. The idempotency record
     * prevents this: the PSP {@code capture()} call is NOT repeated on replay.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why is verifying "no duplicate history entry" after the replay important for payment
     *       compliance? (A duplicated CAPTURE entry would suggest two captures occurred.)</li>
     *   <li>What would happen if the backend used wall-clock time (not the idempotency key) to
     *       detect duplicate captures? (Race condition: two requests within the same millisecond
     *       would not be detected as duplicates.)</li>
     *   <li>If the first capture succeeded but the network dropped before the client received
     *       the response, the client retries. Without idempotency, what PSP-level problem arises?
     *       (Answer: double capture — funds moved twice.)</li>
     * </ul>
     */
    @Test
    @DisplayName("capture replay returns stable 200 and does not create duplicate history entry [Phase 8D]")
    void capture_replay_returns_stable_200_and_does_not_create_duplicate_history_entry() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));
        String merchantId = Seeds.MERCHANT_ALPHA_001_ID;

        // 1. Create a fresh payment order.
        String createKey = IdempotencyKeys.generate("8d-cap-create");
        CreatePaymentOrderRequest createBody = CreatePaymentOrderRequest.valid(
                2200L, "EUR", UniqueReferences.paymentRef("8d-cap-replay"));
        Response createResp = PaymentOrdersApi.create(merchantId, createBody, createKey);
        createResp.then().statusCode(201);
        String paymentOrderId = createResp.jsonPath().getString("paymentOrderId");
        String eTagV0 = createResp.getHeader("ETag"); // "v0"

        // 2. Authorize to reach AUTHORIZED state (prerequisite for capture).
        String authorizeKey = IdempotencyKeys.generate("8d-auth-for-cap");
        Response authResp = PaymentOrdersApi.authorize(merchantId, paymentOrderId, eTagV0, authorizeKey);
        authResp.then().statusCode(200);
        String eTagV1 = authResp.getHeader("ETag"); // "v1"
        assertThat(eTagV1).isEqualTo("\"v1\"");

        // 3. First capture — writes idempotency record, increments ETag to v2, writes history.
        String captureKey = IdempotencyKeys.generate("8d-cap-replay");
        Response firstCapture = PaymentOrdersApi.capture(merchantId, paymentOrderId, eTagV1, captureKey);
        firstCapture.then().statusCode(200);
        String eTagAfterCapture = firstCapture.getHeader("ETag"); // "v2"
        assertThat(eTagAfterCapture).isEqualTo("\"v2\"");

        // 4. Verify history has exactly 2 entries (AUTHORIZE + CAPTURE) before replay.
        PaymentHistoryResponse historyBeforeReplay = PaymentOrdersApi.history(merchantId, paymentOrderId)
                .then().statusCode(200)
                .extract().as(PaymentHistoryResponse.class);
        assertThat(historyBeforeReplay.content())
                .as("history before capture replay must have exactly 2 entries: AUTHORIZE + CAPTURE")
                .hasSize(2);
        assertThat(historyBeforeReplay.content().get(0).action()).isEqualTo("AUTHORIZE");
        assertThat(historyBeforeReplay.content().get(1).action()).isEqualTo("CAPTURE");

        // 5. Replay capture — same captureKey, same If-Match ("v1", stale after first capture but
        //    syntactically valid). Replay check fires before version check → no 412.
        //    PSP capture is NOT repeated; ETag stays at v2; no new history row.
        Response replayCapture = PaymentOrdersApi.capture(merchantId, paymentOrderId, eTagV1, captureKey);
        replayCapture.then()
                .statusCode(200)
                .header("ETag", eTagAfterCapture); // must be "v2", same as original
        assertThat(replayCapture.jsonPath().getString("status"))
                .as("replay response body must show CAPTURED (same state as original response)")
                .isEqualTo("CAPTURED");

        // 6. Verify history still has exactly 2 entries — replay must not add a second CAPTURE row.
        PaymentHistoryResponse historyAfterReplay = PaymentOrdersApi.history(merchantId, paymentOrderId)
                .then().statusCode(200)
                .extract().as(PaymentHistoryResponse.class);
        assertThat(historyAfterReplay.content())
                .as("history after capture replay must still have exactly 2 entries — no duplicate CAPTURE")
                .hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Idempotency conflict: same key + different body → 409
    // -------------------------------------------------------------------------

    /**
     * Lifecycle idempotency conflict: same Idempotency-Key, same action, different body (reason field)
     * returns 409 {@code idempotency_conflict}.
     *
     * <p><strong>Test category:</strong> Idempotency conflict — negative path for lifecycle replay.
     *
     * <p><strong>HTTP/REST concept:</strong> the backend uses a SHA-256 fingerprint of the
     * canonical request JSON to detect re-use of the same key with different data. For lifecycle
     * actions, the fingerprint includes the {@code reason} field when it is non-null:
     *
     * <pre>
     * First call:  authorize(key=K, body={})                  → fingerprint A (reason=null, not included)
     * Second call: authorize(key=K, body={reason:"conflict"}) → fingerprint B (reason="conflict", included)
     * Fingerprint A ≠ fingerprint B → IdempotencyConflictException → 409
     * </pre>
     *
     * <p><strong>Guard order on conflict:</strong> the conflict is detected inside
     * {@code isIdempotentLifecycleReplay()}, which fires BEFORE the version check in the service.
     * The controller still calls {@code PaymentEtag.requireVersion(ifMatch)} before the service,
     * so a syntactically valid {@code If-Match} is required — the test uses the current
     * {@code "v1"} ETag (the order is in AUTHORIZED state after the first call).
     *
     * <p><strong>Why 409 and not 400?</strong> 400 signals a malformed request (bad JSON, wrong
     * types). A conflict body is structurally valid — it passes JSON parsing and bean validation.
     * The problem is semantic: this key was already committed to a different transaction. 409 is
     * the correct HTTP status for a server-state conflict.
     *
     * <p><strong>Distinguish from create idempotency conflict:</strong> for the payment order
     * create endpoint, the fingerprint includes {@code amountMinor}, {@code currency}, and
     * {@code clientOrderReference}. For lifecycle actions, the fingerprint includes
     * {@code amountMinor} (capture/refund only, when non-null) and {@code reason} (when non-null).
     * The patterns are analogous but the fields differ because the operations have different semantics.
     *
     * <p><strong>Business risk:</strong> a client bug could reuse an old idempotency key from a
     * different request context — for example, retrying after a crash where the key-generation
     * code used a stale counter. The 409 conflict prevents the old key from authorizing a new
     * transaction with different parameters. Without fingerprint matching, the backend would
     * silently return the old (first call's) authorization result, which may have the wrong reason,
     * wrong amount, or be for a completely different order.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>What would happen if the backend only compared the idempotency key without the
     *       fingerprint? (Answer: same key + different amount → 200 with the wrong amount returned.
     *       The client would think the new request succeeded, but the wrong amount would have
     *       been authorized.)</li>
     *   <li>Why does the fingerprint NOT include all request body fields for lifecycle actions?
     *       (E.g., what if the client sends an extra unknown field?) (Answer: the fingerprint
     *       uses only the domain-significant fields defined by {@code RequestFingerprint.forLifecycle()}.
     *       Extra fields in the JSON body are ignored or rejected at the controller layer.)</li>
     *   <li>If the conflict fires before the version check, what is the HTTP status when
     *       the conflict body is sent with a STALE If-Match header?
     *       (Answer: still 409 — the version check is never reached.)</li>
     * </ul>
     */
    @Test
    @DisplayName("lifecycle idempotency conflict with different fingerprint returns 409 [Phase 8D]")
    void lifecycle_idempotency_conflict_with_different_fingerprint_returns_409() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));
        String merchantId = Seeds.MERCHANT_ALPHA_001_ID;

        // 1. Create a fresh payment order.
        String createKey = IdempotencyKeys.generate("8d-conflict-create");
        CreatePaymentOrderRequest createBody = CreatePaymentOrderRequest.valid(
                1100L, "PLN", UniqueReferences.paymentRef("8d-auth-conflict"));
        Response createResp = PaymentOrdersApi.create(merchantId, createBody, createKey);
        createResp.then().statusCode(201);
        String paymentOrderId = createResp.jsonPath().getString("paymentOrderId");
        String eTagV0 = createResp.getHeader("ETag"); // "v0"

        // 2. First authorize with body {} (reason=null).
        //    Idempotency record: key=K, action=AUTHORIZE, fingerprint_A (no reason field).
        String authorizeKey = IdempotencyKeys.generate("8d-conflict-key");
        Response firstAuth = PaymentOrdersApi.authorize(merchantId, paymentOrderId, eTagV0, authorizeKey);
        firstAuth.then().statusCode(200);
        String eTagV1 = firstAuth.getHeader("ETag"); // "v1" — current version

        // 3. Same Idempotency-Key + same action + body {reason:"conflict-reason"}.
        //    Fingerprint_B (reason="conflict-reason") ≠ Fingerprint_A (no reason) → conflict.
        //    The conflict fires in isIdempotentLifecycleReplay() BEFORE the version check,
        //    but the controller's PaymentEtag.requireVersion() fires before the service call,
        //    so eTagV1 (syntactically valid) is passed to avoid a 428.
        ProblemAssert.assertThat(
                        PaymentOrdersApi.authorizeWithReason(
                                merchantId, paymentOrderId, eTagV1, authorizeKey, "conflict-reason"))
                .hasStatus(409)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.IDEMPOTENCY_CONFLICT)
                .hasNoStore();
    }
}
