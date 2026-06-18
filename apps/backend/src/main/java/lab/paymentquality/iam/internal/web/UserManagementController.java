package lab.paymentquality.iam.internal.web;

import lab.paymentquality.iam.internal.application.UserListQuery;
import lab.paymentquality.iam.internal.application.UserManagementService;
import lab.paymentquality.iam.internal.web.dto.CreateUserRequest;
import lab.paymentquality.iam.internal.web.dto.RoleAssignmentRequest;
import lab.paymentquality.iam.internal.web.dto.UpdateUserRequest;
import lab.paymentquality.iam.internal.web.dto.UserDetail;
import lab.paymentquality.iam.internal.web.dto.UserListResponse;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService service;
    private final TenantResolver tenantResolver;

    public UserManagementController(UserManagementService service, TenantResolver tenantResolver) {
        this.service = service;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + Authorities.PLATFORM_USERS_READ
            + "','" + Authorities.TENANT_USERS_READ + "')")
    public ResponseEntity<UserListResponse> list(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext ctx = tenantResolver.resolve(jwt);
        String effectiveTenant = ctx.isPlatformScoped() && tenantId != null && !tenantId.isBlank()
                ? tenantId.strip()
                : null;
        UserListQuery query = new UserListQuery(effectiveTenant, role, status, search, page, size);
        return ResponseEntity.ok()
                .varyBy("Authorization")
                .body(service.list(query, ctx));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('" + Authorities.PLATFORM_USERS_CREATE
            + "','" + Authorities.TENANT_USERS_CREATE + "')")
    public ResponseEntity<UserDetail> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext ctx = tenantResolver.resolve(jwt);
        UserDetail created = service.create(request, ctx);
        return ResponseEntity
                .created(URI.create("/api/users/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + Authorities.PLATFORM_USERS_READ
            + "','" + Authorities.TENANT_USERS_READ + "')")
    public ResponseEntity<UserDetail> get(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext ctx = tenantResolver.resolve(jwt);
        return ResponseEntity.ok()
                .varyBy("Authorization")
                .body(service.get(id, ctx));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + Authorities.PLATFORM_USERS_UPDATE
            + "','" + Authorities.TENANT_USERS_UPDATE + "')")
    public ResponseEntity<UserDetail> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext ctx = tenantResolver.resolve(jwt);
        return ResponseEntity.ok(service.update(id, request, ctx));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAnyAuthority('" + Authorities.PLATFORM_USERS_ASSIGN_ROLES
            + "','" + Authorities.TENANT_USERS_ASSIGN_ROLES + "')")
    public ResponseEntity<UserDetail> assignRoles(
            @PathVariable String id,
            @Valid @RequestBody RoleAssignmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext ctx = tenantResolver.resolve(jwt);
        return ResponseEntity.ok(service.assignRoles(id, request, ctx));
    }
}
