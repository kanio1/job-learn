package lab.paymentquality.payment.internal.domain;

import org.springframework.http.HttpStatus;

public class DualControlRequiredException extends RuntimeException {

    public DualControlRequiredException() {
        super("Merchant refunds require a dual-control approval; direct POST /refund is not allowed");
    }

    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }

    public String error() {
        return "dual_control_required";
    }
}
