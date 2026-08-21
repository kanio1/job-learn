package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.PaymentOrderSearch;
import lab.paymentquality.merchant.internal.domain.SearchQueryRequiredException;
import lab.paymentquality.merchant.internal.web.MerchantListRequest;
import lab.paymentquality.merchant.internal.web.SearchMerchantHit;
import lab.paymentquality.merchant.internal.web.SearchPaymentHit;
import lab.paymentquality.merchant.internal.web.SearchResponse;
import lab.paymentquality.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EntitySearchService {

    static final int LIMIT = 10;
    static final int MAX_QUERY_LENGTH = 80;

    private final MerchantService merchantService;
    private final PaymentOrderSearch paymentOrderSearch;

    public EntitySearchService(MerchantService merchantService, PaymentOrderSearch paymentOrderSearch) {
        this.merchantService = merchantService;
        this.paymentOrderSearch = paymentOrderSearch;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(
            TenantContext tenantContext,
            String q,
            boolean includeMerchants,
            boolean includePayments,
            UUID scopedMerchantId) {
        if (q == null || q.isBlank()) {
            throw new SearchQueryRequiredException();
        }
        String query = q.strip();
        if (query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH);
        }
        List<SearchMerchantHit> merchants = includeMerchants
                ? merchantService.list(
                        tenantContext,
                        new MerchantListRequest(query, null, null, null, 0, LIMIT, "updatedAt,desc"),
                        null).content().stream()
                        .map(row -> new SearchMerchantHit(row.merchantId(), row.merchantReference(), row.displayName()))
                        .toList()
                : List.of();
        List<SearchPaymentHit> payments = includePayments
                ? paymentOrderSearch.searchByClientOrderReference(
                        query,
                        tenantContext.isPlatformScoped() ? null : tenantContext.tenantId(),
                        scopedMerchantId,
                        LIMIT).stream()
                        .map(hit -> new SearchPaymentHit(
                                hit.paymentOrderId(),
                                hit.merchantId(),
                                hit.clientOrderReference()))
                        .toList()
                : List.of();
        return new SearchResponse(merchants, payments);
    }
}
