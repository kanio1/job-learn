package lab.paymentquality.merchant.internal.web;

import java.util.List;
import java.util.UUID;

public record MerchantImportPreviewResponse(
        UUID previewId,
        int validCount,
        int warningCount,
        int rejectedCount,
        List<MerchantImportRowResult> rows) {
}
