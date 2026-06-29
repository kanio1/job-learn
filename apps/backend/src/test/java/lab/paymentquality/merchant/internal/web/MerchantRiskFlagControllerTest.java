package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.merchant.internal.domain.MerchantNotFoundException;
import lab.paymentquality.shared.security.SecurityConfig;
import lab.paymentquality.tenant.TenantResolver;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static lab.paymentquality.testsupport.TestJwtSupport.tokenWithRolesAndTenantId;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-C6: RBAC enforcement for PATCH /api/merchants/{id}/risk-flag.
 *
 * Uses @WebMvcTest (no Testcontainers, no DB) to verify:
 *   - platform admin with merchants:update-risk-flag → 200
 *   - merchant:read-only authority → 403
 *   - unauthenticated → 401
 *   - risk flag value reflected in response
 */
@WebMvcTest(MerchantController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, TestJwtConfiguration.class})
class MerchantRiskFlagControllerTest {

    private static final UUID MERCHANT_ID = UUID.fromString("aaaaaaaa-1111-4111-8111-111111111111");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MerchantService merchantService;

    @MockitoBean
    TenantResolver tenantResolver;

    private MerchantResponse riskFlaggedResponse(boolean flagged) {
        return new MerchantResponse(
                MERCHANT_ID, "RISK-MERCH-001", "Risk Test Merchant",
                "ACTIVE", Instant.now(), Instant.now(), flagged);
    }

    @Test
    void platformAdminCanMarkMerchantAsRiskFlagged() throws Exception {
        String token = tokenWithRolesAndTenantId("platform.admin",
                List.of("merchants:update-risk-flag"), "PLATFORM_TENANT");
        when(merchantService.updateRiskFlag(MERCHANT_ID, true))
                .thenReturn(riskFlaggedResponse(true));

        mockMvc.perform(patch("/api/merchants/{id}/risk-flag", MERCHANT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskFlagged\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskFlagged").value(true))
                .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID.toString()));
    }

    @Test
    void platformAdminCanClearRiskFlag() throws Exception {
        String token = tokenWithRolesAndTenantId("platform.admin",
                List.of("merchants:update-risk-flag"), "PLATFORM_TENANT");
        when(merchantService.updateRiskFlag(MERCHANT_ID, false))
                .thenReturn(riskFlaggedResponse(false));

        mockMvc.perform(patch("/api/merchants/{id}/risk-flag", MERCHANT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskFlagged\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskFlagged").value(false));
    }

    @Test
    void readOnlyRoleCannotUpdateRiskFlag() throws Exception {
        String token = tokenWithRolesAndTenantId("read.only",
                List.of("merchants:read"), "TENANT_ALPHA");

        mockMvc.perform(patch("/api/merchants/{id}/risk-flag", MERCHANT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskFlagged\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(patch("/api/merchants/{id}/risk-flag", MERCHANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskFlagged\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void merchantListIncludesRiskFlaggedField() throws Exception {
        String token = tokenWithRolesAndTenantId("platform.admin",
                List.of("merchants:update-risk-flag"), "PLATFORM_TENANT");
        when(merchantService.updateRiskFlag(MERCHANT_ID, true))
                .thenReturn(riskFlaggedResponse(true));

        mockMvc.perform(patch("/api/merchants/{id}/risk-flag", MERCHANT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riskFlagged\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskFlagged").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.displayName").value("Risk Test Merchant"));
    }
}
