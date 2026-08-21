package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;
    private final TenantResolver tenantResolver;

    public MerchantController(MerchantService merchantService, TenantResolver tenantResolver) {
        this.merchantService = merchantService;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_CREATE + "')")
    public ResponseEntity<MerchantResponse> create(@Valid @RequestBody CreateMerchantRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        var merchant = merchantService.create(
                request.merchantReference(),
                request.displayName(),
                tenantContext,
                request.tenantReference());
        var response = MerchantMapper.toResponse(merchant);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("ETag", MerchantEtag.from(response.version()))
                .body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_READ + "')")
    public ResponseEntity<MerchantResponse> getById(@PathVariable String id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID uuid = parseUUID(id);
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        var response = merchantService.findById(uuid, tenantContext);
        return ResponseEntity.ok()
                .header("ETag", MerchantEtag.from(response.version()))
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_READ + "')")
    public ResponseEntity<MerchantListResponse> list(@Valid @ModelAttribute MerchantListRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        UUID filterTenantId = null;
        if (tenantContext.isPlatformScoped() && request.tenantId() != null) {
            try {
                filterTenantId = tenantResolver.resolveTenantId(TenantReference.of(request.tenantId()));
            } catch (IllegalArgumentException | TenantResolutionException e) {
                return ResponseEntity.ok(MerchantListResponse.empty(request.effectivePage(), request.effectiveSize()));
            }
        }

        return ResponseEntity.ok(merchantService.list(tenantContext, request, filterTenantId));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_UPDATE_STATUS + "')")
    public ResponseEntity<MerchantResponse> activate(@PathVariable String id,
                                                     @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                     @AuthenticationPrincipal Jwt jwt) {
        UUID uuid = parseUUID(id);
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        long expectedVersion = MerchantEtag.requireVersion(ifMatch);
        var response = merchantService.activate(uuid, tenantContext, expectedVersion);
        return ResponseEntity.ok()
                .header("ETag", MerchantEtag.from(response.version()))
                .body(response);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_UPDATE_STATUS + "')")
    public ResponseEntity<MerchantResponse> suspend(@PathVariable String id,
                                                    @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID uuid = parseUUID(id);
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        long expectedVersion = MerchantEtag.requireVersion(ifMatch);
        var response = merchantService.suspend(uuid, tenantContext, expectedVersion);
        return ResponseEntity.ok()
                .header("ETag", MerchantEtag.from(response.version()))
                .body(response);
    }

    @PatchMapping("/{id}/risk-flag")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_UPDATE_RISK_FLAG + "')")
    public ResponseEntity<MerchantResponse> updateRiskFlag(@PathVariable String id,
                                                           @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                           @RequestBody UpdateRiskFlagRequest request) {
        UUID uuid = parseUUID(id);
        long expectedVersion = MerchantEtag.requireVersion(ifMatch);
        var response = merchantService.updateRiskFlag(uuid, request.riskFlagged(), expectedVersion);
        return ResponseEntity.ok()
                .header("ETag", MerchantEtag.from(response.version()))
                .body(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_UPDATE_STATUS + "')")
    public ResponseEntity<MerchantResponse> patch(@PathVariable String id,
                                                  @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                  @RequestBody UpdateMerchantRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        UUID uuid = parseUUID(id);
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        long expectedVersion = MerchantEtag.requireVersion(ifMatch);
        var response = merchantService.patch(uuid, request, tenantContext, expectedVersion);
        return ResponseEntity.ok()
                .header("ETag", MerchantEtag.from(response.version()))
                .body(response);
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid merchant ID: " + id, e);
        }
    }
}
