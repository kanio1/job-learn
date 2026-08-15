package lab.paymentquality.shared.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Code-first OpenAPI document for the public HTTP surface.
 *
 * <p>Path and package filters live in {@code application.yml} (lab/test exclude set).
 * Do not exclude {@code *.internal} packages — production controllers live there.
 */
@Configuration
class OpenApiConfiguration {

    private static final String BEARER_JWT = "bearer-jwt";

    @Bean
    OpenAPI paymentQualityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Quality Lab API")
                        .version("0.0.1")
                        .description("Code-first public HTTP contract. Lab and test paths are excluded."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
