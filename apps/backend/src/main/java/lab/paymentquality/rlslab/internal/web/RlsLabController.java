package lab.paymentquality.rlslab.internal.web;

import lab.paymentquality.rlslab.internal.application.RlsLabCompareResult;
import lab.paymentquality.rlslab.internal.application.RlsLabItem;
import lab.paymentquality.rlslab.internal.application.RlsLabService;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rls-lab")
@Profile("!prod")
@ConditionalOnProperty(name = "app.rls-lab.enabled", havingValue = "true")
class RlsLabController {

    private final RlsLabService service;
    private final TenantResolver tenantResolver;

    RlsLabController(RlsLabService service, TenantResolver tenantResolver) {
        this.service = service;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/items")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Map<String, Object>> items(@AuthenticationPrincipal Jwt jwt) {
        TenantContext tenant = tenantResolver.resolve(jwt);
        List<Map<String, Object>> items = service.list(tenant).stream()
                .map(RlsLabController::toBody)
                .toList();
        return ResponseEntity.ok(Map.of("items", items));
    }

    @GetMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Map<String, Object>> item(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenant = tenantResolver.resolve(jwt);
        return ResponseEntity.ok(toBody(service.requireItem(tenant, itemId)));
    }

    /**
     * Leak demo. Three gates, on purpose:
     * <ol>
     *   <li>{@code SecurityConfig} URL matcher — {@code platform:payments:read}</li>
     *   <li>{@code @PreAuthorize} — same authority (method security, not only the matcher)</li>
     *   <li>service — {@code TenantContext.isPlatformScoped()} → {@code rls_forbidden}</li>
     * </ol>
     * Missing authority is 403 from Spring Security. Authority plus a merchant tenant
     * is 403 {@code rls_forbidden}. JSON keys name the BYPASSRLS role, not a client GUC.
     */
    @GetMapping("/compare")
    @PreAuthorize("hasAuthority('" + Authorities.PLATFORM_PAYMENTS_READ + "')")
    ResponseEntity<Map<String, Object>> compare(@AuthenticationPrincipal Jwt jwt) {
        TenantContext tenant = tenantResolver.resolve(jwt);
        RlsLabCompareResult result = service.compare(tenant);
        return ResponseEntity.ok(Map.of(
                "bypassRoleCount", result.bypassRoleCount(),
                "restrictedWithoutTenantGuc", result.restrictedWithoutTenantGuc(),
                "unprotected", result.unprotected()));
    }

    private static Map<String, Object> toBody(RlsLabItem item) {
        return Map.of(
                "itemId", item.itemId(),
                "tenantId", item.tenantId(),
                "label", item.label(),
                "amountMinor", item.amountMinor());
    }
}
