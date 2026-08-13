package lab.paymentquality.mirrorlab.internal.infrastructure;

import lab.paymentquality.mirrorlab.internal.domain.MirrorLabDisputeEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaMirrorLabDisputeEvidenceRepository extends JpaRepository<MirrorLabDisputeEvidence, UUID> {
}
