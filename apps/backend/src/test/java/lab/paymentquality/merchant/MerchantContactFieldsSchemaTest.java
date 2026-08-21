package lab.paymentquality.merchant;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class MerchantContactFieldsSchemaTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_contact_fields_schema");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("RA-OPS-001 V31 contact_phone and contact_address exist")
    void flywayV31AddsMerchantContactColumns() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE script = 'V31__merchant_contact_fields.sql'
                """, Integer.class)).isEqualTo(1);

        assertThat(jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'merchants'
                  AND column_name IN ('contact_phone', 'contact_address')
                ORDER BY column_name
                """, String.class)).containsExactly("contact_address", "contact_phone");

        Integer phoneMax = jdbcTemplate.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'merchants' AND column_name = 'contact_phone'
                """, Integer.class);
        Integer addressMax = jdbcTemplate.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'merchants' AND column_name = 'contact_address'
                """, Integer.class);
        assertThat(phoneMax).isEqualTo(32);
        assertThat(addressMax).isEqualTo(200);
    }
}
