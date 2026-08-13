package lab.paymentquality.mirrorlab.internal.infrastructure;

import lab.paymentquality.mirrorlab.internal.domain.MirrorLabConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaMirrorLabConsentRepository extends JpaRepository<MirrorLabConsent, UUID> {

    Optional<MirrorLabConsent> findByAccessToken(String accessToken);
}
