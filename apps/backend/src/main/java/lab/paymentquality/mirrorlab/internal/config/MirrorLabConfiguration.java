package lab.paymentquality.mirrorlab.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
@ConditionalOnProperty(name = "app.mirror-lab.enabled", havingValue = "true")
@EnableConfigurationProperties(MirrorLabProperties.class)
class MirrorLabConfiguration {
}
