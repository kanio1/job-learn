package lab.paymentquality.merchant.internal.web;

import jakarta.validation.Valid;
import lab.paymentquality.merchant.internal.application.MerchantImportService;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/merchants/import")
public class MerchantImportController {

    private final MerchantImportService merchantImportService;
    private final TenantResolver tenantResolver;

    public MerchantImportController(MerchantImportService merchantImportService, TenantResolver tenantResolver) {
        this.merchantImportService = merchantImportService;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_CREATE + "')")
    public MerchantImportPreviewResponse preview(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        return merchantImportService.preview(file, tenantContext, jwt.getSubject());
    }

    @PostMapping("/commit")
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_CREATE + "')")
    public MerchantImportCommitResponse commit(
            @Valid @RequestBody MerchantImportCommitRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        return merchantImportService.commit(request.previewId(), tenantContext);
    }
}
