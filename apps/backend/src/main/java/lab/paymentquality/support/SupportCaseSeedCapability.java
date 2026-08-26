package lab.paymentquality.support;

import java.util.UUID;

public interface SupportCaseSeedCapability {
    void clear();

    UUID seedCase(SupportCaseSeed seed);
}
