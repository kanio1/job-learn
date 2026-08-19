package lab.paymentquality.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.UUID;

public abstract class PostgresContainerSupport {

    private static final int START_ATTEMPTS = 3;

    @SuppressWarnings("resource")
    protected static PostgreSQLContainer newPostgresContainer(String prefix) {
        return new PostgreSQLContainer(
                DockerImageName.parse("docker.io/library/postgres:18").asCompatibleSubstituteFor("postgres"))
                .withDatabaseName(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Start a unique Postgres 18 container. Rootless Podman can drop the API
     * socket after many create/destroy cycles in one Surefire JVM; retry with a
     * fresh container object instead of reusing a failed GenericContainer.
     */
    protected static PostgreSQLContainer startPostgres(String prefix) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= START_ATTEMPTS; attempt++) {
            PostgreSQLContainer container = newPostgresContainer(prefix);
            try {
                container.start();
                return container;
            } catch (RuntimeException ex) {
                last = ex;
                stopQuietly(container);
                if (attempt == START_ATTEMPTS || !isTransientDockerFailure(ex)) {
                    throw ex;
                }
                sleepBeforeRetry(attempt);
            }
        }
        throw last;
    }

    protected static void registerPostgresProperties(
            DynamicPropertyRegistry registry,
            PostgreSQLContainer postgres) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    private static boolean isTransientDockerFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof IOException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null
                    && (message.contains("Broken pipe")
                            || message.contains("Przerwany potok")
                            || message.contains("Connection reset")
                            || message.contains("Container startup failed")
                            || message.contains("Could not create/start container"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void stopQuietly(PostgreSQLContainer container) {
        try {
            container.stop();
        } catch (RuntimeException ignored) {
            // Failed start often leaves the object unusable.
        }
    }

    private static void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(2_000L * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
