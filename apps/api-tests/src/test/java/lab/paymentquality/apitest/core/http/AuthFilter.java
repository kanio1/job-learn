package lab.paymentquality.apitest.core.http;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;

/**
 * REST Assured filter that injects {@code Authorization: Bearer <token>} for non-anonymous identities.
 *
 * <p>Reads the current {@link TestContext} from {@link Ctx}. If no context is set or the identity
 * is anonymous, no Authorization header is added — the request is sent without auth.
 *
 * <p>Rules enforced by this filter:
 * <ul>
 *   <li>Anonymous identity → no Authorization header (public endpoints: {@code /api/status}, seed/reset).</li>
 *   <li>Non-anonymous identity → {@code Authorization: Bearer <token>}. Token is NOT logged.</li>
 *   <li>Null context (filter called outside test context) → no Authorization header, filter passes through.</li>
 * </ul>
 *
 * <p>SDET learning: using a filter for auth injection keeps scenarios free of auth concerns.
 * The filter is added once to the {@code BASE} request spec in {@link RestAssuredSetup#install}.
 * The {@code ANONYMOUS} spec is built without this filter — so public-endpoint tests never
 * accidentally carry an Authorization header.
 *
 * <p>Category: Auth/correlation infrastructure filter.
 */
public final class AuthFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        TestContext testCtx = Ctx.currentOrNull();
        if (testCtx != null && !testCtx.identity().isAnonymous()) {
            String token = testCtx.identity().token();
            requestSpec.header("Authorization", "Bearer " + token);
            // Token value is intentionally not logged here.
        }
        return ctx.next(requestSpec, responseSpec);
    }
}
