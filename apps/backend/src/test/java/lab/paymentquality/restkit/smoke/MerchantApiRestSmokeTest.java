package lab.paymentquality.restkit.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import lab.paymentquality.testsupport.restkit.client.MerchantApi;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
public class MerchantApiRestSmokeTest extends PostgresContainerSupport {

        @Container
        static PostgreSQLContainer postgres = newPostgresContainer("merchant_api_rest_smoke_test");

        @DynamicPropertySource
        static void postgresProperties(DynamicPropertyRegistry registry) {
            registerPostgresProperties(registry, postgres);
        }

        @LocalServerPort
        private int port;

        @Test
        void createMerchantReturns201AndMerchantid() {
            var merchantApi = new MerchantApi(port);
            var merchantReference = MerchantApi.uniqueMerchantReference("smoke");
            var token = TestJwtSupport.platformOperatorToken();
            var displayName = "Merchant Smoke";

            String merchantId = merchantApi.createMerchant(token, merchantReference, displayName)
                .statusCode(201)
                .extract()
                .path("merchantId");
            
            assertThat(merchantId).isNotBlank();
        }
}
