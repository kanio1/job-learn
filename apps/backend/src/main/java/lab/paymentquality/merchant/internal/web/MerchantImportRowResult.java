package lab.paymentquality.merchant.internal.web;

public record MerchantImportRowResult(
        int line,
        String status,
        String merchantReference,
        String displayName,
        String tenantReference,
        String reason) {
}
