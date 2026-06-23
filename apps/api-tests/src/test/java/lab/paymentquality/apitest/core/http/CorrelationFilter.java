package lab.paymentquality.apitest.core.http;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.CorrelationIds;

/**
 * REST Assured filter that injects {@code X-Correlation-ID} on every request.
 *
 * <p>ID resolution order:
 * <ol>
 *   <li>Uses the correlation ID from the current {@link TestContext} (set via {@link Ctx}).</li>
 *   <li>If no context is set, generates a new ID via {@link CorrelationIds#generate()}.</li>
 *   <li>Never overwrites an ID already set on the request (e.g. by a scenario testing
 *       custom correlation IDs — explicitly set headers take precedence).</li>
 * </ol>
 *
 * <p>SDET learning: the backend echoes {@code X-Correlation-ID} in every response and logs it
 * via MDC. Binding the same ID to all requests within a test makes log grep trivial:
 * {@code grep "test-merchant-a3f7c2d1" backend.log} shows the entire test trace.
 *
 * <p>Category: Auth/correlation infrastructure filter.
 */
public final class CorrelationFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        if (requestSpec.getHeaders().hasHeaderWithName(Headers.CORRELATION_ID)) {
            return ctx.next(requestSpec, responseSpec);
        }

        TestContext testCtx = Ctx.currentOrNull();
        String correlationId = (testCtx != null)
                ? testCtx.correlationId()
                : CorrelationIds.generate();

        requestSpec.header(Headers.CORRELATION_ID, correlationId);
        return ctx.next(requestSpec, responseSpec);
    }
}
