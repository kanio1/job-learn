package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.merchant.internal.domain.MissingTenantReferenceException;
import lab.paymentquality.merchant.internal.domain.TenantBoundaryViolationException;
import lab.paymentquality.shared.security.SecurityConfig;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MerchantController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, TestJwtConfiguration.class})
class MerchantControllerTenantSecurityTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PLATFORM_TENANT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final TenantContext TENANT_ALPHA_CONTEXT =
            new TenantContext(TENANT_ALPHA_ID, TenantReference.of("TENANT_ALPHA"), false);
    private static final TenantContext PLATFORM_CONTEXT =
            new TenantContext(PLATFORM_TENANT_ID, TenantReference.of("PLATFORM_TENANT"), true);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MerchantService merchantService;

    @MockitoBean
    TenantResolver tenantResolver;

    @Test
    void getMerchantWithTenantScopedJwtResolvesTenantOnceAndCallsServiceWithContext() throws Exception {
        UUID merchantId = UUID.randomUUID();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(TENANT_ALPHA_CONTEXT);
        when(merchantService.findById(merchantId, TENANT_ALPHA_CONTEXT))
                .thenReturn(new MerchantResponse(
                        merchantId,
                        "MERCH-ALPHA",
                        "Alpha Merchant",
                        "DRAFT",
                        java.time.Instant.parse("2026-01-01T00:00:00Z"),
                        java.time.Instant.parse("2026-01-01T00:00:00Z"),
                        false,
                        0L));

        mockMvc.perform(get("/api/merchants/{id}", merchantId)
                        .header("Authorization", bearer(TestJwtSupport.tenantAdminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()));

        verify(tenantResolver, times(1)).resolve(any(Jwt.class));
        verify(merchantService).findById(merchantId, TENANT_ALPHA_CONTEXT);
    }

    @Test
    void platformCreateMissingTenantReferenceServiceExceptionMapsToProblemJson400() throws Exception {
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(merchantService.create(eq("MERCH-MISSING"), eq("Missing Tenant"), eq(PLATFORM_CONTEXT), eq(null)))
                .thenThrow(new MissingTenantReferenceException());

        mockMvc.perform(post("/api/merchants")
                        .header("Authorization", bearer(TestJwtSupport.platformAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantReference": "MERCH-MISSING",
                                  "displayName": "Missing Tenant"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("validation"))
                .andExpect(jsonPath("$.message").value("Invalid merchant request"));
    }

    @Test
    void suspendTenantBoundaryViolationMapsToGenericProblemJson403() throws Exception {
        UUID merchantId = UUID.randomUUID();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(TENANT_ALPHA_CONTEXT);
        when(merchantService.suspend(merchantId, TENANT_ALPHA_CONTEXT, 0L))
                .thenThrow(new TenantBoundaryViolationException());

        mockMvc.perform(post("/api/merchants/{id}/suspend", merchantId)
                        .header("Authorization", bearer(TestJwtSupport.tenantAdminToken()))
                        .header("If-Match", "\"v0\""))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.message").value("Forbidden"))
                .andExpect(jsonPath("$.details").value("Access denied"))
                .andExpect(content().string(not(containsString("TENANT_ALPHA"))))
                .andExpect(content().string(not(containsString(TENANT_ALPHA_ID.toString()))));
    }

    @Test
    void jwtWithoutTenantClaimResolverFailureMapsToGenericProblemJson403() throws Exception {
        when(tenantResolver.resolve(any(Jwt.class)))
                .thenThrow(new TenantResolutionException("TENANT_ALPHA " + TENANT_ALPHA_ID));

        mockMvc.perform(get("/api/merchants/{id}", UUID.randomUUID())
                        .header("Authorization", bearer(TestJwtSupport.tokenWithoutTenantClaim())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.message").value("Forbidden"))
                .andExpect(jsonPath("$.details").value("Access denied"))
                .andExpect(content().string(not(containsString("TENANT_ALPHA"))))
                .andExpect(content().string(not(containsString(TENANT_ALPHA_ID.toString()))));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
