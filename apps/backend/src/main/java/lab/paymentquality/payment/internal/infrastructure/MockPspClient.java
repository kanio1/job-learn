package lab.paymentquality.payment.internal.infrastructure;

import lab.paymentquality.payment.internal.application.PspClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPspClient implements PspClient {

    @Override
    public PspResult authorize(UUID paymentOrderId, long amountMinor, String currency) {
        return PspResult.success("PSP-AUTH-" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Override
    public PspResult capture(UUID paymentOrderId, long amountMinor, String currency) {
        return PspResult.success("PSP-CAP-" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Override
    public PspResult voidAuthorization(UUID paymentOrderId, String pspReference) {
        return PspResult.success("PSP-VOID-" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Override
    public PspResult refund(UUID paymentOrderId, long amountMinor, String currency) {
        return PspResult.success("PSP-REF-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
