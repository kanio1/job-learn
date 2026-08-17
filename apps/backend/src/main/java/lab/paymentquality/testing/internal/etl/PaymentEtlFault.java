package lab.paymentquality.testing.internal.etl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
class PaymentEtlFault {

    private boolean failAfterStaging;

    void armAfterStaging() {
        failAfterStaging = true;
    }

    void disarm() {
        failAfterStaging = false;
    }

    boolean consumeAfterStaging() {
        if (!failAfterStaging) {
            return false;
        }
        failAfterStaging = false;
        return true;
    }
}
