package lab.paymentquality.checkoutlab.internal.web;

import lab.paymentquality.checkoutlab.internal.domain.CheckoutSession;

final class CheckoutSessionMapper {

    private CheckoutSessionMapper() {
    }

    static CheckoutSessionResponse toResponse(CheckoutSession session) {
        return new CheckoutSessionResponse(
                session.getSessionId(),
                session.getExtOrderId(),
                session.getStatus(),
                session.getAmountMinor(),
                session.getCurrency(),
                session.getValidityUntil(),
                session.getContinueUrl(),
                session.getNotifyUrl(),
                session.getRedirectUri(),
                session.getCorrelationId());
    }
}
