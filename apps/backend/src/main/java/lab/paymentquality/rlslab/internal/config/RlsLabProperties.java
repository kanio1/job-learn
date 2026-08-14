package lab.paymentquality.rlslab.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rls-lab")
public record RlsLabProperties(boolean enabled, Datasource datasource, Datasource bypass) {

    public RlsLabProperties {
        if (datasource == null) {
            datasource = new Datasource(null, "rls_lab_app", "rls_lab_app");
        }
        if (bypass == null) {
            bypass = new Datasource(null, "rls_lab_bypass", "rls_lab_bypass");
        }
    }

    public record Datasource(String url, String username, String password) {
    }
}
