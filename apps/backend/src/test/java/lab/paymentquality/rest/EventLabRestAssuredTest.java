package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import lab.paymentquality.testsupport.KafkaContainerSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "kafka"})
@Import(TestJwtConfiguration.class)
@Testcontainers
public class EventLabRestAssuredTest extends PostgresContainerSupport {
    @Container static PostgreSQLContainer postgres = newPostgresContainer("eventlab_http");
    @DynamicPropertySource static void props(DynamicPropertyRegistry r) {
        registerPostgresProperties(r, postgres);
        r.add("spring.kafka.bootstrap-servers", KafkaContainerSupport::bootstrapServers);
        r.add("app.event-lab.enabled", () -> "true");
    }
    @LocalServerPort int port;
    @Autowired JpaEventLabProcessedRepository repo;

    private String bearer(String token) { return "Bearer " + token; }

    @Test
    void sec001_noJwt401() {
        given().port(port).when().get("/api/event-lab").then().statusCode(401);
    }
    @Test
    void sec002_noEventLabRead403() {
        String token = TestJwtSupport.tokenWithRolesAndTenantId("no-read", java.util.List.of("merchants:read"), "TENANT_ALPHA");
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(token)).when().get("/api/event-lab").then().statusCode(403);
    }
    @Test
    void sec003_readWithoutOperateGet200Inject403() {
        String token = TestJwtSupport.tokenWithRolesAndTenantId("reader", java.util.List.of("platform:event-lab:read"), "PLATFORM_TENANT");
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(token)).when().get("/api/event-lab").then().statusCode(200);
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(ContentType.JSON).body(Map.of("eventId", UUID.randomUUID().toString())).when().post("/api/event-lab/inject/duplicate").then().statusCode(403);
    }
    @Test
    void ra030_injectDuplicate201() {
        String platformOp = TestJwtSupport.tokenWithRolesAndTenantId("op", java.util.List.of("platform:event-lab:read","platform:event-lab:operate"), "PLATFORM_TENANT");
        EventLabProcessed row = EventLabProcessed.of("eventlab-inspector", UUID.randomUUID(), "PAYMENT_AUTHORIZED","PAYMENT_ORDER", UUID.randomUUID().toString(), "PLATFORM_TENANT","PROCESSED","lab.auditable-actions.v1",0,0L, "k");
        repo.saveAndFlush(row);
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(platformOp)).contentType(ContentType.JSON).body(Map.of("eventId", row.getEventId().toString())).when().post("/api/event-lab/inject/duplicate").then().statusCode(201).body("eventId", equalTo(row.getEventId().toString()));
    }
    @Test
    void ra031_merchantManager403() {
        EventLabProcessed row = EventLabProcessed.of("eventlab-inspector", UUID.randomUUID(), "PAYMENT_AUTHORIZED","PAYMENT_ORDER", UUID.randomUUID().toString(), "PLATFORM_TENANT","PROCESSED","lab.auditable-actions.v1",0,0L, "k");
        repo.saveAndFlush(row);
        String mm = TestJwtSupport.tokenWithRolesTenantIdAndMerchantId("mm", java.util.List.of("merchant:payments:read"), "TENANT_ALPHA", UUID.randomUUID().toString());
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(mm)).contentType(ContentType.JSON).body(Map.of("eventId", row.getEventId().toString())).when().post("/api/event-lab/inject/duplicate").then().statusCode(403);
    }
    @Test
    void ra032_wrongTenant404() {
        EventLabProcessed row = EventLabProcessed.of("eventlab-inspector", UUID.randomUUID(), "PAYMENT_AUTHORIZED","PAYMENT_ORDER", UUID.randomUUID().toString(), "TENANT_BETA","PROCESSED","lab.auditable-actions.v1",0,0L, "k");
        repo.saveAndFlush(row);
        String tokenAlpha = TestJwtSupport.tokenWithRolesAndTenantId("reader", java.util.List.of("platform:event-lab:read"), "TENANT_ALPHA");
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(tokenAlpha)).when().get("/api/event-lab/" + row.getId()).then().statusCode(404).contentType(containsString("application/problem+json"));
    }
    @Test
    void ra033_injectNoBody400() {
        String op = TestJwtSupport.tokenWithRolesAndTenantId("op", java.util.List.of("platform:event-lab:operate"), "PLATFORM_TENANT");
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(op)).contentType(ContentType.JSON).when().post("/api/event-lab/inject/duplicate").then().statusCode(400);
    }
    @Test
    void sec005_correlationEcho() {
        String op = TestJwtSupport.tokenWithRolesAndTenantId("op", java.util.List.of("platform:event-lab:read"), "PLATFORM_TENANT");
        String cid = UUID.randomUUID().toString();
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(op)).header("X-Correlation-ID", cid).when().get("/api/event-lab").then().statusCode(200).header("X-Correlation-ID", equalTo(cid));
    }
}
