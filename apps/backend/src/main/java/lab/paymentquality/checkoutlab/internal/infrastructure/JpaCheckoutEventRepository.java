package lab.paymentquality.checkoutlab.internal.infrastructure;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCheckoutEventRepository extends JpaRepository<CheckoutEvent, UUID> {

    Optional<CheckoutEvent> findByEventId(String eventId);

    List<CheckoutEvent> findBySessionIdOrderByReceivedAtAsc(UUID sessionId);

    @Query(value = """
            WITH picked AS (
                SELECT id
                  FROM checkout_event
                 WHERE process_status = 'RECEIVED'
                 ORDER BY received_at ASC
                 LIMIT :batchSize
                 FOR UPDATE SKIP LOCKED
            )
            UPDATE checkout_event AS event
               SET process_status = 'PROCESSING',
                   attempts = event.attempts + 1
              FROM picked
             WHERE event.id = picked.id
            RETURNING event.id
            """, nativeQuery = true)
    List<UUID> claimNextReceivedIds(@Param("batchSize") int batchSize);
}
