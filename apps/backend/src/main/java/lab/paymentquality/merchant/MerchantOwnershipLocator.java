package lab.paymentquality.merchant;

import java.util.Optional;
import java.util.UUID;

public interface MerchantOwnershipLocator {
    Optional<MerchantOwnership> find(UUID merchantId);
}
