package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.OrgTreeInvalidParentException;
import lab.paymentquality.merchant.internal.domain.TenantBoundaryViolationException;
import lab.paymentquality.merchant.internal.web.MerchantResponse;
import lab.paymentquality.merchant.internal.web.OrgTreeNode;
import lab.paymentquality.merchant.internal.web.OrgTreeResponse;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantDirectory;
import lab.paymentquality.tenant.TenantSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrgTreeService {

    private static final Pattern TENANT_PARENT = Pattern.compile("^tenant:([0-9a-fA-F-]{36})$");

    private final TenantDirectory tenantDirectory;
    private final MerchantService merchantService;

    public OrgTreeService(TenantDirectory tenantDirectory, MerchantService merchantService) {
        this.tenantDirectory = tenantDirectory;
        this.merchantService = merchantService;
    }

    @Transactional(readOnly = true)
    public OrgTreeResponse roots(TenantContext tenantContext) {
        List<TenantSummary> tenants = tenantContext.isPlatformScoped()
                ? tenantDirectory.listAll()
                : tenantDirectory.findById(tenantContext.tenantId()).stream().toList();
        return new OrgTreeResponse(tenants.stream().map(OrgTreeService::toTenantNode).toList());
    }

    @Transactional(readOnly = true)
    public OrgTreeResponse children(TenantContext tenantContext, String parent) {
        UUID tenantId = parseTenantParent(parent);
        if (tenantContext.isTenantScoped() && !tenantContext.tenantId().equals(tenantId)) {
            throw new TenantBoundaryViolationException();
        }
        List<MerchantResponse> merchants = merchantService.listByTenantId(tenantId);
        return new OrgTreeResponse(merchants.stream().map(OrgTreeService::toMerchantNode).toList());
    }

    private static UUID parseTenantParent(String parent) {
        if (parent == null || parent.isBlank()) {
            throw new OrgTreeInvalidParentException(String.valueOf(parent));
        }
        Matcher matcher = TENANT_PARENT.matcher(parent.strip());
        if (!matcher.matches()) {
            throw new OrgTreeInvalidParentException(parent);
        }
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new OrgTreeInvalidParentException(parent);
        }
    }

    private static OrgTreeNode toTenantNode(TenantSummary tenant) {
        return new OrgTreeNode(
                "tenant:" + tenant.tenantId(),
                "TENANT",
                tenant.name(),
                tenant.tenantReference(),
                true);
    }

    private static OrgTreeNode toMerchantNode(MerchantResponse merchant) {
        return new OrgTreeNode(
                "merchant:" + merchant.merchantId(),
                "MERCHANT",
                merchant.merchantReference(),
                merchant.merchantReference(),
                false);
    }
}
