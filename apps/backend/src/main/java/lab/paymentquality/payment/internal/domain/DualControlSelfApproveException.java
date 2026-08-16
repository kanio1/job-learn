package lab.paymentquality.payment.internal.domain;

import org.springframework.http.HttpStatus;

public class DualControlSelfApproveException extends RuntimeException {

    public DualControlSelfApproveException() {
        super("Maker cannot approve their own refund request");
    }

    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }

    public String error() {
        return "dual_control_self_approve";
    }
}
