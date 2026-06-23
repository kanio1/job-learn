package lab.paymentquality.apitest.core.stack;

import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Wraps TC {@code PostgreSQLContainer} for the Testcontainers stack.
 *
 * <p>The container joins a shared TC {@link Network} so the backend container can reach
 * it via the {@code postgres-db} network alias — test JVM code uses the host-mapped port
 * via {@link #jdbcUrl()}, while the backend container uses {@link #internalJdbcUrl()}.
 */
public final class PostgresSupport {

    private static final String IMAGE = "postgres:18";
    private static final String DB_NAME = "payment_quality_lab";
    private static final String USERNAME = "payment_quality";
    private static final String PASSWORD = "payment_quality_dev";
    private static final String NETWORK_ALIAS = "postgres-db";

    @SuppressWarnings("resource")
    private final PostgreSQLContainer container;

    @SuppressWarnings("deprecation")
    public PostgresSupport(Network network) {
        this.container = new PostgreSQLContainer(
                DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .withDatabaseName(DB_NAME)
                .withUsername(USERNAME)
                .withPassword(PASSWORD);
    }

    public void start() {
        container.start();
    }

    public void stop() {
        container.stop();
    }

    /** JDBC URL for test JVM connections (host-mapped port). */
    public String jdbcUrl() {
        return container.getJdbcUrl();
    }

    /**
     * JDBC URL for use inside other containers on the same TC network.
     * Uses the {@code postgres-db} network alias, not localhost.
     */
    public String internalJdbcUrl() {
        return "jdbc:postgresql://" + NETWORK_ALIAS + ":5432/" + DB_NAME;
    }

    public String username() {
        return USERNAME;
    }

    public String password() {
        return PASSWORD;
    }
}
