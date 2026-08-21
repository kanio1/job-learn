package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.application.EntitySearchService;
import lab.paymentquality.shared.security.Authorities;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/search")
public class EntitySearchController {

    private final EntitySearchService entitySearchService;
    private final TenantResolver tenantResolver;

    public EntitySearchController(EntitySearchService entitySearchService, TenantResolver tenantResolver) {
        this.entitySearchService = entitySearchService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + Authorities.MERCHANTS_READ + "', '"
            + Authorities.MERCHANT_PAYMENTS_READ + "', '"
            + Authorities.PLATFORM_PAYMENTS_READ + "')")
    public SearchResponse search(@RequestParam(required = false) String q,
                                 @AuthenticationPrincipal Jwt jwt,
                                 Authentication authentication) {
        TenantContext tenantContext = tenantResolver.resolve(jwt);
        boolean includeMerchants = hasAuthority(authentication, Authorities.MERCHANTS_READ);
        boolean includePayments = hasAuthority(authentication, Authorities.MERCHANT_PAYMENTS_READ)
                || hasAuthority(authentication, Authorities.PLATFORM_PAYMENTS_READ);
        UUID scopedMerchantId = parseUuid(jwt.getClaimAsString("merchant_id"));
        return entitySearchService.search(tenantContext, q, includeMerchants, includePayments, scopedMerchantId);
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
