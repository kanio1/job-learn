package lab.paymentquality.merchant;

import java.util.List;

public interface MerchantSeedCapability {

    void seed(List<MerchantSeed> merchants);

    void clear();
}
