package lab.paymentquality.merchant.internal.domain;

public class MerchantImportAlreadyCommittedException extends RuntimeException {
    public MerchantImportAlreadyCommittedException() {
        super("This import preview was already committed");
    }
}
