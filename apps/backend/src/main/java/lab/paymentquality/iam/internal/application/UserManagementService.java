package lab.paymentquality.iam.internal.application;

import lab.paymentquality.iam.internal.domain.CompositeRole;
import lab.paymentquality.iam.internal.domain.ManagedUser;
import lab.paymentquality.iam.internal.domain.exception.InvalidRoleException;
import lab.paymentquality.iam.internal.domain.exception.MissingTenantReferenceException;
import lab.paymentquality.iam.internal.domain.exception.TenantBoundaryViolationException;
import lab.paymentquality.iam.internal.domain.exception.UserNotFoundException;
import lab.paymentquality.iam.internal.infrastructure.KeycloakAdminClient;
import lab.paymentquality.iam.internal.web.UserMapper;
import lab.paymentquality.iam.internal.web.dto.CreateUserRequest;
import lab.paymentquality.iam.internal.web.dto.RoleAssignmentRequest;
import lab.paymentquality.iam.internal.web.dto.UpdateUserRequest;
import lab.paymentquality.iam.internal.web.dto.UserDetail;
import lab.paymentquality.iam.internal.web.dto.UserListResponse;
import lab.paymentquality.iam.internal.web.dto.UserSummary;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class UserManagementService {

    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";
    private static final String TENANT_ID_ATTRIBUTE = "tenant_id";

    private final KeycloakAdminClient adminClient;
    private final TenantResolver tenantResolver;

    public UserManagementService(
            KeycloakAdminClient adminClient,
            TenantResolver tenantResolver) {
        this.adminClient = adminClient;
        this.tenantResolver = tenantResolver;
    }

    public UserListResponse list(UserListQuery query, TenantContext tenantContext) {
        validateRoleFilter(query.role());
        validateStatusFilter(query.status());

        int first = Math.multiplyExact(query.page(), query.size());
        List<ManagedUser> candidates = adminClient.listUsers(first, query.size(), query.search());
        List<UserSummary> users = candidates.stream()
                .filter(user -> isVisibleInList(user, query, tenantContext))
                .map(UserMapper::toSummary)
                .toList();

        long totalEstimate = (long) first + candidates.size();
        if (candidates.size() == query.size()) {
            totalEstimate++;
        }
        return new UserListResponse(users, query.page(), query.size(), totalEstimate);
    }

    public UserDetail get(String id, TenantContext tenantContext) {
        ManagedUser user = findUser(id);
        enforceReadBoundary(user, tenantContext);
        return UserMapper.toDetail(user);
    }

    public UserDetail create(CreateUserRequest request, TenantContext tenantContext) {
        validateRolesAreComposite(request.roles());
        String tenantReference = resolveCreateTenant(tenantContext, request.tenantId());

        String userId = adminClient.createUser(request, tenantReference);
        adminClient.setTemporaryPassword(userId, request.temporaryPassword());
        adminClient.assignRealmComposites(userId, request.roles());

        return UserMapper.toDetail(findUser(userId));
    }

    public UserDetail update(
            String id,
            UpdateUserRequest request,
            TenantContext tenantContext) {
        ManagedUser current = findUser(id);
        enforceWriteBoundary(current, tenantContext);

        UpdateUserRequest scopedRequest = preserveTenantScope(request, tenantContext);
        adminClient.updateUser(id, current, scopedRequest);
        return UserMapper.toDetail(findUser(id));
    }

    public UserDetail assignRoles(
            String id,
            RoleAssignmentRequest request,
            TenantContext tenantContext) {
        validateRolesAreComposite(request.assign());
        validateRolesAreComposite(request.remove());

        ManagedUser current = findUser(id);
        enforceWriteBoundary(current, tenantContext);

        adminClient.assignRealmComposites(id, request.assign());
        adminClient.removeRealmComposites(id, request.remove());
        return UserMapper.toDetail(findUser(id));
    }

    private ManagedUser findUser(String id) {
        return adminClient.getUser(id).orElseThrow(UserNotFoundException::new);
    }

    private boolean isVisibleInList(
            ManagedUser user,
            UserListQuery query,
            TenantContext tenantContext) {
        return matchesTenant(user, query.tenantId(), tenantContext)
                && matchesRole(user, query.role())
                && matchesStatus(user, query.status())
                && matchesSearch(user, query.search());
    }

    private boolean matchesTenant(
            ManagedUser user,
            String requestedTenant,
            TenantContext tenantContext) {
        if (tenantContext.isTenantScoped()) {
            return Objects.equals(user.tenantId(), tenantContext.tenantReference().value());
        }
        return !hasText(requestedTenant)
                || Objects.equals(user.tenantId(), requestedTenant.strip());
    }

    private boolean matchesRole(ManagedUser user, String role) {
        return !hasText(role) || user.roles().contains(role.strip());
    }

    private boolean matchesStatus(ManagedUser user, String status) {
        if (!hasText(status)) {
            return true;
        }
        return ENABLED.equalsIgnoreCase(status.strip()) ? user.enabled() : !user.enabled();
    }

    private boolean matchesSearch(ManagedUser user, String search) {
        if (!hasText(search)) {
            return true;
        }
        String needle = search.strip().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(user.username(), needle)
                || containsIgnoreCase(user.email(), needle);
    }

    private void enforceReadBoundary(ManagedUser user, TenantContext tenantContext) {
        if (tenantContext.isTenantScoped()
                && !Objects.equals(user.tenantId(), tenantContext.tenantReference().value())) {
            throw new UserNotFoundException();
        }
    }

    private void enforceWriteBoundary(ManagedUser user, TenantContext tenantContext) {
        if (tenantContext.isTenantScoped()
                && !Objects.equals(user.tenantId(), tenantContext.tenantReference().value())) {
            throw new TenantBoundaryViolationException();
        }
    }

    private String resolveCreateTenant(TenantContext tenantContext, String requestedTenant) {
        if (tenantContext.isTenantScoped()) {
            return tenantContext.tenantReference().value();
        }
        if (!hasText(requestedTenant)) {
            throw new MissingTenantReferenceException();
        }

        String tenantReference = requestedTenant.strip();
        try {
            tenantResolver.resolveTenantId(TenantReference.of(tenantReference));
        } catch (IllegalArgumentException | TenantResolutionException exception) {
            throw new MissingTenantReferenceException();
        }
        return tenantReference;
    }

    private UpdateUserRequest preserveTenantScope(
            UpdateUserRequest request,
            TenantContext tenantContext) {
        if (tenantContext.isPlatformScoped() || request.attributes() == null) {
            return request;
        }

        Map<String, List<String>> attributes = new LinkedHashMap<>(request.attributes());
        attributes.put(TENANT_ID_ATTRIBUTE, List.of(tenantContext.tenantReference().value()));
        return new UpdateUserRequest(request.email(), request.enabled(), attributes);
    }

    private void validateRoleFilter(String role) {
        if (hasText(role) && !CompositeRole.isAssignable(role.strip())) {
            throw new InvalidRoleException();
        }
    }

    private void validateStatusFilter(String status) {
        if (hasText(status)
                && !ENABLED.equalsIgnoreCase(status.strip())
                && !DISABLED.equalsIgnoreCase(status.strip())) {
            throw new IllegalArgumentException("status must be enabled or disabled");
        }
    }

    private void validateRolesAreComposite(Collection<String> roles) {
        if (roles == null) {
            throw new InvalidRoleException();
        }
        for (String role : roles) {
            if (!CompositeRole.isAssignable(role)) {
                throw new InvalidRoleException();
            }
        }
    }

    private static boolean containsIgnoreCase(String value, String lowerCaseNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
