package lab.paymentquality.ops.internal.infrastructure;

import lab.paymentquality.ops.internal.domain.OpsNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaOpsNotificationRepository extends JpaRepository<OpsNotification, UUID> {

    List<OpsNotification> findByRecipientSubjectOrderByCreatedAtDesc(String recipientSubject);

    List<OpsNotification> findByRecipientSubjectAndReadAtIsNullOrderByCreatedAtDesc(String recipientSubject);

    Optional<OpsNotification> findByNotificationIdAndRecipientSubject(UUID notificationId, String recipientSubject);

    boolean existsByRecipientSubjectAndEventId(String recipientSubject, UUID eventId);

    @Modifying
    @Query("update OpsNotification n set n.readAt = :readAt where n.recipientSubject = :subject and n.readAt is null")
    int markAllRead(@Param("subject") String subject, @Param("readAt") Instant readAt);
}
