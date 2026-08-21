package lab.paymentquality.merchant.internal.infrastructure;

public record MerchantImportAcceptedRow(
        String merchantReference,
        String displayName,
        String tenantReference) {
}
