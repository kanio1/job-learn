package lab.paymentquality.mirrorlab.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mirror-lab")
public record MirrorLabProperties(boolean enabled, long stepUpThresholdMinor) {

    public MirrorLabProperties {
        if (stepUpThresholdMinor <= 0) {
            stepUpThresholdMinor = 10_000L;
        }
    }
}
