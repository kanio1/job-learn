package lab.paymentquality.audit.internal.application;

import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.audit.internal.infrastructure.JpaAuditEventRepository;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class AuditEventListener {

    private final JpaAuditEventRepository repository;

    AuditEventListener(JpaAuditEventRepository repository) {
        this.repository = repository;
    }

    @ApplicationModuleListener
    void on(AuditableActionOccurred event) {
        repository.save(AuditEvent.fromEvent(event));
    }
}
