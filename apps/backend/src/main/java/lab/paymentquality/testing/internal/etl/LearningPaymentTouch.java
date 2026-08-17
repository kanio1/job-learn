package lab.paymentquality.testing.internal.etl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
public class LearningPaymentTouch {

    public static final List<String> DEFAULT_REFERENCES = List.of(
            "LEARN-PAY-000003",
            "LEARN-PAY-000004",
            "LEARN-PAY-000005"
    );

    private final JdbcTemplate jdbc;

    public LearningPaymentTouch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int touch(Instant updatedAt, String... references) {
        List<String> refs = references.length == 0 ? DEFAULT_REFERENCES : List.of(references);
        OffsetDateTime at = OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC);
        int updated = 0;
        for (String reference : refs) {
            updated += jdbc.update("""
                    UPDATE payment_orders
                    SET amount_minor = amount_minor + 1,
                        updated_at = ?
                    WHERE client_order_reference = ?
                    """, at, reference);
        }
        return updated;
    }
}
