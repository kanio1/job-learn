package lab.paymentquality.testing.internal.seed;

import org.springframework.jdbc.core.JdbcTemplate;

final class SatelliteTableWipes {

    private SatelliteTableWipes() {
    }

    static void clearCheckoutAuditAndPublications(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM user_saved_views");
        jdbc.update("DELETE FROM checkout_anomaly");
        jdbc.update("DELETE FROM checkout_event");
        jdbc.update("DELETE FROM checkout_fulfillment");
        jdbc.update("DELETE FROM checkout_session");
        jdbc.update("DELETE FROM audit_event");
        jdbc.update("DELETE FROM event_publication");
        clearLearningEtl(jdbc);
    }

    static void clearLearningEtl(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM learning_dwh.fact_payment");
        jdbc.update("DELETE FROM learning_staging.payment");
        jdbc.update("DELETE FROM learning_etl.batch_run");
    }
}
