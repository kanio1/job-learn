package lab.paymentquality.merchant.internal.domain;

public class MerchantImportPreviewNotFoundException extends RuntimeException {
    public MerchantImportPreviewNotFoundException() {
        super("Import preview was not found");
    }
}
