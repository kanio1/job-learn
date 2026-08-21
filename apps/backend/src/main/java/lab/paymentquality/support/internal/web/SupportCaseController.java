package lab.paymentquality.support.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.support.internal.application.SupportCaseService;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/support/cases")
public class SupportCaseController {

    private final SupportCaseService supportCaseService;
    private final TenantResolver tenantResolver;

    public SupportCaseController(SupportCaseService supportCaseService, TenantResolver tenantResolver) {
        this.supportCaseService = supportCaseService;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Authorities.SUPPORT_OPERATE + "')")
    public ResponseEntity<SupportCaseResponse> create(
            @Valid @RequestBody CreateSupportCaseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        SupportCaseResponse response = supportCaseService.create(request, tenantContext);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/support/cases/" + response.caseId()))
                .header("ETag", SupportEtag.from(response.version()))
                .body(response);
    }

    @PostMapping("/bulk-assign")
    @PreAuthorize("hasAuthority('" + Authorities.SUPPORT_OPERATE + "')")
    public ResponseEntity<BulkAssignResponse> bulkAssign(
            @Valid @RequestBody BulkAssignRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        return ResponseEntity.ok(supportCaseService.bulkAssign(
                request.caseIds(), request.assigneeSubject(), tenantContext));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Authorities.SUPPORT_READ + "')")
    public ResponseEntity<SupportCaseListResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignee,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        return ResponseEntity.ok(SupportCaseMapper.toListResponse(
                supportCaseService.list(tenantContext, status, assignee)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Authorities.SUPPORT_READ + "')")
    public ResponseEntity<SupportCaseResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID caseId = parseUuid(id);
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        SupportCaseResponse response = supportCaseService.getById(caseId, tenantContext);
        return ResponseEntity.ok()
                .header("ETag", SupportEtag.from(response.version()))
                .body(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Authorities.SUPPORT_OPERATE + "')")
    public ResponseEntity<SupportCaseResponse> patch(
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody UpdateSupportCaseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID caseId = parseUuid(id);
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        long expectedVersion = SupportEtag.requireVersion(ifMatch);
        SupportCaseResponse response = supportCaseService.update(caseId, request, tenantContext, expectedVersion);
        return ResponseEntity.ok()
                .header("ETag", SupportEtag.from(response.version()))
                .body(response);
    }

    private static UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new InvalidSupportCaseRequestException("Invalid support case ID");
        }
    }
}
