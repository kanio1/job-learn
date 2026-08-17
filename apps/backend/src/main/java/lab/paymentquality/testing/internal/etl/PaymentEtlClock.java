package lab.paymentquality.testing.internal.etl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
class PaymentEtlClock {

    private volatile Clock clock = Clock.systemUTC();

    Instant instant() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    void setFixed(Instant instant) {
        this.clock = Clock.fixed(instant, ZoneOffset.UTC);
    }

    void resetToSystem() {
        this.clock = Clock.systemUTC();
    }
}
