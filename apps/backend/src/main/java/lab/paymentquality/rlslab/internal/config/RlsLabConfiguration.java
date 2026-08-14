package lab.paymentquality.rlslab.internal.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("!prod")
@ConditionalOnProperty(name = "app.rls-lab.enabled", havingValue = "true")
@EnableConfigurationProperties(RlsLabProperties.class)
class RlsLabConfiguration {

    @Bean
    RlsLabJdbc rlsLabJdbc(
            RlsLabProperties properties,
            @Value("${spring.datasource.url}") String primaryUrl) {
        return new RlsLabJdbc(
                labTemplate(properties.datasource(), "rls_lab_app", primaryUrl),
                labTemplate(properties.bypass(), "rls_lab_bypass", primaryUrl));
    }

    private static DestroyableJdbcTemplate labTemplate(
            RlsLabProperties.Datasource datasource,
            String defaultUsername,
            String primaryUrl) {
        String url = datasource != null && datasource.url() != null && !datasource.url().isBlank()
                ? datasource.url()
                : primaryUrl;
        String username = datasource != null && datasource.username() != null && !datasource.username().isBlank()
                ? datasource.username()
                : defaultUsername;
        String password = datasource != null && datasource.password() != null && !datasource.password().isBlank()
                ? datasource.password()
                : defaultUsername;
        DataSource labDataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
        return new DestroyableJdbcTemplate(labDataSource);
    }
}
