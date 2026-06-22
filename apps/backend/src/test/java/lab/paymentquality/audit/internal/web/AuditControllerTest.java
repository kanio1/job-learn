package lab.paymentquality.audit.internal.web;

import lab.paymentquality.audit.internal.application.AuditEventService;
import lab.paymentquality.audit.internal.domain.exception.AuditEventNotFoundException;
import lab.paymentquality.audit.internal.web.dto.AuditEventDetail;
import lab.paymentquality.audit.internal.web.dto.AuditEventSummary;
import lab.paymentquality.audit.internal.web.dto.AuditListResponse;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.shared.security.SecurityConfig;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolver;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static lab.paymentquality.testsupport.TestJwtSupport.tokenWithRolesAndTenantId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, TestJwtConfiguration.class})
class AuditControllerTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000017");
    private static final TenantContext PLATFORM_CONTEXT = new TenantContext(
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            TenantReference.of("PLATFORM_TENANT"), true);
    private static final TenantContext TENANT_CONTEXT = new TenantContext(
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            TenantReference.of("TENANT_ALPHA"), false);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuditEventService service;

    @MockitoBean
    TenantResolver tenantResolver;

    @Test
    void platformListReturnsSafePageAndRequiredHeaders() throws Exception {
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.list(any(), eq(PLATFORM_CONTEXT))).thenReturn(new AuditListResponse(
                List.of(summary()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/audit")
                        .header("Authorization", bearer(platformToken()))
                        .header("X-Correlation-ID", "audit-web-correlation"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeaders("Vary"))
                        .contains("Authorization"))
                .andExpect(header().string("X-Correlation-ID", "audit-web-correlation"))
                .andExpect(jsonPath("$.content[0].id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.content[0].actorDisplay").value("Visible Operator"))
                .andExpect(jsonPath("$.content[0].actorSubject").doesNotExist());
    }

    @Test
    void tenantListWithTenantAuthorityReturns200() throws Exception {
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(TENANT_CONTEXT);
        when(service.list(any(), eq(TENANT_CONTEXT))).thenReturn(new AuditListResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/audit").header("Authorization", bearer(tenantToken())))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void listWithoutAuditAuthorityReturns403ProblemWithoutAuthorityDisclosure() throws Exception {
        mockMvc.perform(get("/api/audit").header("Authorization", bearer(deniedToken())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", not(containsString("audit:read"))))
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void visibleDetailReturns200AndRedactedBody() throws Exception {
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(TENANT_CONTEXT);
        when(service.get(EVENT_ID.toString(), TENANT_CONTEXT)).thenReturn(detail());

        mockMvc.perform(get("/api/audit/{id}", EVENT_ID)
                        .header("Authorization", bearer(tenantToken())))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeaders("Vary"))
                        .contains("Authorization"))
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.actorSubject").doesNotExist());
    }

    @Test
    void nonexistentAndCrossTenantUseIdenticalMaskedProblem() throws Exception {
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(TENANT_CONTEXT);
        when(service.get(EVENT_ID.toString(), TENANT_CONTEXT))
                .thenThrow(new AuditEventNotFoundException())
                .thenThrow(new AuditEventNotFoundException());

        String first = mockMvc.perform(get("/api/audit/{id}", EVENT_ID)
                        .header("Authorization", bearer(tenantToken()))
                        .header("X-Correlation-ID", "masked-correlation"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("not_found"))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get("/api/audit/{id}", EVENT_ID)
                        .header("Authorization", bearer(tenantToken()))
                        .header("X-Correlation-ID", "masked-correlation"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
        assertThat(first).doesNotContain("TENANT_ALPHA");
    }

    @Test
    void malformedIdentifierUsesMasked404() throws Exception {
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.get("malformed", PLATFORM_CONTEXT)).thenThrow(new AuditEventNotFoundException());

        mockMvc.perform(get("/api/audit/malformed").header("Authorization", bearer(platformToken())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(post("/api/audit").header("Authorization", bearer(platformToken())))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unsupportedAcceptReturns406() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .header("Authorization", bearer(platformToken()))
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    private static AuditEventSummary summary() {
        return new AuditEventSummary(
                EVENT_ID, Instant.parse("2026-06-19T10:00:00Z"), "Visible Operator",
                "MERCHANT_CREATED", "MERCHANT", "merchant-17", "TENANT_ALPHA",
                "correlation-17", Outcome.SUCCESS);
    }

    private static AuditEventDetail detail() {
        AuditEventSummary summary = summary();
        return new AuditEventDetail(
                summary.id(), summary.occurredAt(), summary.actorDisplay(), summary.action(),
                summary.targetType(), summary.targetId(), summary.tenantId(),
                summary.correlationId(), summary.outcome());
    }

    private static String platformToken() {
        return tokenWithRolesAndTenantId(
                "platform.audit.reader", List.of("platform:audit:read"), "PLATFORM_TENANT");
    }

    private static String tenantToken() {
        return tokenWithRolesAndTenantId(
                "tenant.audit.reader", List.of("tenant:audit:read"), "TENANT_ALPHA");
    }

    private static String deniedToken() {
        return tokenWithRolesAndTenantId(
                "audit.denied", List.of("merchants:read"), "TENANT_ALPHA");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
