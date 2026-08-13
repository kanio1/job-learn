package lab.paymentquality.checkoutlab.internal.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Mutable lab clock so expiry tests can freeze time without sleeping.
 */
@Component
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabClock {

    private volatile Clock clock = Clock.systemUTC();

    public Instant instant() {
        return clock.instant();
    }

    public Clock clock() {
        return clock;
    }

    public void setFixed(Instant instant) {
        this.clock = Clock.fixed(instant, ZoneOffset.UTC);
    }

    public void resetToSystem() {
        this.clock = Clock.systemUTC();
    }
}
