package lab.paymentquality.audit.internal.web.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditQueryTest {

    @Test
    void parsesDatesNormalizesFiltersAndUsesControllerDefaults() {
        AuditQuery query = AuditQuery.of(
                "  operator  ", "  MERCHANT_CREATED  ", "  MERCHANT  ",
                "2026-06-01", "2026-06-30", 0, 20);

        assertThat(query).isEqualTo(new AuditQuery(
                "operator", "MERCHANT_CREATED", "MERCHANT",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 0, 20));
    }

    @Test
    void clampsPageSizeToOneHundred() {
        assertThat(AuditQuery.of(null, null, null, null, null, 0, 500).size())
                .isEqualTo(100);
    }

    @Test
    void treatsBlankFiltersAsAbsent() {
        AuditQuery query = AuditQuery.of(" ", "\t", "", " ", null, 0, 20);

        assertThat(query.actor()).isNull();
        assertThat(query.action()).isNull();
        assertThat(query.targetType()).isNull();
        assertThat(query.from()).isNull();
        assertThat(query.to()).isNull();
    }

    @Test
    void rejectsMalformedAndInvertedDateRanges() {
        assertThatThrownBy(() -> AuditQuery.of(null, null, null, "not-a-date", null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditQuery.of(null, null, null, "2026-06-02", "2026-06-01", 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> AuditQuery.of(null, null, null, null, null, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditQuery.of(null, null, null, null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
