package lab.paymentquality.testing.internal.etl;

public class IncrementalEtlNotReadyException extends RuntimeException {

    public IncrementalEtlNotReadyException() {
        super("Incremental payment ETL requires a prior SUCCEEDED run");
    }
}
