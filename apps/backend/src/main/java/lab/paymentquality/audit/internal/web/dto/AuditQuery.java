package lab.paymentquality.audit.internal.web.dto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record AuditQuery(
        String actor,
        String action,
        String targetType,
        LocalDate from,
        LocalDate to,
        int page,
        int size
) {

    private static final int MAX_PAGE_SIZE = 100;

    public static AuditQuery of(
            String actor,
            String action,
            String targetType,
            String from,
            String to,
            int page,
            int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than or equal to 1");
        }

        LocalDate parsedFrom = parseDate(from);
        LocalDate parsedTo = parseDate(to);
        if (parsedFrom != null && parsedTo != null && parsedFrom.isAfter(parsedTo)) {
            throw new IllegalArgumentException("from must not be after to");
        }

        return new AuditQuery(
                normalize(actor),
                normalize(action),
                normalize(targetType),
                parsedFrom,
                parsedTo,
                page,
                Math.min(size, MAX_PAGE_SIZE));
    }

    private static LocalDate parseDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Date filters must use ISO format YYYY-MM-DD");
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
