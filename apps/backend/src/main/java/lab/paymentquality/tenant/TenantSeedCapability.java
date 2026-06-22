package lab.paymentquality.tenant;

import java.util.List;

public interface TenantSeedCapability {

    void seed(List<TenantSeed> tenants);

    void clear();
}
