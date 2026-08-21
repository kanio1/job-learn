package lab.paymentquality.ops.internal.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaOpsFeedEventRepository extends JpaRepository<OpsFeedEventRecord, UUID> {
}
