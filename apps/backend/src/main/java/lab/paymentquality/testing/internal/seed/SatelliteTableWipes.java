package lab.paymentquality.testing.internal.seed;

import org.springframework.jdbc.core.JdbcTemplate;

final class SatelliteTableWipes {

    private SatelliteTableWipes() {
    }

    static void clearCheckoutAuditAndPublications(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM checkout_anomaly");
        jdbc.update("DELETE FROM checkout_event");
        jdbc.update("DELETE FROM checkout_fulfillment");
        jdbc.update("DELETE FROM checkout_session");
        jdbc.update("DELETE FROM audit_event");
        jdbc.update("DELETE FROM event_publication");
    }
}
