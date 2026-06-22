package lab.paymentquality.payment.internal.domain;

public class AuthorizationExpiredException extends RuntimeException {

    public AuthorizationExpiredException() {
        super("Authorization has expired");
    }
}
