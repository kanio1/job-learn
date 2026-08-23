package lab.paymentquality.eventlab.internal;

import lab.paymentquality.shared.events.AuditableActionOccurred;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

@Configuration
@Profile("kafka")
@ConditionalOnProperty(name = "app.event-lab.enabled", havingValue = "true")
class EventLabExternalizationConfiguration {

    @Bean
    EventExternalizationConfiguration eventLabExternalization() {
        return EventExternalizationConfiguration.externalizing()
                .select(AuditableActionOccurred.class::isInstance)
                .mapping(AuditableActionOccurred.class, EventLabEnvelope::payloadOf)
                .headers(AuditableActionOccurred.class, EventLabHeaders::from)
                .route(AuditableActionOccurred.class,
                        e -> RoutingTarget.forTarget("lab.auditable-actions.v1").andKey(e.targetId()))
                .build();
    }
}
