package lab.paymentquality.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

public abstract class PostgresContainerSupport {

    protected static PostgreSQLContainer<?> newPostgresContainer(String prefix) {
        return new PostgreSQLContainer<>(
                DockerImageName.parse("docker.io/library/postgres:18").asCompatibleSubstituteFor("postgres"))
                .withDatabaseName(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
    }

    protected static void registerPostgresProperties(
            DynamicPropertyRegistry registry,
            PostgreSQLContainer<?> postgres) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }
}
