package lab.paymentquality.testing.internal.etl;

final class EtlForcedFailureException extends RuntimeException {

    EtlForcedFailureException() {
        super("Forced failure after staging");
    }
}
