package lab.paymentquality.apitest.support;

import lab.paymentquality.apitest.core.http.RestAssuredSetup;
import lab.paymentquality.apitest.core.stack.ApiStack;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit extension that starts the test stack once per session and installs REST Assured.
 *
 * <p>Storing the stack in the <em>root</em> extension store ensures a single shared instance
 * across all {@link ApiTest}-annotated spec classes — containers start once, not per class.
 * JUnit calls {@link ExtensionContext.Store.CloseableResource#close()} when the root context
 * closes, which triggers {@link ApiStack#stop()}.
 *
 * <p>SDET learning: the root store is the correct scope for a singleton shared resource.
 * Using a class-level store would start the stack once per spec class — expensive and wrong.
 */
public class ApiStackExtension implements BeforeAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ApiStackExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot()
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent("stack", k -> {
                    ApiStack stack = ApiStack.get();
                    RestAssuredSetup.install(stack.baseUri());
                    return new StackResource(stack);
                }, StackResource.class);
    }

    private record StackResource(ApiStack stack) implements ExtensionContext.Store.CloseableResource {
        @Override
        public void close() {
            stack.stop();
        }
    }
}
