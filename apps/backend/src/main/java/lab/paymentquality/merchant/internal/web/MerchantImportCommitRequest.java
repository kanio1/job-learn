package lab.paymentquality.merchant.internal.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MerchantImportCommitRequest(@NotNull UUID previewId) {
}
