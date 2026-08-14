package lab.paymentquality.rlslab.internal.application;

import lab.paymentquality.rlslab.internal.config.RlsLabJdbc;
import lab.paymentquality.tenant.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Profile("!prod")
@ConditionalOnProperty(name = "app.rls-lab.enabled", havingValue = "true")
public class RlsLabService {

    private static final String LIST_SQL =
            "SELECT item_id, tenant_id, label, amount_minor FROM rls_lab_item ORDER BY label";
    private static final String ITEM_SQL =
            "SELECT item_id, tenant_id, label, amount_minor FROM rls_lab_item WHERE item_id = ?";

    private static final RowMapper<RlsLabItem> ITEM_MAPPER = (rs, rowNum) -> new RlsLabItem(
            rs.getObject("item_id", UUID.class),
            rs.getObject("tenant_id", UUID.class),
            rs.getString("label"),
            rs.getLong("amount_minor"));

    private final JdbcTemplate rlsLabJdbcTemplate;
    private final JdbcTemplate rlsLabBypassJdbcTemplate;

    public RlsLabService(RlsLabJdbc rlsLabJdbc) {
        this.rlsLabJdbcTemplate = rlsLabJdbc.restricted();
        this.rlsLabBypassJdbcTemplate = rlsLabJdbc.bypass();
    }

    public List<RlsLabItem> list(TenantContext tenant) {
        if (tenant.isPlatformScoped()) {
            return on(rlsLabBypassJdbcTemplate, connection -> queryItems(connection, LIST_SQL));
        }
        return withTenantSession(tenant, connection -> queryItems(connection, LIST_SQL));
    }

    public RlsLabItem requireItem(TenantContext tenant, UUID itemId) {
        List<RlsLabItem> found = tenant.isPlatformScoped()
                ? on(rlsLabBypassJdbcTemplate, connection -> queryItem(connection, itemId))
                : withTenantSession(tenant, connection -> queryItem(connection, itemId));
        if (found.isEmpty()) {
            throw new RlsLabProblemException(HttpStatus.NOT_FOUND, "not_found", "RLS lab item not visible");
        }
        return found.getFirst();
    }

    /**
     * Leak-contrast demo. Callers without a platform tenant are rejected here even if
     * they somehow hold {@code platform:payments:read} (defense in depth after the
     * URL matcher and {@code @PreAuthorize}).
     */
    public RlsLabCompareResult compare(TenantContext tenant) {
        if (!tenant.isPlatformScoped()) {
            throw new RlsLabProblemException(
                    HttpStatus.FORBIDDEN, "rls_forbidden", "Compare leak demo is platform-only");
        }
        long withBypassRole = on(rlsLabBypassJdbcTemplate, connection -> count(connection, "rls_lab_item"));
        long withoutTenantGuc = on(rlsLabJdbcTemplate, connection -> count(connection, "rls_lab_item"));
        long unprotected = on(rlsLabJdbcTemplate, connection -> count(connection, "rls_lab_item_unprotected"));
        return new RlsLabCompareResult(withBypassRole, withoutTenantGuc, unprotected);
    }

    private List<RlsLabItem> queryItems(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<RlsLabItem> items = new ArrayList<>();
            int row = 0;
            while (rs.next()) {
                items.add(ITEM_MAPPER.mapRow(rs, row++));
            }
            return items;
        }
    }

    private List<RlsLabItem> queryItem(Connection connection, UUID itemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(ITEM_SQL)) {
            statement.setObject(1, itemId);
            try (ResultSet rs = statement.executeQuery()) {
                List<RlsLabItem> items = new ArrayList<>();
                if (rs.next()) {
                    items.add(ITEM_MAPPER.mapRow(rs, 0));
                }
                return items;
            }
        }
    }

    private long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private <T> T withTenantSession(TenantContext tenant, SqlWork<T> work) {
        return on(rlsLabJdbcTemplate, connection -> {
            try (PreparedStatement tenantSetting = connection.prepareStatement(
                    "SELECT set_config('app.tenant_id', ?, true)")) {
                tenantSetting.setString(1, tenant.tenantId().toString());
                tenantSetting.execute();
            }
            return work.run(connection);
        });
    }

    private <T> T on(JdbcTemplate template, SqlWork<T> work) {
        return template.execute((Connection connection) -> {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } catch (Exception ex) {
                connection.rollback();
                throw new SQLException(ex);
            } finally {
                connection.setAutoCommit(previous);
            }
        });
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }
}
