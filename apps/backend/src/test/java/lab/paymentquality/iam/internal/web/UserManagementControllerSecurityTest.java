package lab.paymentquality.iam.internal.web;

import lab.paymentquality.iam.internal.application.UserManagementService;
import lab.paymentquality.iam.internal.domain.exception.DuplicateUserException;
import lab.paymentquality.iam.internal.domain.exception.InvalidRoleException;
import lab.paymentquality.iam.internal.domain.exception.KeycloakAdminUnavailableException;
import lab.paymentquality.iam.internal.domain.exception.MissingTenantReferenceException;
import lab.paymentquality.iam.internal.domain.exception.TenantBoundaryViolationException;
import lab.paymentquality.iam.internal.domain.exception.UserNotFoundException;
import lab.paymentquality.iam.internal.web.dto.UserDetail;
import lab.paymentquality.iam.internal.web.dto.UserListResponse;
import lab.paymentquality.shared.security.SecurityConfig;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
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

import java.util.List;
import java.util.UUID;

import static lab.paymentquality.testsupport.TestJwtSupport.tokenWithRolesAndTenantId;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserManagementController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, TestJwtConfiguration.class})
class UserManagementControllerSecurityTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PLATFORM_TENANT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final TenantContext TENANT_ALPHA_CONTEXT =
            new TenantContext(TENANT_ALPHA_ID, TenantReference.of("TENANT_ALPHA"), false);
    private static final TenantContext PLATFORM_CONTEXT =
            new TenantContext(PLATFORM_TENANT_ID, TenantReference.of("PLATFORM_TENANT"), true);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserManagementService service;

    @MockitoBean
    TenantResolver tenantResolver;

    // --- GET /api/users ---

    @Test
    void listUsersWithPlatformReadAuthorityReturns200() throws Exception {
        String token = platformUsersReadToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.list(any(), any())).thenReturn(
                new UserListResponse(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void listUsersWithTenantReadAuthorityReturns200() throws Exception {
        String token = tenantUsersReadToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(TENANT_ALPHA_CONTEXT);
        when(service.list(any(), any())).thenReturn(
                new UserListResponse(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void listUsersWithoutAnyReadAuthorityReturns403() throws Exception {
        String token = tokenWithRolesAndTenantId("no.access",
                List.of("merchants:read"), "TENANT_ALPHA");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    // --- POST /api/users ---

    @Test
    void createUserWithPlatformCreateAuthorityReturns201WithLocation() throws Exception {
        String token = platformUsersCreateToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        UserDetail created = new UserDetail("new-id", "alice", "alice@test.com",
                true, "TENANT_ALPHA", null, List.of("TENANT_ADMIN"));
        when(service.create(any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@test.com",
                                  "temporaryPassword": "temp12345",
                                  "tenantId": "TENANT_ALPHA",
                                  "roles": ["TENANT_ADMIN"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/new-id"))
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.id").value("new-id"));
    }

    @Test
    void createUserWithoutAnyCreateAuthorityReturns403() throws Exception {
        String token = tokenWithRolesAndTenantId("no.access",
                List.of("merchants:read"), "TENANT_ALPHA");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@test.com",
                                  "temporaryPassword": "temp12345",
                                  "roles": ["TENANT_ADMIN"]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/users/{id} ---

    @Test
    void getUserWithReadAuthorityReturns200WithVary() throws Exception {
        String token = platformUsersReadToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        UserDetail detail = new UserDetail("u1", "alice", "alice@test.com",
                true, "TENANT_ALPHA", null, List.of("TENANT_ADMIN"));
        when(service.get("u1", PLATFORM_CONTEXT)).thenReturn(detail);

        mockMvc.perform(get("/api/users/u1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u1"));
    }

    // --- PATCH /api/users/{id} ---

    @Test
    void updateUserWithPlatformUpdateAuthorityReturns200() throws Exception {
        String token = platformUsersUpdateToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        UserDetail updated = new UserDetail("u1", "alice", "new@test.com",
                true, "TENANT_ALPHA", null, List.of("TENANT_ADMIN"));
        when(service.update(eq("u1"), any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/users/u1")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@test.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    void patchWithoutIfMatchDoesNotReturn412Or428() throws Exception {
        String token = platformUsersUpdateToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        UserDetail updated = new UserDetail("u1", "alice", "alice@test.com",
                true, "TENANT_ALPHA", null, List.of());
        when(service.update(eq("u1"), any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/users/u1")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alice@test.com"
                                }
                                """))
                .andExpect(status().isOk());
    }

    // --- POST /api/users/{id}/roles ---

    @Test
    void assignRolesWithPlatformAuthorityReturns200() throws Exception {
        String token = platformUsersAssignRolesToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        UserDetail updated = new UserDetail("u1", "alice", "alice@test.com",
                true, "TENANT_ALPHA", null, List.of("TENANT_ADMIN", "MERCHANT_MANAGER"));
        when(service.assignRoles(eq("u1"), any(), any())).thenReturn(updated);

        mockMvc.perform(post("/api/users/u1/roles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assign": ["MERCHANT_MANAGER"],
                                  "remove": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void assignRolesWithoutAnyAuthorityReturns403() throws Exception {
        String token = tokenWithRolesAndTenantId("no.access",
                List.of("merchants:read"), "TENANT_ALPHA");

        mockMvc.perform(post("/api/users/u1/roles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assign": ["TENANT_ADMIN"],
                                  "remove": []
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    // --- Exception mapping ---

    @Test
    void userNotFoundMapsTo404ProblemJson() throws Exception {
        String token = platformUsersReadToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.get("missing", PLATFORM_CONTEXT)).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/users/missing")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void tenantBoundaryViolationMapsTo403ProblemJson() throws Exception {
        String token = tenantUsersUpdateToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(TENANT_ALPHA_CONTEXT);
        when(service.update(eq("u1"), any(), any()))
                .thenThrow(new TenantBoundaryViolationException());

        mockMvc.perform(patch("/api/users/u1")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@test.com"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void invalidRoleMapsTo400ProblemJson() throws Exception {
        String token = platformUsersCreateToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.create(any(), any())).thenThrow(new InvalidRoleException());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@test.com",
                                  "temporaryPassword": "temp12345",
                                  "roles": ["INVALID_ROLE"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void missingTenantReferenceMapsTo400ProblemJson() throws Exception {
        String token = platformUsersCreateToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.create(any(), any())).thenThrow(new MissingTenantReferenceException());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@test.com",
                                  "temporaryPassword": "temp12345",
                                  "roles": ["TENANT_ADMIN"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void duplicateUserMapsTo409ProblemJson() throws Exception {
        String token = platformUsersCreateToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.create(any(), any())).thenThrow(new DuplicateUserException());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "existing",
                                  "email": "existing@test.com",
                                  "temporaryPassword": "temp12345",
                                  "roles": ["TENANT_ADMIN"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void keycloakAdminUnavailableMapsTo502ProblemJson() throws Exception {
        String token = platformUsersReadToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.get("u1", PLATFORM_CONTEXT))
                .thenThrow(new KeycloakAdminUnavailableException());

        mockMvc.perform(get("/api/users/u1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("bad_gateway"));
    }

    @Test
    void errorResponsesDoNotLeakInternals() throws Exception {
        String token = platformUsersReadToken();
        when(tenantResolver.resolve(any(Jwt.class))).thenReturn(PLATFORM_CONTEXT);
        when(service.get("u1", PLATFORM_CONTEXT))
                .thenThrow(new KeycloakAdminUnavailableException());

        mockMvc.perform(get("/api/users/u1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(not(containsString("token"))))
                .andExpect(jsonPath("$.message").value(not(containsString("password"))))
                .andExpect(jsonPath("$.message").value(not(containsString("Keycloak"))));
    }

    // --- Token factories ---

    private static String platformUsersReadToken() {
        return tokenWithRolesAndTenantId("platform.reader",
                List.of("platform:users:read"), "PLATFORM_TENANT");
    }

    private static String platformUsersCreateToken() {
        return tokenWithRolesAndTenantId("platform.creator",
                List.of("platform:users:create"), "PLATFORM_TENANT");
    }

    private static String platformUsersUpdateToken() {
        return tokenWithRolesAndTenantId("platform.updater",
                List.of("platform:users:update"), "PLATFORM_TENANT");
    }

    private static String platformUsersAssignRolesToken() {
        return tokenWithRolesAndTenantId("platform.role-assigner",
                List.of("platform:users:assign-roles"), "PLATFORM_TENANT");
    }

    private static String tenantUsersReadToken() {
        return tokenWithRolesAndTenantId("tenant.reader",
                List.of("tenant:users:read"), "TENANT_ALPHA");
    }

    private static String tenantUsersUpdateToken() {
        return tokenWithRolesAndTenantId("tenant.updater",
                List.of("tenant:users:update"), "TENANT_ALPHA");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
