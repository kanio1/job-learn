package lab.paymentquality.testing.internal.seed;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SatelliteTableWipesTest {

    @Test
    void clearsUserSavedViewsAlongsideOtherDeterministicDatasetSatellites() {
        List<String> statements = new ArrayList<>();
        JdbcTemplate jdbc = new JdbcTemplate() {
            @Override
            public int update(String sql) {
                statements.add(sql);
                return 1;
            }
        };

        SatelliteTableWipes.clearCheckoutAuditAndPublications(jdbc);

        assertThat(statements).contains("DELETE FROM user_saved_views");
    }
}
