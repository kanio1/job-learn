package lab.paymentquality.rlslab.internal.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Lab JDBC handles. Must not be a {@link JdbcTemplate} or {@link javax.sql.DataSource}
 * bean — those would disable Spring Boot's primary DataSource / JdbcTemplate auto-configuration.
 */
public final class RlsLabJdbc implements DisposableBean {

    private final DestroyableJdbcTemplate restricted;
    private final DestroyableJdbcTemplate bypass;

    RlsLabJdbc(DestroyableJdbcTemplate restricted, DestroyableJdbcTemplate bypass) {
        this.restricted = restricted;
        this.bypass = bypass;
    }

    public JdbcTemplate restricted() {
        return restricted;
    }

    public JdbcTemplate bypass() {
        return bypass;
    }

    @Override
    public void destroy() throws Exception {
        restricted.destroy();
        bypass.destroy();
    }
}
