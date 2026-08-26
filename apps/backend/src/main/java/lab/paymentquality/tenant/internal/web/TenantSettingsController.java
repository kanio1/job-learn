package lab.paymentquality.tenant.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolver;
import lab.paymentquality.tenant.internal.application.TenantSettingsService;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * F-C4: Tenant settings REST endpoints.
 *
 * <pre>
 *   GET  /api/tenants/current/settings  → 200 + ETag
 *   PATCH /api/tenants/current/settings → 200 + new ETag  (requires If-Match)
 *                                        → 412             (stale ETag)
 *                                        → 428             (missing If-Match)
 * </pre>
 */
@RestController
@RequestMapping(value = "/api/tenants/current/settings", produces = MediaType.APPLICATION_JSON_VALUE)
class TenantSettingsController {

    private static final String X_CORRELATION_ID = "X-Correlation-ID";
    private static final String VARY_AUTHORIZATION = "Authorization";

    private final TenantSettingsService settingsService;
    private final TenantResolver tenantResolver;

    TenantSettingsController(TenantSettingsService settingsService, TenantResolver tenantResolver) {
        this.settingsService = settingsService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Authorities.TENANT_SETTINGS_READ + "')")
    ResponseEntity<TenantSettingsDto> getSettings(@AuthenticationPrincipal Jwt jwt) {
        TenantContext ctx = tenantResolver.resolve(jwt);
        var result = settingsService.getSettings(ctx);
        String etag = TenantSettingsEtag.from(result.version());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().noTransform())
                .varyBy(VARY_AUTHORIZATION)
                .header("ETag", etag)
                .header(X_CORRELATION_ID, correlationId())
                .body(result.dto());
    }

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + Authorities.TENANT_SETTINGS_UPDATE + "')")
    ResponseEntity<TenantSettingsDto> updateSettings(
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateTenantSettingsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        long expectedVersion = TenantSettingsEtag.requireVersion(ifMatch);
        TenantContext ctx = tenantResolver.resolve(jwt);
        var result = settingsService.updateSettings(ctx, request, expectedVersion);
        String etag = TenantSettingsEtag.from(result.version());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().noTransform())
                .varyBy(VARY_AUTHORIZATION)
                .header("ETag", etag)
                .header(X_CORRELATION_ID, correlationId())
                .body(result.dto());
    }

    private static String correlationId() {
        String id = MDC.get("correlationId");
        return id != null ? id : UUID.randomUUID().toString();
    }
}
