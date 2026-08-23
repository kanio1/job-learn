package lab.paymentquality.eventlab.internal.infrastructure;

import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaEventLabProcessedRepository extends JpaRepository<EventLabProcessed, UUID> {

    Optional<EventLabProcessed> findByConsumerGroupAndEventId(String consumerGroup, UUID eventId);

    List<EventLabProcessed> findByTargetId(String targetId);

    List<EventLabProcessed> findByTenantRef(String tenantRef);

    Optional<EventLabProcessed> findById(UUID id);

    @Modifying
    @Query("DELETE FROM EventLabProcessed e WHERE e.consumedAt < :cutoff AND e.status = 'PROCESSED'")
    int deleteProcessedOlderThan(Instant cutoff);
}
