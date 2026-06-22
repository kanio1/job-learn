/**
 * Cross-cutting technical module (security filter chain, CORS, correlation-id filter, request-rejection
 * filters, and the authority catalog). Marked OPEN so other modules (e.g. {@code merchant}) may
 * reference types such as {@code shared.security.Authorities} without tripping architecture tests.
 *
 * <p>Decision recorded in design OQ1: the shared module is inherently cross-cutting and benefits from
 * colocation of the authority catalog with its primary consumers ({@code SecurityConfig} and
 * {@code KeycloakRealmRoleConverter}). Marking it OPEN is Spring Modulith's documented idiom for
 * cross-cutting / technical modules.
 *
 * @see <a href="https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.open">
 *     Spring Modulith – Open Application Modules</a>
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package lab.paymentquality.shared;
