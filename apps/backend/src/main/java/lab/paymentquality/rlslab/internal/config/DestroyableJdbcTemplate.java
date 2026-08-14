package lab.paymentquality.rlslab.internal.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * JdbcTemplate that closes its backing pool. Must not be registered as a {@link DataSource}
 * bean — that would disable Spring Boot's primary DataSource auto-configuration.
 */
final class DestroyableJdbcTemplate extends JdbcTemplate implements DisposableBean {

    private final AutoCloseable pool;

    DestroyableJdbcTemplate(DataSource dataSource) {
        super(dataSource);
        if (!(dataSource instanceof AutoCloseable closeable)) {
            throw new IllegalArgumentException("RLS lab DataSource must be closeable");
        }
        this.pool = closeable;
    }

    @Override
    public void destroy() throws Exception {
        pool.close();
    }
}
