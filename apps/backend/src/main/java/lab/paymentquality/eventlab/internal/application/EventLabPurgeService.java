package lab.paymentquality.eventlab.internal.application;

import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EventLabPurgeService {

    private final JpaEventLabProcessedRepository repository;

    public EventLabPurgeService(JpaEventLabProcessedRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int purgeOlderThanDays(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return repository.deleteProcessedOlderThan(cutoff);
    }
}
