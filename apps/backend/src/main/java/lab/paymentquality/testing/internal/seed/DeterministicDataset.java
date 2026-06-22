package lab.paymentquality.testing.internal.seed;

import lab.paymentquality.merchant.MerchantSeedCapability;
import lab.paymentquality.payment.PaymentSeedCapability;
import lab.paymentquality.tenant.TenantSeedCapability;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeterministicDataset {

    private final TenantSeedCapability tenants;
    private final MerchantSeedCapability merchants;
    private final PaymentSeedCapability payments;

    public DeterministicDataset(TenantSeedCapability tenants,
                                MerchantSeedCapability merchants,
                                PaymentSeedCapability payments) {
        this.tenants = tenants;
        this.merchants = merchants;
        this.payments = payments;
    }

    @Transactional
    public void reset() {
        payments.clear();
        merchants.clear();
        tenants.clear();
    }

    @Transactional
    public void seed() {
        payments.clear();
        merchants.clear();
        tenants.clear();
        tenants.seed(Fixtures.tenants());
        merchants.seed(Fixtures.merchants());
        payments.seed(Fixtures.paymentOrders());
    }
}
