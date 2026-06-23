package lab.paymentquality.apitest.core.stack;

/**
 * Documents the Phase 6B class structure for the Testcontainers stack.
 *
 * <p>This class contains no runnable code — it is a compile-time anchor for the
 * planned Phase 6B implementation. Remove this class when the real {@code ApiStack}
 * is implemented.
 *
 * <h2>Phase 6B implementation order</h2>
 * <ol>
 *   <li><strong>Uncomment TC deps</strong> in {@code pom.xml}: {@code testcontainers},
 *       {@code postgresql}, {@code junit-jupiter} (TC module), and {@code awaitility}.
 *       Also uncomment {@code testcontainers-keycloak} version property and dep.</li>
 *   <li><strong>{@code PostgresSupport}</strong> — wraps {@code PostgreSQLContainer("postgres:18")}.
 *       Exposes {@code jdbcUrl()}, {@code username()}, {@code password()}.</li>
 *   <li><strong>{@code KeycloakSupport}</strong> — wraps {@code dasniko/testcontainers-keycloak:3.7.0}.
 *       Mounts {@code payment-quality-realm.json} from classpath.
 *       Exposes {@code issuerUri()}, {@code jwksUri()}, {@code tokenEndpoint()}.</li>
 *   <li><strong>{@code BackendSupport}</strong> — wraps {@code GenericContainer(BACKEND_IMAGE)}.
 *       Receives postgres JDBC URL, Keycloak issuer URI, JWKS URI, and
 *       {@code APP_TESTING_ENABLED=true} as env vars.
 *       Waits for {@code GET /api/status} → 200 before signalling ready.</li>
 *   <li><strong>{@code ApiStack}</strong> — singleton coordinator. Reads {@link StackMode}.
 *       In {@code TESTCONTAINERS} mode: starts containers in order, wires URLs between them.
 *       In {@code EXTERNAL} mode: reads {@code API_BASE_URI}; no containers started.
 *       Exposes {@code baseUri()}, {@code keycloak()}, {@code stackMode()}.</li>
 *   <li><strong>{@code ApiStackExtension}</strong> — JUnit {@code BeforeAllCallback + AfterAllCallback}.
 *       Stores {@code ApiStack} in JUnit root store (singleton across classes).
 *       Calls {@link lab.paymentquality.apitest.core.http.RestAssuredSetup#install} with
 *       {@code ApiStack.get().baseUri()}.</li>
 *   <li><strong>{@code KeycloakTokenFactory}</strong> — implements
 *       {@link lab.paymentquality.apitest.core.auth.TokenFactory}.
 *       ROPC flow: {@code POST /realms/payment-quality/protocol/openid-connect/token}.
 *       Client: {@code payment-quality-dashboard} (public client, direct grants enabled).
 *       Caches tokens; never logs token values.</li>
 *   <li><strong>Update {@link lab.paymentquality.apitest.core.auth.Identities}</strong> —
 *       replace {@code TokenFactory.placeholder()} with real user credentials
 *       from the realm ({@code platform.admin / platform.admin}, etc.).</li>
 *   <li><strong>{@code StatusApi}</strong> — first thin client.
 *       Single method {@code status() → Response}. Uses {@code RequestSpecs.anonymous()}.</li>
 *   <li><strong>{@code ApiTest} annotation</strong> — {@code @ExtendWith(ApiStackExtension.class)}
 *       + {@code @Tag("live")}. Placed in {@code support/} package.</li>
 *   <li><strong>{@code StatusSpec}</strong> — first live {@code *Spec.java}.
 *       Asserts {@code GET /api/status} → 200 with correct body. Uses {@code ApiTest}.</li>
 * </ol>
 *
 * <h2>Backend OCI image prerequisite</h2>
 * <pre>{@code
 * cd apps/backend
 * ./mvnw spring-boot:build-image -DskipTests \
 *   -Dspring-boot.build-image.imageName=payment-quality/backend:local
 * }</pre>
 *
 * <h2>Environment variables</h2>
 * <ul>
 *   <li>{@code BACKEND_IMAGE} — required for {@code TESTCONTAINERS} mode.</li>
 *   <li>{@code API_BASE_URI} — set to switch to {@code EXTERNAL} mode.</li>
 * </ul>
 *
 * <h2>Critical wiring requirement</h2>
 * The backend JWT decoder validates {@code iss} in every token against
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}. In Testcontainers mode,
 * Keycloak gets a random host port. The backend container MUST receive the TC-mapped
 * Keycloak URL — not the default {@code http://localhost:8081/realms/payment-quality}.
 * Failure to do this causes 401 on every authenticated request.
 *
 * @see StackMode
 * @see <a href="../../../../../../../../docs/testing/rest-assured-framework/PHASE_6A_STACK_DISCOVERY.md">
 *     PHASE_6A_STACK_DISCOVERY.md</a>
 */
public final class ApiStackPlan {

    private ApiStackPlan() {}
}
