package lab.paymentquality.testing.internal.etl;

import java.time.Instant;
import java.util.UUID;

public record EtlBatchResult(
        UUID batchId,
        String pipelineName,
        String loadType,
        String status,
        Instant startedAt,
        Instant finishedAt,
        Instant watermarkFrom,
        Instant watermarkTo,
        int sourceRows,
        int stagedRows,
        int loadedRows,
        int rejectedRows,
        String errorMessage
) {
}
