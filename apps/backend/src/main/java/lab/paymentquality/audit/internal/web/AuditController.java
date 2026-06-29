package lab.paymentquality.audit.internal.web;

import lab.paymentquality.audit.internal.application.AuditEventService;
import lab.paymentquality.audit.internal.web.dto.AuditEventDetail;
import lab.paymentquality.audit.internal.web.dto.AuditExportResponse;
import lab.paymentquality.audit.internal.web.dto.AuditListResponse;
import lab.paymentquality.audit.internal.web.dto.AuditQuery;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventService service;
    private final TenantResolver tenantResolver;

    public AuditController(AuditEventService service, TenantResolver tenantResolver) {
        this.service = service;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('" + Authorities.PLATFORM_AUDIT_READ
            + "','" + Authorities.TENANT_AUDIT_READ + "')")
    public ResponseEntity<AuditListResponse> list(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(name = "target_type", required = false) String targetType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        AuditQuery query = AuditQuery.of(actor, action, targetType, from, to, page, size);
        return ResponseEntity.ok()
                .varyBy("Authorization")
                .body(service.list(query, tenantContext));
    }

    @GetMapping(value = "/export.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('" + Authorities.PLATFORM_AUDIT_READ
            + "','" + Authorities.TENANT_AUDIT_READ + "')")
    public ResponseEntity<AuditExportResponse> exportJson(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(name = "target_type", required = false) String targetType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        AuditQuery query = AuditQuery.of(actor, action, targetType, from, to, page, size);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Disposition", "attachment; filename=\"audit-events.json\"")
                .header("Cache-Control", "no-store")
                .varyBy("Authorization")
                .body(service.export(query, tenantContext));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('" + Authorities.PLATFORM_AUDIT_READ
            + "','" + Authorities.TENANT_AUDIT_READ + "')")
    public ResponseEntity<AuditEventDetail> get(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        return ResponseEntity.ok()
                .varyBy("Authorization")
                .body(service.get(id, tenantContext));
    }
}
