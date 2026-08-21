package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.application.OrgTreeService;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org-tree")
public class OrgTreeController {

    private final OrgTreeService orgTreeService;
    private final TenantResolver tenantResolver;

    public OrgTreeController(OrgTreeService orgTreeService, TenantResolver tenantResolver) {
        this.orgTreeService = orgTreeService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Authorities.MERCHANTS_READ + "')")
    public OrgTreeResponse get(@RequestParam(required = false) String parent,
                               @AuthenticationPrincipal Jwt jwt) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        if (parent == null || parent.isBlank()) {
            return orgTreeService.roots(tenantContext);
        }
        return orgTreeService.children(tenantContext, parent);
    }
}
