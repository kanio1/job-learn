package lab.paymentquality.payment;

import java.util.List;

public interface PaymentSeedCapability {

    void seed(List<PaymentOrderSeed> orders);

    void clear();
}
