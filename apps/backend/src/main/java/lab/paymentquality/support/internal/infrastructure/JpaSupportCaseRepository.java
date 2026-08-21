package lab.paymentquality.support.internal.infrastructure;

import jakarta.persistence.LockModeType;
import lab.paymentquality.support.SupportCaseStatus;
import lab.paymentquality.support.internal.domain.SupportCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSupportCaseRepository extends JpaRepository<SupportCase, UUID> {

    Optional<SupportCase> findByCaseReference(String caseReference);

    Optional<SupportCase> findByCaseIdAndTenantId(UUID caseId, UUID tenantId);

    List<SupportCase> findAllByOrderByUpdatedAtDescCaseIdAsc();

    List<SupportCase> findByTenantIdOrderByUpdatedAtDescCaseIdAsc(UUID tenantId);

    List<SupportCase> findByStatusOrderByUpdatedAtDescCaseIdAsc(SupportCaseStatus status);

    List<SupportCase> findByTenantIdAndStatusOrderByUpdatedAtDescCaseIdAsc(UUID tenantId, SupportCaseStatus status);

    List<SupportCase> findByAssigneeSubjectOrderByUpdatedAtDescCaseIdAsc(String assigneeSubject);

    List<SupportCase> findByTenantIdAndAssigneeSubjectOrderByUpdatedAtDescCaseIdAsc(
            UUID tenantId, String assigneeSubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SupportCase c where c.caseId = :caseId")
    Optional<SupportCase> lockById(@Param("caseId") UUID caseId);
}
