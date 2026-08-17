package lab.paymentquality.testing.internal.web;

import lab.paymentquality.testing.internal.etl.EtlBatchResult;

record EtlOperationResponse(String operation, String status, EtlBatchResult batch) {

    static EtlOperationResponse completed(String operation, EtlBatchResult batch) {
        return new EtlOperationResponse(operation, "completed", batch);
    }
}
