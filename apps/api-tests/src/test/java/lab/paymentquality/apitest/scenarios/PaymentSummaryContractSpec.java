package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.PaymentSummaryResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.http.ResponseSpecs;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import lab.paymentquality.apitest.support.ApiTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Payment Order Summary / Reporting API contract spec — Phase 8B.
 *
 * <p><strong>Test category:</strong> Contract — verifies that the reporting aggregate endpoint
 * {@code GET /api/merchants/{merchantId}/payment-orders/summary} returns the correct body shape,
 * enforces merchant-scope authorization, and produces RFC-7807 problem+json for invalid query
 * parameters.
 *
 * <p><strong>Endpoint contract:</strong>
 * <ul>
 *   <li>Method: GET</li>
 *   <li>Path: {@code /api/merchants/{merchantId}/payment-orders/summary}</li>
 *   <li>Auth: {@code platform:payments:read} bypasses the JWT {@code merchant_id} claim check;
 *       merchant-scoped callers must have a JWT {@code merchant_id} claim equal to the path UUID.</li>
 *   <li>Optional query params: {@code currency} (PLN/EUR/USD), {@code status},
 *       {@code fromDate} (YYYY-MM-DD), {@code toDate} (YYYY-MM-DD).</li>
 *   <li>Response: 200 with JSON body containing {@code totalOrders}, {@code totalAmountMinor},
 *       {@code byCurrency} list (ordered ASC by currency string), and {@code byStatus} list
 *       (ordered ASC by status string).</li>
 * </ul>
 *
 * <p><strong>Why API-level and not unit/MockMvc?</strong>
 * <ul>
 *   <li>JSON field names ({@code byCurrency} vs. {@code currencies}) are invisible to unit tests
 *       but break clients silently in production.</li>
 *   <li>SQL {@code GROUP BY} aggregation correctness cannot be verified without the real database;
 *       mocking the repository loses the SQL semantics.</li>
 *   <li>The authorization scope-check (JWT {@code merchant_id} == path UUID) is a controller-level
 *       concern that requires the full HTTP stack to exercise.</li>
 *   <li>Response header contract ({@code no-store}, {@code Vary: Authorization}) is invisible to
 *       unit tests that bypass Spring's filter chain.</li>
 * </ul>
 *
 * <p><strong>Data isolation strategy:</strong> this class owns its own {@code @BeforeAll} seed
 * and {@code @AfterAll} reset. {@link SeedApi#seed()} is idempotent (clears then re-inserts)
 * so the dataset is always the deterministic fixture set regardless of what previous specs left.
 * No write operations are performed in this class — all tests are read-only — so the data is
 * stable across all test methods and exact aggregate numbers can be asserted safely.
 *
 * <p><strong>HTTP/REST concepts exercised:</strong>
 * <ul>
 *   <li>Aggregate / reporting GET — returns computed totals, not a resource representation.</li>
 *   <li>{@code Cache-Control: no-store} — aggregates over financial data must not be cached;
 *       stale totals could mislead risk/compliance systems.</li>
 *   <li>{@code Vary: Authorization} — per-merchant scoping means different JWT holders see
 *       different totals; caches must not share responses.</li>
 *   <li>400 {@code application/problem+json} — the backend validates the {@code currency}
 *       query param and throws {@code IllegalArgumentException} when the value is unsupported,
 *       mapped by {@code PaymentExceptionHandler.handleIllegalArgument} to 400 {@code validation}.</li>
 *   <li>403 {@code application/problem+json} — the controller throws {@code AccessDeniedException}
 *       when the JWT {@code merchant_id} claim does not match the path UUID (for non-platform callers),
 *       mapped by {@code PaymentExceptionHandler.handleAccessDenied} to 403 {@code forbidden}.</li>
 * </ul>
 *
 * <p><strong>Deterministic summary data for MERCHANT_ALPHA_001 (seed-only, no writes):</strong>
 * The {@link SeedApi#seed()} fixture loads exactly 101 payment orders for MERCHANT_ALPHA_001:
 * 3 named orders (SEED-ALPHA-001-CREATED/AUTHORIZED/CAPTURED) plus 98 pagination orders.
 * The fixture uses cyclic currency (PLN/EUR/USD per offset%3) and status (CREATED/AUTHORIZED/
 * CAPTURED/CANCELLED/REFUNDED per offset%5) patterns, producing the following deterministic totals:
 * <ul>
 *   <li>Total: 101 orders, 104 600 {@code totalAmountMinor}.</li>
 *   <li>By currency (ORDER BY currency ASC): EUR 34/35200, PLN 34/34100, USD 33/35300.</li>
 *   <li>By status (ORDER BY status ASC): AUTHORIZED 21/22200, CANCELLED 19/19000,
 *       CAPTURED 21/23300, CREATED 21/21100, REFUNDED 19/19000.</li>
 * </ul>
 *
 * <p><strong>SDET interview topics:</strong>
 * <ul>
 *   <li>Why is a reporting/aggregate endpoint harder to test than a CRUD endpoint?
 *       (Hint: results depend on the full dataset state, not a single resource.)</li>
 *   <li>What is the risk of asserting exact aggregate totals in a shared database?
 *       (Hint: concurrent test writes change the totals; isolation or read-only tests are needed.)</li>
 *   <li>Why does {@code Cache-Control: no-store} matter more on a reporting endpoint than on a
 *       single-resource GET? (Hint: stale aggregates could expose incorrect totals to risk systems.)</li>
 *   <li>Why does the backend use {@code COALESCE(SUM(...), 0)} in the SQL? What would happen
 *       to the response if {@code totalAmountMinor} were {@code null}?</li>
 *   <li>When asserting ordered list fields, how do you distinguish a contract ordering guarantee
 *       from an incidental ordering that may change?
 *       (Hint: the SQL has {@code ORDER BY po.currency ASC} — it IS a contract.)</li>
 * </ul>
 */
@ApiTest
@DisplayName("Payment Order Summary API — reporting contract")
class PaymentSummaryContractSpec {

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

    // -------------------------------------------------------------------------
    // Happy path — 200 with full aggregate body
    // -------------------------------------------------------------------------

    /**
     * Summary for seeded MERCHANT_ALPHA_001 returns 200 with deterministic aggregate totals.
     *
     * <p><strong>Test category:</strong> Contract — verifies the full body shape and exact
     * aggregate values for a merchant whose dataset is fixed by {@link SeedApi#seed()}.
     *
     * <p><strong>HTTP/REST concept:</strong> a reporting GET returns aggregate computations,
     * not a single-resource representation. The response has no ETag (not versionable) and
     * no {@code Location} header. Cache-Control is still {@code no-store} because financial
     * aggregates over payment orders are as sensitive as the individual records.
     *
     * <p><strong>Authorization note:</strong> {@link Identities#merchantReader} carries
     * {@code platform:payments:read}, which bypasses the JWT {@code merchant_id} claim check
     * in the controller. This persona is used here so the test remains clean even if the
     * seeded merchant creator's scope were restricted.
     *
     * <p><strong>Exact-number rationale:</strong> all 101 orders for MERCHANT_ALPHA_001 are
     * inserted by a deterministic seed (fixed amounts/currencies/statuses). This spec performs
     * no write operations, so the totals are invariant across test method execution order.
     *
     * <p><strong>Ordering contract:</strong>
     * <ul>
     *   <li>{@code byCurrency}: {@code ORDER BY po.currency ASC} → EUR, PLN, USD.</li>
     *   <li>{@code byStatus}: {@code ORDER BY po.status ASC} → AUTHORIZED, CANCELLED,
     *       CAPTURED, CREATED, REFUNDED.</li>
     * </ul>
     * These are SQL-level guarantees, not incidental ordering — safe to assert.
     *
     * <p><strong>Payment/business risk:</strong> if the summary endpoint returned a wrong total
     * (e.g., mixing merchant data or double-counting), a risk dashboard that reads these aggregates
     * would make incorrect decisions on exposure limits or settlement schedules.
     *
     * <p><strong>SDET topics:</strong>
     * <ul>
     *   <li>Why use SoftAssertions for a multi-field aggregate response?
     *       (Hint: all failures are reported in one run — avoids fix-one-field-per-run cycles.)</li>
     *   <li>Why is asserting {@code byCurrency.size() == 3} safer than asserting exact elements
     *       without also verifying size? (Hint: a duplicated row would be missed.)</li>
     *   <li>What does the backend do if {@code totalAmountMinor} is the sum of zero rows?
     *       (Hint: {@code COALESCE(SUM(...), 0)} ensures 0L, not null.)</li>
     * </ul>
     */
    @Test
    @DisplayName("GET /summary for seeded MERCHANT_ALPHA_001 → 200 with deterministic aggregate totals [Phase 8B]")
    void summary_for_seeded_merchant_returns_200_with_aggregate_shape() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        Response response = PaymentOrdersApi.summary(Seeds.MERCHANT_ALPHA_001_ID);
        response.then().statusCode(200);

        // Response headers: sensitive payment contract (Vary: Authorization + Cache-Control: no-store + X-Correlation-ID)
        response.then().spec(ResponseSpecs.sensitive());

        PaymentSummaryResponse body = response.as(PaymentSummaryResponse.class);

        SoftAssertions.assertSoftly(s -> {
            // ── Top-level totals ──────────────────────────────────────────────
            // 3 named orders (1100 PLN, 2200 EUR, 3300 USD) + 98 pagination orders × 1000 each
            s.assertThat(body.totalOrders())
                    .as("totalOrders: 3 named + 98 pagination = 101")
                    .isEqualTo(101L);
            s.assertThat(body.totalAmountMinor())
                    .as("totalAmountMinor: 1100+2200+3300 + 98×1000 = 104600")
                    .isEqualTo(104_600L);

            // ── byCurrency breakdown (ORDER BY currency ASC → EUR, PLN, USD) ──
            s.assertThat(body.byCurrency())
                    .as("byCurrency must have exactly 3 entries")
                    .isNotNull().hasSize(3);

            // EUR: 1 named (2200) + 33 pagination (33×1000=33000) = 34 orders, 35200 total
            PaymentSummaryResponse.CurrencySummary eur = body.byCurrency().get(0);
            s.assertThat(eur.currency()).as("byCurrency[0].currency").isEqualTo("EUR");
            s.assertThat(eur.orderCount()).as("byCurrency[0].orderCount").isEqualTo(34L);
            s.assertThat(eur.totalAmountMinor()).as("byCurrency[0].totalAmountMinor").isEqualTo(35_200L);

            // PLN: 1 named (1100) + 33 pagination (33×1000=33000) = 34 orders, 34100 total
            PaymentSummaryResponse.CurrencySummary pln = body.byCurrency().get(1);
            s.assertThat(pln.currency()).as("byCurrency[1].currency").isEqualTo("PLN");
            s.assertThat(pln.orderCount()).as("byCurrency[1].orderCount").isEqualTo(34L);
            s.assertThat(pln.totalAmountMinor()).as("byCurrency[1].totalAmountMinor").isEqualTo(34_100L);

            // USD: 1 named (3300) + 32 pagination (32×1000=32000) = 33 orders, 35300 total
            PaymentSummaryResponse.CurrencySummary usd = body.byCurrency().get(2);
            s.assertThat(usd.currency()).as("byCurrency[2].currency").isEqualTo("USD");
            s.assertThat(usd.orderCount()).as("byCurrency[2].orderCount").isEqualTo(33L);
            s.assertThat(usd.totalAmountMinor()).as("byCurrency[2].totalAmountMinor").isEqualTo(35_300L);

            // ── byStatus breakdown (ORDER BY status ASC → AUTHORIZED, CANCELLED, CAPTURED, CREATED, REFUNDED) ──
            s.assertThat(body.byStatus())
                    .as("byStatus must have exactly 5 entries")
                    .isNotNull().hasSize(5);

            // AUTHORIZED: 1 named (2200 EUR) + 20 pagination (20×1000=20000) = 21 orders, 22200 total
            PaymentSummaryResponse.StatusSummary authorized = body.byStatus().get(0);
            s.assertThat(authorized.status()).as("byStatus[0].status").isEqualTo("AUTHORIZED");
            s.assertThat(authorized.orderCount()).as("byStatus[0].orderCount").isEqualTo(21L);
            s.assertThat(authorized.totalAmountMinor()).as("byStatus[0].totalAmountMinor").isEqualTo(22_200L);

            // CANCELLED: 0 named + 19 pagination (19×1000=19000) = 19 orders, 19000 total
            PaymentSummaryResponse.StatusSummary cancelled = body.byStatus().get(1);
            s.assertThat(cancelled.status()).as("byStatus[1].status").isEqualTo("CANCELLED");
            s.assertThat(cancelled.orderCount()).as("byStatus[1].orderCount").isEqualTo(19L);
            s.assertThat(cancelled.totalAmountMinor()).as("byStatus[1].totalAmountMinor").isEqualTo(19_000L);

            // CAPTURED: 1 named (3300 USD) + 20 pagination (20×1000=20000) = 21 orders, 23300 total
            PaymentSummaryResponse.StatusSummary captured = body.byStatus().get(2);
            s.assertThat(captured.status()).as("byStatus[2].status").isEqualTo("CAPTURED");
            s.assertThat(captured.orderCount()).as("byStatus[2].orderCount").isEqualTo(21L);
            s.assertThat(captured.totalAmountMinor()).as("byStatus[2].totalAmountMinor").isEqualTo(23_300L);

            // CREATED: 1 named (1100 PLN) + 20 pagination (20×1000=20000) = 21 orders, 21100 total
            PaymentSummaryResponse.StatusSummary created = body.byStatus().get(3);
            s.assertThat(created.status()).as("byStatus[3].status").isEqualTo("CREATED");
            s.assertThat(created.orderCount()).as("byStatus[3].orderCount").isEqualTo(21L);
            s.assertThat(created.totalAmountMinor()).as("byStatus[3].totalAmountMinor").isEqualTo(21_100L);

            // REFUNDED: 0 named + 19 pagination (19×1000=19000) = 19 orders, 19000 total
            PaymentSummaryResponse.StatusSummary refunded = body.byStatus().get(4);
            s.assertThat(refunded.status()).as("byStatus[4].status").isEqualTo("REFUNDED");
            s.assertThat(refunded.orderCount()).as("byStatus[4].orderCount").isEqualTo(19L);
            s.assertThat(refunded.totalAmountMinor()).as("byStatus[4].totalAmountMinor").isEqualTo(19_000L);
        });
    }

    // -------------------------------------------------------------------------
    // Validation — 400 for unsupported currency
    // -------------------------------------------------------------------------

    /**
     * Summary with an unsupported currency value returns 400 with {@code error: "validation"}.
     *
     * <p><strong>Test category:</strong> Validation — verifies that the backend rejects
     * unsupported {@code currency} query parameter values before reaching the database.
     *
     * <p><strong>HTTP/REST concept:</strong> query parameter validation on a GET endpoint.
     * The backend's {@code PaymentOrderSummaryService.validateCurrency()} throws
     * {@code IllegalArgumentException("currency must be PLN, EUR, or USD")} for any value
     * outside the allowed set. {@code PaymentExceptionHandler.handleIllegalArgument} catches
     * this and returns 400 {@code application/problem+json} with {@code error: "validation"}.
     *
     * <p><strong>Business risk:</strong> an API that silently ignores an unrecognised filter
     * value returns unfiltered results — a client expecting "only GBP orders" would receive
     * all orders, inflate their risk view, or make incorrect settlement decisions.
     * Explicit 400 forces the client to correct its request.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why validate query parameters on a GET and not only on write operations?</li>
     *   <li>Why is 400 (not 404 or 422) the correct status for an unsupported currency?
     *       (Hint: the request is syntactically well-formed but semantically invalid.)</li>
     *   <li>What is the difference between {@code IllegalArgumentException} → 400
     *       and {@code AccessDeniedException} → 403 in the same controller?</li>
     * </ul>
     */
    @Test
    @DisplayName("GET /summary?currency=INVALID → 400 validation [Phase 8B]")
    void summary_with_invalid_currency_returns_400_validation() {
        Ctx.set(TestContext.of(Identities.merchantReader(Seeds.MERCHANT_ALPHA_001_ID)));

        ProblemAssert.assertThat(
                        PaymentOrdersApi.summaryWithCurrency(Seeds.MERCHANT_ALPHA_001_ID, "INVALID"))
                .hasStatus(400)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.VALIDATION)
                .hasNoStore()
                .varyContains("Authorization");
    }

    // -------------------------------------------------------------------------
    // Authorization boundary — 403 for merchant-scope mismatch
    // -------------------------------------------------------------------------

    /**
     * Summary request where JWT {@code merchant_id} claim does not match the path UUID → 403.
     *
     * <p><strong>Test category:</strong> Authorization boundary — verifies the controller-level
     * merchant-scope check for a non-platform caller.
     *
     * <p><strong>HTTP/REST concept:</strong> the summary endpoint enforces the same scope guard
     * as create/list: if the caller lacks {@code platform:payments:read}, the JWT
     * {@code merchant_id} claim must equal the {@code merchantId} path UUID.
     * Mismatch throws {@code AccessDeniedException}, which
     * {@code PaymentExceptionHandler.handleAccessDenied} maps to 403 {@code forbidden}.
     *
     * <p><strong>Scenario:</strong> {@link Identities#seededMerchantCreator()} has
     * {@code merchant_id = MERCHANT_ALPHA_001_ID} in its JWT. Calling summary for
     * {@code MERCHANT_ALPHA_002_ID} triggers the mismatch. The caller has valid lifecycle roles
     * but no {@code platform:payments:read} authority, so the platform bypass does not apply.
     *
     * <p><strong>Payment/business risk:</strong> a reporting endpoint that leaks cross-merchant
     * aggregates would expose a competitor's transaction volumes to a merchant-scoped caller —
     * a confidentiality and PCI-DSS scope violation. The scope guard must fire on summary
     * as well as on individual order reads.
     *
     * <p><strong>SDET interview topics:</strong>
     * <ul>
     *   <li>Why is the scope check on summary just as important as on payment order list?</li>
     *   <li>What is the difference between a missing-role 403 and a scope-mismatch 403?
     *       (Hint: one is rejected by {@code @PreAuthorize}; the other by a controller guard.)</li>
     *   <li>Why is 403 (not 404) returned here, while a GET on a specific non-owned payment
     *       order ID returns 404?
     *       (Hint: the summary URL has no payment order ID to mask; 404 is not meaningful.)</li>
     * </ul>
     */
    @Test
    @DisplayName("GET /summary with mismatched merchant_id JWT claim → 403 forbidden [Phase 8B]")
    void summary_with_mismatched_merchant_scope_returns_403() {
        // seededMerchantCreator has merchant_id = MERCHANT_ALPHA_001_ID.
        // Requesting summary for MERCHANT_ALPHA_002_ID triggers the scope-mismatch guard
        // because the caller lacks platform:payments:read.
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        ProblemAssert.assertThat(
                        PaymentOrdersApi.summary(Seeds.MERCHANT_ALPHA_002_ID))
                .hasStatus(403)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.FORBIDDEN)
                .hasNoStore()
                .varyContains("Authorization");
    }
}
