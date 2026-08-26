package lab.paymentquality.tenant.internal.web;

import lab.paymentquality.shared.security.SecurityConfig;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolver;
import lab.paymentquality.tenant.internal.application.TenantSettingsService;
import lab.paymentquality.tenant.internal.application.TenantSettingsService.SettingsWithVersion;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static lab.paymentquality.testsupport.TestJwtSupport.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F-C4: WebMvcTest for tenant settings RBAC and ETag/If-Match contract.
 *
 * No Testcontainers, no DB — pure controller/security/ETag behaviour.
 *
 * Covers:
 *  1. PLATFORM_ADMIN GET returns settings + ETag.
 *  2. PLATFORM_ADMIN PATCH with matching If-Match succeeds and returns new ETag.
 *  3. Stale If-Match → 412.
 *  4. Missing If-Match → 428.
 *  5. Unauthorized role → 403.
 *  6. Unauthenticated → 401.
 *  7. Invalid timezone → 400.
 */
@WebMvcTest({TenantSettingsController.class, TenantSettingsExceptionHandler.class})
@ActiveProfiles("test")
@Import({SecurityConfig.class, TestJwtConfiguration.class})
class TenantSettingsControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private static final TenantContext PLATFORM_CTX = new TenantContext(
            TENANT_ID, TenantReference.of("PLATFORM_TENANT"), true);

    private static final TenantSettingsDto SAMPLE_DTO =
            new TenantSettingsDto("ops@example.com", "Europe/Warsaw", null, PaymentPolicyDto.from(null));

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TenantSettingsService settingsService;

    @MockitoBean
    TenantResolver tenantResolver;

    @Test
    void platformAdminGetSettingsReturnsETag() throws Exception {
        when(tenantResolver.resolve(any())).thenReturn(PLATFORM_CTX);
        when(settingsService.getSettings(PLATFORM_CTX))
                .thenReturn(new SettingsWithVersion(SAMPLE_DTO, 3L));

        mockMvc.perform(get("/api/tenants/current/settings")
                        .header("Authorization", "Bearer " + platformAdminToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"v3\""))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-transform")))
                .andExpect(jsonPath("$.contactEmail").value("ops@example.com"))
                .andExpect(jsonPath("$.timezone").value("Europe/Warsaw"));
    }

    @Test
    void platformAdminPatchWithMatchingIfMatchSucceeds() throws Exception {
        when(tenantResolver.resolve(any())).thenReturn(PLATFORM_CTX);
        TenantSettingsDto updated = new TenantSettingsDto(
                "new@example.com", "UTC", null, PaymentPolicyDto.from(null));
        when(settingsService.updateSettings(eq(PLATFORM_CTX), any(), eq(3L)))
                .thenReturn(new SettingsWithVersion(updated, 4L));

        mockMvc.perform(patch("/api/tenants/current/settings")
                        .header("Authorization", "Bearer " + platformAdminToken())
                        .header("If-Match", "\"v3\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"UTC\",\"contactEmail\":\"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"v4\""))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-transform")))
                .andExpect(jsonPath("$.contactEmail").value("new@example.com"))
                .andExpect(jsonPath("$.timezone").value("UTC"));
    }

    @Test
    void staleIfMatchReturns412() throws Exception {
        when(tenantResolver.resolve(any())).thenReturn(PLATFORM_CTX);
        when(settingsService.updateSettings(eq(PLATFORM_CTX), any(), eq(0L)))
                .thenThrow(new TenantSettingsPreconditionFailedException());

        mockMvc.perform(patch("/api/tenants/current/settings")
                        .header("Authorization", "Bearer " + platformAdminToken())
                        .header("If-Match", "\"v0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"UTC\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412))
                .andExpect(jsonPath("$.code").value("tenant_settings_version_mismatch"));
    }

    @Test
    void missingIfMatchReturns428() throws Exception {
        when(tenantResolver.resolve(any())).thenReturn(PLATFORM_CTX);

        mockMvc.perform(patch("/api/tenants/current/settings")
                        .header("Authorization", "Bearer " + platformAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"UTC\"}"))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428));
    }

    @Test
    void unauthorizedRoleReturns403() throws Exception {
        String merchantReadToken = tokenWithRoles("merchant.reader", java.util.List.of("merchant:payments:read"));

        mockMvc.perform(get("/api/tenants/current/settings")
                        .header("Authorization", "Bearer " + merchantReadToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/tenants/current/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTimezonePatternReturns400() throws Exception {
        when(tenantResolver.resolve(any())).thenReturn(PLATFORM_CTX);

        mockMvc.perform(patch("/api/tenants/current/settings")
                        .header("Authorization", "Bearer " + platformAdminToken())
                        .header("If-Match", "\"v0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"!! not valid !!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
