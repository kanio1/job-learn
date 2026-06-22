package lab.paymentquality.iam.internal.application;

import lab.paymentquality.iam.internal.domain.ManagedUser;
import lab.paymentquality.iam.internal.domain.exception.InvalidRoleException;
import lab.paymentquality.iam.internal.domain.exception.MissingTenantReferenceException;
import lab.paymentquality.iam.internal.domain.exception.TenantBoundaryViolationException;
import lab.paymentquality.iam.internal.domain.exception.UserNotFoundException;
import lab.paymentquality.iam.internal.infrastructure.KeycloakAdminClient;
import lab.paymentquality.iam.internal.web.dto.CreateUserRequest;
import lab.paymentquality.iam.internal.web.dto.RoleAssignmentRequest;
import lab.paymentquality.iam.internal.web.dto.UpdateUserRequest;
import lab.paymentquality.iam.internal.web.dto.UserDetail;
import lab.paymentquality.iam.internal.web.dto.UserListResponse;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID TENANT_BETA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

    private static final TenantContext TENANT_ALPHA_CONTEXT =
            new TenantContext(TENANT_ALPHA_ID, TenantReference.of("TENANT_ALPHA"), false);
    private static final TenantContext PLATFORM_CONTEXT =
            new TenantContext(PLATFORM_TENANT_ID, TenantReference.of("PLATFORM_TENANT"), true);

    @Mock
    KeycloakAdminClient adminClient;

    @Mock
    TenantResolver tenantResolver;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    UserManagementService service;

    // --- list ---

    @Test
    void tenantScopedListReturnsOnlyInScopeUsers() {
        ManagedUser inScope = managedUser("u1", "alice", "TENANT_ALPHA");
        ManagedUser outOfScope = managedUser("u2", "bob", "TENANT_BETA");
        when(adminClient.listUsers(anyInt(), anyInt(), any())).thenReturn(List.of(inScope, outOfScope));

        UserListQuery query = new UserListQuery(null, null, null, 0, 20);
        UserListResponse response = service.list(query, TENANT_ALPHA_CONTEXT);

        assertThat(response.users()).hasSize(1);
        assertThat(response.users().getFirst().username()).isEqualTo("alice");
    }

    @Test
    void platformListWithTenantFilterReturnsMatchingUsers() {
        ManagedUser alpha = managedUser("u1", "alice", "TENANT_ALPHA");
        ManagedUser beta = managedUser("u2", "bob", "TENANT_BETA");
        when(adminClient.listUsers(anyInt(), anyInt(), any())).thenReturn(List.of(alpha, beta));

        UserListQuery query = new UserListQuery("TENANT_ALPHA", null, null, null, 0, 20);
        UserListResponse response = service.list(query, PLATFORM_CONTEXT);

        assertThat(response.users()).hasSize(1);
        assertThat(response.users().getFirst().username()).isEqualTo("alice");
    }

    @Test
    void listWithRoleFilterReturnsMatchingUsers() {
        ManagedUser withRole = new ManagedUser("u1", "alice", "alice@test.com", true,
                "TENANT_ALPHA", null, List.of("PLATFORM_ADMIN"));
        ManagedUser withoutRole = new ManagedUser("u2", "bob", "bob@test.com", true,
                "TENANT_ALPHA", null, List.of("READ_ONLY_USER"));
        when(adminClient.listUsers(anyInt(), anyInt(), any())).thenReturn(List.of(withRole, withoutRole));

        UserListQuery query = new UserListQuery(null, "PLATFORM_ADMIN", null, null, 0, 20);
        UserListResponse response = service.list(query, TENANT_ALPHA_CONTEXT);

        assertThat(response.users()).hasSize(1);
        assertThat(response.users().getFirst().username()).isEqualTo("alice");
    }

    @Test
    void listWithStatusFilterReturnsMatchingUsers() {
        ManagedUser enabled = new ManagedUser("u1", "alice", "alice@test.com", true,
                "TENANT_ALPHA", null, List.of());
        ManagedUser disabled = new ManagedUser("u2", "bob", "bob@test.com", false,
                "TENANT_ALPHA", null, List.of());
        when(adminClient.listUsers(anyInt(), anyInt(), any())).thenReturn(List.of(enabled, disabled));

        UserListQuery query = new UserListQuery(null, null, "enabled", null, 0, 20);
        UserListResponse response = service.list(query, TENANT_ALPHA_CONTEXT);

        assertThat(response.users()).hasSize(1);
        assertThat(response.users().getFirst().username()).isEqualTo("alice");
    }

    @Test
    void listWithInvalidRoleFilterThrowsInvalidRoleException() {
        UserListQuery query = new UserListQuery(null, "NOT_A_ROLE", null, null, 0, 20);

        assertThatThrownBy(() -> service.list(query, PLATFORM_CONTEXT))
                .isInstanceOf(InvalidRoleException.class);
    }

    // --- get ---

    @Test
    void getReturnsUserForPlatformScopedCaller() {
        ManagedUser user = managedUser("u1", "alice", "TENANT_ALPHA");
        when(adminClient.getUser("u1")).thenReturn(Optional.of(user));

        UserDetail detail = service.get("u1", PLATFORM_CONTEXT);

        assertThat(detail.id()).isEqualTo("u1");
        assertThat(detail.username()).isEqualTo("alice");
    }

    @Test
    void getReturnsUserForTenantScopedCallerOnOwnTenant() {
        ManagedUser user = managedUser("u1", "alice", "TENANT_ALPHA");
        when(adminClient.getUser("u1")).thenReturn(Optional.of(user));

        UserDetail detail = service.get("u1", TENANT_ALPHA_CONTEXT);

        assertThat(detail.id()).isEqualTo("u1");
    }

    @Test
    void getCrossTenantReadThrowsMaskedNotFound() {
        ManagedUser user = managedUser("u1", "alice", "TENANT_BETA");
        when(adminClient.getUser("u1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.get("u1", TENANT_ALPHA_CONTEXT))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getNonExistentUserThrowsNotFound() {
        when(adminClient.getUser("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("unknown", PLATFORM_CONTEXT))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- create ---

    @Test
    void tenantScopedCreateIgnoresBodyTenantAndUsesCallerTenant() {
        when(adminClient.createUser(any(), eq("TENANT_ALPHA"))).thenReturn("new-id");
        when(adminClient.getUser("new-id")).thenReturn(
                Optional.of(managedUser("new-id", "alice", "TENANT_ALPHA")));

        CreateUserRequest request = new CreateUserRequest(
                "alice", "alice@test.com", "temp-password",
                "TENANT_BETA", null, List.of("TENANT_ADMIN"));

        UserDetail result = service.create(request, TENANT_ALPHA_CONTEXT);

        assertThat(result.id()).isEqualTo("new-id");
        verify(adminClient).createUser(any(), eq("TENANT_ALPHA"));
    }

    @Test
    void platformCreateWithValidTenantCreatesUser() {
        when(tenantResolver.resolveTenantId(TenantReference.of("TENANT_ALPHA")))
                .thenReturn(TENANT_ALPHA_ID);
        when(adminClient.createUser(any(), eq("TENANT_ALPHA"))).thenReturn("new-id");
        when(adminClient.getUser("new-id")).thenReturn(
                Optional.of(managedUser("new-id", "alice", "TENANT_ALPHA")));

        CreateUserRequest request = new CreateUserRequest(
                "alice", "alice@test.com", "temp-password",
                "TENANT_ALPHA", null, List.of("TENANT_ADMIN"));

        UserDetail result = service.create(request, PLATFORM_CONTEXT);

        assertThat(result.id()).isEqualTo("new-id");
    }

    @Test
    void platformCreateWithBlankTenantThrowsMissingTenantReference() {
        CreateUserRequest request = new CreateUserRequest(
                "alice", "alice@test.com", "temp-password",
                null, null, List.of("TENANT_ADMIN"));

        assertThatThrownBy(() -> service.create(request, PLATFORM_CONTEXT))
                .isInstanceOf(MissingTenantReferenceException.class);
        verify(adminClient, never()).createUser(any(), anyString());
    }

    @Test
    void createWithInvalidRoleThrowsInvalidRoleException() {
        CreateUserRequest request = new CreateUserRequest(
                "alice", "alice@test.com", "temp-password",
                "TENANT_ALPHA", null, List.of("RAW_AUTHORITY_ROLE"));

        assertThatThrownBy(() -> service.create(request, PLATFORM_CONTEXT))
                .isInstanceOf(InvalidRoleException.class);
        verify(adminClient, never()).createUser(any(), anyString());
    }

    @Test
    void createSetsTemporaryPasswordAndAssignsRoles() {
        when(tenantResolver.resolveTenantId(TenantReference.of("TENANT_ALPHA")))
                .thenReturn(TENANT_ALPHA_ID);
        when(adminClient.createUser(any(), eq("TENANT_ALPHA"))).thenReturn("new-id");
        when(adminClient.getUser("new-id")).thenReturn(
                Optional.of(managedUser("new-id", "alice", "TENANT_ALPHA")));

        CreateUserRequest request = new CreateUserRequest(
                "alice", "alice@test.com", "temp-password",
                "TENANT_ALPHA", null, List.of("TENANT_ADMIN"));

        service.create(request, PLATFORM_CONTEXT);

        InOrder order = inOrder(adminClient);
        order.verify(adminClient).createUser(any(), eq("TENANT_ALPHA"));
        order.verify(adminClient).setTemporaryPassword("new-id", "temp-password");
        order.verify(adminClient).assignRealmComposites("new-id", List.of("TENANT_ADMIN"));
    }

    // --- update ---

    @Test
    void updateRefetchesBeforeWriting() {
        ManagedUser current = managedUser("u1", "alice", "TENANT_ALPHA");
        ManagedUser updated = managedUser("u1", "alice", "TENANT_ALPHA");
        when(adminClient.getUser("u1"))
                .thenReturn(Optional.of(current))
                .thenReturn(Optional.of(updated));

        UpdateUserRequest request = new UpdateUserRequest("newemail@test.com", null, null);

        service.update("u1", request, TENANT_ALPHA_CONTEXT);

        InOrder order = inOrder(adminClient);
        order.verify(adminClient).getUser("u1");
        order.verify(adminClient).updateUser(eq("u1"), eq(current), any());
        order.verify(adminClient).getUser("u1");
    }

    @Test
    void updateCrossTenantWriteThrowsTenantBoundaryViolation() {
        ManagedUser user = managedUser("u1", "alice", "TENANT_BETA");
        when(adminClient.getUser("u1")).thenReturn(Optional.of(user));

        UpdateUserRequest request = new UpdateUserRequest("new@test.com", null, null);

        assertThatThrownBy(() -> service.update("u1", request, TENANT_ALPHA_CONTEXT))
                .isInstanceOf(TenantBoundaryViolationException.class);
        verify(adminClient, never()).updateUser(anyString(), any(), any());
    }

    @Test
    void updatePreservesTenantIdForTenantScopedCaller() {
        ManagedUser current = managedUser("u1", "alice", "TENANT_ALPHA");
        when(adminClient.getUser("u1"))
                .thenReturn(Optional.of(current))
                .thenReturn(Optional.of(current));

        UpdateUserRequest request = new UpdateUserRequest(
                "new@test.com", null,
                Map.of("tenant_id", List.of("HACKED_TENANT")));

        service.update("u1", request, TENANT_ALPHA_CONTEXT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UpdateUserRequest> captor = ArgumentCaptor.forClass(UpdateUserRequest.class);
        verify(adminClient).updateUser(eq("u1"), eq(current), captor.capture());

        UpdateUserRequest scoped = captor.getValue();
        assertThat(scoped.attributes().get("tenant_id")).containsExactly("TENANT_ALPHA");
    }

    // --- assignRoles ---

    @Test
    void assignRolesRefetchesBeforeWriting() {
        ManagedUser current = managedUser("u1", "alice", "TENANT_ALPHA");
        when(adminClient.getUser("u1"))
                .thenReturn(Optional.of(current))
                .thenReturn(Optional.of(current));

        RoleAssignmentRequest request = new RoleAssignmentRequest(
                List.of("TENANT_ADMIN"), List.of());

        service.assignRoles("u1", request, TENANT_ALPHA_CONTEXT);

        InOrder order = inOrder(adminClient);
        order.verify(adminClient).getUser("u1");
        order.verify(adminClient).assignRealmComposites("u1", List.of("TENANT_ADMIN"));
        order.verify(adminClient).getUser("u1");
    }

    @Test
    void assignRolesCrossTenantThrowsTenantBoundaryViolation() {
        ManagedUser user = managedUser("u1", "alice", "TENANT_BETA");
        when(adminClient.getUser("u1")).thenReturn(Optional.of(user));

        RoleAssignmentRequest request = new RoleAssignmentRequest(
                List.of("TENANT_ADMIN"), List.of());

        assertThatThrownBy(() -> service.assignRoles("u1", request, TENANT_ALPHA_CONTEXT))
                .isInstanceOf(TenantBoundaryViolationException.class);
        verify(adminClient, never()).assignRealmComposites(anyString(), any());
    }

    @Test
    void assignRolesWithInvalidRoleThrowsInvalidRoleException() {
        RoleAssignmentRequest request = new RoleAssignmentRequest(
                List.of("RAW_ROLE"), List.of());

        assertThatThrownBy(() -> service.assignRoles("u1", request, PLATFORM_CONTEXT))
                .isInstanceOf(InvalidRoleException.class);
        verify(adminClient, never()).getUser(anyString());
    }

    private ManagedUser managedUser(String id, String username, String tenantId) {
        return new ManagedUser(id, username, username + "@test.com", true,
                tenantId, null, List.of());
    }
}
