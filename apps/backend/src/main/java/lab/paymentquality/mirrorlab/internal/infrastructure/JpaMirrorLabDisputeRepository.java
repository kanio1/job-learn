package lab.paymentquality.mirrorlab.internal.infrastructure;

import lab.paymentquality.mirrorlab.internal.domain.MirrorLabDispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaMirrorLabDisputeRepository extends JpaRepository<MirrorLabDispute, UUID> {
}
