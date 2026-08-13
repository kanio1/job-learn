package lab.paymentquality.mirrorlab.internal.infrastructure;

import lab.paymentquality.mirrorlab.internal.domain.MirrorLabRefundApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaMirrorLabRefundApprovalRepository extends JpaRepository<MirrorLabRefundApproval, UUID> {
}
