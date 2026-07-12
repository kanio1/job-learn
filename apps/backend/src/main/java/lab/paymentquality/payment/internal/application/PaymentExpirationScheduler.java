package lab.paymentquality.payment.internal.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic trigger for {@link PaymentExpirationService} (F-D1). Disabled via
 * {@code payment.expiration.scheduler.enabled=false} in the test profile so
 * integration tests stay deterministic — the sweep logic itself is unit
 * tested directly against {@link PaymentExpirationService}, not via waiting
 * for a scheduler tick.
 */
@Component
@ConditionalOnProperty(
        name = "payment.expiration.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpirationScheduler.class);

    private final PaymentExpirationService expirationService;

    public PaymentExpirationScheduler(PaymentExpirationService expirationService) {
        this.expirationService = expirationService;
    }

    @Scheduled(fixedRateString = "${payment.expiration.scheduler.fixed-rate-ms:60000}")
    public void sweepOverdueAuthorizations() {
        int expiredCount = expirationService.expireOverdueAuthorizations();
        if (expiredCount > 0) {
            log.info("payment.expiration.sweep.completed expiredCount={}", expiredCount);
        }
    }
}
